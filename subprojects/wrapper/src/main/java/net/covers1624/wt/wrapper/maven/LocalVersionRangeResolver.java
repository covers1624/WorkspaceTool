/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.wrapper.maven;

import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Basically a copy of {@link DefaultVersionRangeResolver} except handles repos with the id of 'local'.
 */
public class LocalVersionRangeResolver implements VersionRangeResolver, Service {

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";
    private static final String MAVEN_LOCAL_METADATA_XML = "maven-metadata-local.xml";

    private MetadataResolver metadataResolver;

    private SyncContextFactory syncContextFactory;

    private RepositoryEventDispatcher repositoryEventDispatcher;

    public LocalVersionRangeResolver() {
        // enable default constructor
    }

    @Inject
    LocalVersionRangeResolver(MetadataResolver metadataResolver, SyncContextFactory syncContextFactory,
            RepositoryEventDispatcher repositoryEventDispatcher) {
        setMetadataResolver(metadataResolver);
        setSyncContextFactory(syncContextFactory);
        setRepositoryEventDispatcher(repositoryEventDispatcher);
    }

    public void initService(ServiceLocator locator) {
        setMetadataResolver(locator.getService(MetadataResolver.class));
        setSyncContextFactory(locator.getService(SyncContextFactory.class));
        setRepositoryEventDispatcher(locator.getService(RepositoryEventDispatcher.class));
    }

    public LocalVersionRangeResolver setMetadataResolver(MetadataResolver metadataResolver) {
        this.metadataResolver = Objects.requireNonNull(metadataResolver, "metadataResolver cannot be null");
        return this;
    }

    public LocalVersionRangeResolver setSyncContextFactory(SyncContextFactory syncContextFactory) {
        this.syncContextFactory = Objects.requireNonNull(syncContextFactory, "syncContextFactory cannot be null");
        return this;
    }

    public LocalVersionRangeResolver setRepositoryEventDispatcher(
            RepositoryEventDispatcher repositoryEventDispatcher) {
        this.repositoryEventDispatcher = Objects.requireNonNull(repositoryEventDispatcher,
                "repositoryEventDispatcher cannot be null");
        return this;
    }

    public VersionRangeResult resolveVersionRange(RepositorySystemSession session, VersionRangeRequest request)
            throws VersionRangeResolutionException {
        VersionRangeResult result = new VersionRangeResult(request);

        VersionScheme versionScheme = new GenericVersionScheme();

        VersionConstraint versionConstraint;
        try {
            versionConstraint = versionScheme.parseVersionConstraint(request.getArtifact().getVersion());
        } catch (InvalidVersionSpecificationException e) {
            result.addException(e);
            throw new VersionRangeResolutionException(result);
        }

        result.setVersionConstraint(versionConstraint);

        if (versionConstraint.getRange() == null) {
            result.addVersion(versionConstraint.getVersion());
        } else {
            Map<String, ArtifactRepository> versionIndex = getVersions(session, result, request);

            List<Version> versions = new ArrayList<>();
            for (Map.Entry<String, ArtifactRepository> v : versionIndex.entrySet()) {
                try {
                    Version ver = versionScheme.parseVersion(v.getKey());
                    if (versionConstraint.containsVersion(ver)) {
                        versions.add(ver);
                        result.setRepository(ver, v.getValue());
                    }
                } catch (InvalidVersionSpecificationException e) {
                    result.addException(e);
                }
            }

            Collections.sort(versions);
            result.setVersions(versions);
        }

        return result;
    }

    private Map<String, ArtifactRepository> getVersions(RepositorySystemSession session, VersionRangeResult result,
            VersionRangeRequest request) {
        RequestTrace trace = RequestTrace.newChild(request.getTrace(), request);

        Map<String, ArtifactRepository> versionIndex = new HashMap<>();

        Metadata metadata =
                new DefaultMetadata(request.getArtifact().getGroupId(), request.getArtifact().getArtifactId(),
                        MAVEN_METADATA_XML, Metadata.Nature.RELEASE_OR_SNAPSHOT);

        List<MetadataRequest> metadataRequests = new ArrayList<>(request.getRepositories().size());

        metadataRequests.add(new MetadataRequest(metadata, null, request.getRequestContext()));

        for (RemoteRepository repository : request.getRepositories()) {
            Metadata meta = metadata;
            if (repository.getId().equals("local")) {
                meta = new DefaultMetadata(request.getArtifact().getGroupId(), request.getArtifact().getArtifactId(),
                        MAVEN_LOCAL_METADATA_XML, Metadata.Nature.RELEASE_OR_SNAPSHOT);
            }
            MetadataRequest metadataRequest = new MetadataRequest(meta, repository, request.getRequestContext());
            metadataRequest.setDeleteLocalCopyIfMissing(true);
            metadataRequest.setTrace(trace);
            metadataRequests.add(metadataRequest);

        }

        List<MetadataResult> metadataResults = metadataResolver.resolveMetadata(session, metadataRequests);

        WorkspaceReader workspace = session.getWorkspaceReader();
        if (workspace != null) {
            List<String> versions = workspace.findVersions(request.getArtifact());
            for (String version : versions) {
                versionIndex.put(version, workspace.getRepository());
            }
        }

        for (MetadataResult metadataResult : metadataResults) {
            result.addException(metadataResult.getException());

            ArtifactRepository repository = metadataResult.getRequest().getRepository();
            if (repository == null) {
                repository = session.getLocalRepository();
            }

            Versioning versioning = readVersions(session, trace, metadataResult.getMetadata(), repository, result);
            for (String version : versioning.getVersions()) {
                if (!versionIndex.containsKey(version)) {
                    versionIndex.put(version, repository);
                }
            }
        }

        return versionIndex;
    }

    private Versioning readVersions(RepositorySystemSession session, RequestTrace trace, Metadata metadata,
            ArtifactRepository repository, VersionRangeResult result) {
        Versioning versioning = null;
        try {
            if (metadata != null) {
                try (SyncContext syncContext = syncContextFactory.newInstance(session, true)) {
                    syncContext.acquire(null, Collections.singleton(metadata));

                    if (metadata.getFile() != null && metadata.getFile().exists()) {
                        try (final InputStream in = new FileInputStream(metadata.getFile())) {
                            versioning = new MetadataXpp3Reader().read(in, false).getVersioning();
                        }
                    }
                }
            }
        } catch (Exception e) {
            invalidMetadata(session, trace, metadata, repository, e);
            result.addException(e);
        }

        return (versioning != null) ? versioning : new Versioning();
    }

    private void invalidMetadata(RepositorySystemSession session, RequestTrace trace, Metadata metadata,
            ArtifactRepository repository, Exception exception) {
        RepositoryEvent.Builder event = new RepositoryEvent.Builder(session, RepositoryEvent.EventType.METADATA_INVALID);
        event.setTrace(trace);
        event.setMetadata(metadata);
        event.setException(exception);
        event.setRepository(repository);

        repositoryEventDispatcher.dispatch(event.build());
    }
}
