package net.covers1624.wt.forge.api.impl;

import net.covers1624.wt.forge.api.script.ForgeFramework;

/**
 * Created by covers1624 on 13/05/19.
 */
public abstract class AbstractForgeFrameworkImpl implements ForgeFramework {

    private String path;
    private String url;
    private String branch;
    private String commit;

    public AbstractForgeFrameworkImpl() {
        setPath("Forge");
        setUrl("https://github.com/MinecraftForge/MinecraftForge.git");
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setBranch(String branch) {
        this.branch = branch;
    }

    @Override
    public String getBranch() {
        return branch;
    }

    @Override
    public void setCommit(String commit) {
        this.commit = commit;
    }

    @Override
    public String getCommit() {
        return commit;
    }
}
