package net.covers1624.wt.wrapper.iso;

import com.google.gson.Gson;
import net.covers1624.wt.wrapper.Main;
import net.covers1624.wt.wrapper.WrappedClasspath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 9/04/19.
 */
public class Bootstrap {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^\\}]*)\\}");

    public static WrappedClasspath computeClasspath(File globalDir, File properties) throws Exception {

        File tmpFile = File.createTempFile("log4j2_wrapper", ".xml");
        try (InputStream is = Bootstrap.class.getResourceAsStream("/log4j2_wrapper.xml")) {
            try (FileOutputStream fos = new FileOutputStream(Main.makeFile(tmpFile))) {
                Main.copy(is, fos);
            }
        }

        String prev = System.getProperty("log4.configurationFile");
        System.setProperty("log4j.configurationFile", tmpFile.getAbsolutePath());
        try {
            return resolve(globalDir, properties);
        } finally {
            shutdownLog4j();
            if (prev == null) {
                System.getProperties().remove("log4j.configurationFile");
            } else {
                System.setProperty("log4j.configurationFile", prev);
            }
        }
    }

    private static void shutdownLog4j() {
        LogManager.shutdown();
    }

    private static WrappedClasspath resolve(File globalDir, File properties) throws Exception {
        Logger logger = LogManager.getLogger("Bootstrap");
        logger.info("Initializing Bootstrap.");
        WTProperties props;
        try (FileInputStream fis = new FileInputStream(properties)) {
            props = new Gson().fromJson(new InputStreamReader(fis), WTProperties.class);
        }
        List<RemoteRepository> repos = new ArrayList<>();

        for (Map.Entry<String, String> entry : props.repos.entrySet()) {
            repos.add(new RemoteRepository.Builder(entry.getKey(), "default", resolvePlaceholders(entry.getValue())).build());
        }

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        RepositorySystem system = locator.getService(RepositorySystem.class);
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(new File(globalDir, "local-repo"));
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        session.setTransferListener(new ConsoleTransferListener());

        Artifact artifact = new DefaultArtifact(props.artifact);

        //Resolve the highest version of the artifact
        // for absolute artifacts that will be the provided version,
        // otherwise it will find the latest.
        VersionRangeRequest request = new VersionRangeRequest();
        request.setArtifact(artifact);
        request.setRepositories(repos);

        VersionRangeResult result = system.resolveVersionRange(session, request);
        Version version = result.getHighestVersion();

        //Collect all runtime dependencies for the artifact.
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact.setVersion(version.toString()), JavaScopes.RUNTIME));
        collectRequest.setRepositories(repos);

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME, JavaScopes.COMPILE));
        List<ArtifactResult> artifactResults = system.resolveDependencies(session, dependencyRequest).getArtifactResults();

        logger.info("Launching..");
        WrappedClasspath classpath = new WrappedClasspath();
        classpath.mainClass = props.mainClass;
        classpath.classPath.addAll(artifactResults.stream()//
                .map(ArtifactResult::getArtifact)//
                .map(Artifact::getFile)//
                .collect(Collectors.toList()));
        return classpath;
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

}
