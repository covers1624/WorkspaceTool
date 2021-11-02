package net.covers1624.wt.wrapper.maven;

import net.covers1624.quack.maven.MavenNotation;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.version.Version;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 6/12/20.
 */
public class MavenResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^\\}]*)\\}");

    private final DefaultRepositorySystemSession session;
    private final RepositorySystem system;

    private final List<RemoteRepository> repos;

    public MavenResolver(Path localRepoPath, Map<String, String> repos) {
        session = MavenRepositorySystemUtils.newSession();

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.setService(VersionRangeResolver.class, LocalVersionRangeResolver.class);
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        system = locator.getService(RepositorySystem.class);

        LocalRepository localRepo = new LocalRepository(localRepoPath.toFile());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        session.setTransferListener(new ConsoleTransferListener());

        this.repos = repos.entrySet().stream()
                .map(e -> new RemoteRepository.Builder(e.getKey(), "default", resolvePlaceholders(e.getValue())).build())
                .collect(Collectors.toList());
    }

    public List<Path> resolve(MavenNotation notation) throws Exception {
        Artifact artifact = toArtifact(notation);

        VersionRangeRequest versionRangeRequest = new VersionRangeRequest();
        versionRangeRequest.setArtifact(artifact);
        versionRangeRequest.setRepositories(repos);

        VersionRangeResult versionRangeResult = system.resolveVersionRange(session, versionRangeRequest);
        Version latest = versionRangeResult.getHighestVersion();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact.setVersion(latest.toString()), JavaScopes.RUNTIME));
        collectRequest.setRepositories(repos);

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME, JavaScopes.COMPILE));
        List<ArtifactResult> artifactResults = system.resolveDependencies(session, dependencyRequest).getArtifactResults();

        return artifactResults.stream()
                .map(ArtifactResult::getArtifact)
                .map(Artifact::getFile)
                .map(File::toPath)
                .map(Path::toAbsolutePath)
                .distinct()
                .collect(Collectors.toList());
    }

    private static String resolvePlaceholders(String value) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = placeholder.startsWith("env.") ? System.getenv(placeholder.substring(4)) : System.getProperty(placeholder);
            if (replacement == null) {
                throw new RuntimeException(String.format("Cannot resolve placeholder '%s' in value '%s'", placeholder, value));
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static Artifact toArtifact(MavenNotation notation) {
        return new DefaultArtifact(notation.group, notation.module, notation.classifier, notation.extension, notation.version);
    }
}
