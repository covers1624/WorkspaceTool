package net.covers1624.wt.forge.api.script;

import net.covers1624.wt.api.framework.ModdingFramework;

/**
 * See: {@link Forge112}
 * Created by covers1624 on 13/05/19.
 */
public interface ForgeFramework extends ModdingFramework {

    /**
     * Sets the path for the Forge clone.
     * Relative to your project's root.
     *
     * @param path The path.
     */
    void setPath(String path);

    String getPath();

    //Groovy magic.
    default void path(String path) {
        setPath(path);
    }

    /**
     * Sets the URL to clone Forge from.
     * Useful if you want to use your own clone or a fork.
     * Defaults to: 'https://github.com/MinecraftForge/MinecraftForge.git'
     *
     * @param url The URL.
     */
    void setUrl(String url);

    String getUrl();

    //Groovy magic.
    default void url(String url) {
        setUrl(url);
    }

    /**
     * Sets the Forge branch to clone.
     * This is defaulted by the various specific forge handlers.
     *
     * @param branch The branch.
     */
    void setBranch(String branch);

    String getBranch();

    //Groovy magic.
    default void branch(String branch) {
        setBranch(branch);
    }

    /**
     * Sets the commit of Forge to checkout.
     *
     * @param commit The commit.
     */
    void setCommit(String commit);

    String getCommit();

    //Groovy magic.
    default void commit(String commit) {
        setCommit(commit);
    }
}
