package net.covers1624.wt.util;

import net.covers1624.wt.wrapper.iso.ConsoleTransferListener;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 3/8/19.
 */
public class ScriptDepsResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^\\}]*)\\}");

    public static List<URL> getDeps(List<String> repos, List<String> deps) throws Exception {
        List<RemoteRepository> remoteRepos = new ArrayList<>();
        for (String repo : repos) {
            remoteRepos.add(new RemoteRepository.Builder(null, "default", resolvePlaceholders(repo)).build());
        }

        File globalDir = new File(System.getProperty("wt.global.dir"));
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        RepositorySystem system = locator.getService(RepositorySystem.class);
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(new File(globalDir, "local-repo"));
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        //Safely imported, only iso'd in wrapper because dep extraction.
        session.setTransferListener(new ConsoleTransferListener());

        List<File> dependencies = new ArrayList<>();
        for (String dep : deps) {
            Artifact artifact = new DefaultArtifact(dep);
            VersionRangeRequest request = new VersionRangeRequest();
            request.setArtifact(artifact);
            request.setRepositories(remoteRepos);

            VersionRangeResult result = system.resolveVersionRange(session, request);
            Version version = result.getHighestVersion();

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new org.eclipse.aether.graph.Dependency(artifact.setVersion(version.toString()), JavaScopes.RUNTIME));
            collectRequest.setRepositories(remoteRepos);
            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME));
            List<ArtifactResult> artifactResults = system.resolveDependencies(session, dependencyRequest).getArtifactResults();
            dependencies.addAll(artifactResults.stream()//
                    .map(ArtifactResult::getArtifact)//
                    .map(Artifact::getFile)//
                    .collect(Collectors.toList())//
            );
        }
        return dependencies.stream().map(ScriptDepsResolver::quietToURL).collect(Collectors.toList());
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

    private static URL quietToURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
