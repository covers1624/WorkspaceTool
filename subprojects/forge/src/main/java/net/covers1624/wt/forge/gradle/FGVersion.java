package net.covers1624.wt.forge.gradle;

import net.covers1624.wt.event.VersionedClass;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Created by covers1624 on 15/6/19.
 */
@VersionedClass (1)
public class FGVersion implements Serializable {

    public static final FGVersion UNKNOWN = new FGVersion("unknown");
    public static final FGVersion FG22 = new FGVersion("2.2");
    public static final FGVersion FG23 = new FGVersion("2.3");
    public static final FGVersion FG30 = new FGVersion("3.0");
    public static final FGVersion FG40 = new FGVersion("4.0", FG30);
    public static final FGVersion FG41 = new FGVersion("4.1", FG30);
    public static final FGVersion FG50 = new FGVersion("5.0", FG30);
    public static final FGVersion FG51 = new FGVersion("5.1", FG30);

    public final String version;
    @Nullable
    public final FGVersion effectiveVersion;

    public FGVersion(String version) {
        this(version, null);
    }

    public FGVersion(String version, @Nullable FGVersion effectiveVersion) {
        this.version = version;
        this.effectiveVersion = effectiveVersion;
    }

    public boolean isFG2() {
        return this.equals(FG22) || this.equals(FG23);
    }

    public boolean isFG3Compatible() {
        return this.equals(FG30) || effectiveVersion.equals(FG30);
    }

    @Override
    public String toString() {
        return version;
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (!(obj instanceof FGVersion)) {
            return false;
        }
        return ((FGVersion) obj).version.equals(version);
    }
}
