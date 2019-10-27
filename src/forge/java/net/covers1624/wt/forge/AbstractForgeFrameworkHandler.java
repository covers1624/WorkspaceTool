package net.covers1624.wt.forge;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.framework.FrameworkHandler;
import net.covers1624.wt.forge.api.script.ForgeFramework;
import net.covers1624.wt.util.GitHelper;
import net.covers1624.wt.util.HashContainer;
import net.covers1624.wt.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * Created by covers1624 on 7/8/19.
 */
public abstract class AbstractForgeFrameworkHandler<T extends ForgeFramework> implements FrameworkHandler<T> {

    protected static final String GRADLE_VERSION = "4.10.3";
    protected static final HashCode MARKER_HASH = HashCode.fromString("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    protected static final Logger logger = LogManager.getLogger("Forge112FrameworkHandler");
    protected static final String HASH_MERGED_AT = "merged-at";
    protected static final String HASH_MARKER_SETUP = "marker-setup";
    protected static final String HASH_GSTART_LOGIN = "gstart-login";
    protected static final HashFunction sha256 = Hashing.sha256();

    protected static final String LOCAL_BRANCH_SUFFIX = "-wt-local";

    protected final WorkspaceToolContext context;
    protected final HashContainer hashContainer;
    protected final GitHelper gitHelper;

    protected Path forgeDir;
    protected boolean needsSetup;

    public AbstractForgeFrameworkHandler(WorkspaceToolContext context) {
        this.context = context;
        hashContainer = new HashContainer(context.cacheDir.resolve("forge_framework_cache.json"));
        gitHelper = new GitHelper(hashContainer);
        needsSetup = hashContainer.get(HASH_MARKER_SETUP) != null;
    }

    @Override
    public void constructFrameworkModules(T frameworkImpl) {
        forgeDir = context.projectDir.resolve(frameworkImpl.getPath());

        gitHelper.setRepoUrl(frameworkImpl.getUrl());
        gitHelper.setPath(forgeDir);
        gitHelper.setBranch(frameworkImpl.getBranch());
        gitHelper.setCommit(frameworkImpl.getCommit());
        gitHelper.setBranchSuffix(LOCAL_BRANCH_SUFFIX);

        if (Utils.sneaky(gitHelper::validate)) {
            needsSetup = true;
            hashContainer.set(HASH_MARKER_SETUP, MARKER_HASH);
        }
    }
}
