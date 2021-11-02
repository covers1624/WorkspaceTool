package net.covers1624.wt.mc.data;

import com.google.common.hash.HashCode;
import net.covers1624.quack.maven.MavenNotation;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by covers1624 on 5/02/19.
 */
public class VersionInfoJson {

    public List<Library> libraries = new ArrayList<>();
    public Map<String, Download> downloads;
    public AssetIndex assetIndex;

    public class AssetIndex {

        public String id;
        public HashCode sha1;
        public URL url;

        public String getId(String version) {
            if (!version.equals(id)) {
                return version + "-" + id;
            }
            return id;
        }
    }

    public class Download {

        public URL url;
        public HashCode sha1;
    }

    public class Library {

        public MavenNotation name;
        public Extract extract;
        public List<Rule> rules = new ArrayList<>();
        public Map<OS, String> natives = new HashMap<>();

        public boolean allowed() {
            if (rules == null || rules.isEmpty()) {
                return true;
            }
            boolean last = false;
            for (Rule rule : rules) {
                if (rule.os != null && !rule.os.isCurrent()) {
                    last = rule.action.equals("allow");
                }
            }
            return last;
        }

        public MavenNotation getArtifact(boolean skipNatives) {
            if (natives == null || skipNatives) {
                return name;
            } else {
                String classifier = natives.get(OS.currentOS());
                if (classifier == null) {
                    return name;
                }
                return name.withClassifier(classifier);
            }
        }

        public class Extract {

            public List<String> exclude = new ArrayList<>();
        }

        public class Rule {

            public String action;
            public OSRestriction os;

            public class OSRestriction {

                public OS os;
                public String version;
                public String arch;

                public boolean isCurrent() {
                    OS current = OS.currentOS();
                    if (os != null && os != current) {
                        return false;
                    }
                    if (version != null) {
                        try {
                            Pattern pattern = Pattern.compile(version);
                            if (!pattern.matcher(OS.VERSION).matches()) {
                                return false;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    if (arch != null) {
                        try {
                            Pattern pattern = Pattern.compile(arch);
                            if (!pattern.matcher(OS.ARCH).matches()) {
                                return false;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    return true;
                }
            }
        }

    }

    public enum OS {
        LINUX("linux", "bsd", "unix"),
        WINDOWS("windows", "win"),
        OSX("osx", "mac"),
        UNKNOWN("unknown");

        public static final String VERSION = System.getProperty("os.version");
        public static final String ARCH = System.getProperty("os.arch");
        private static OS currentOS;

        private String name;
        private String[] aliases;

        OS(String name, String... aliases) {
            this.name = name;
            this.aliases = aliases;
        }

        public static OS currentOS() {
            if (currentOS == null) {
                String osName = System.getProperty("os.name");
                for (OS os : values()) {
                    if (StringUtils.containsIgnoreCase(osName, os.name)) {
                        return currentOS = os;
                    }
                    for (String alias : os.aliases) {
                        if (StringUtils.containsIgnoreCase(alias, os.name)) {
                            return currentOS = os;
                        }
                    }
                }
                currentOS = UNKNOWN;
            }
            return currentOS;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
