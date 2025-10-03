package net.covers1624.wstool.api.extension;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.workspace.Workspace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by covers1624 on 22/9/24.
 */
public interface Extension {

    default void registerConfigTypes(ConfigTypeRegistry registry) { }

    default void prepareEnvironment(Environment env) { }

    default void processWorkspace(Environment env, Workspace workspace) { }

    interface ConfigTypeRegistry {

        void registerFrameworkType(String key, Class<? extends FrameworkType> type);

        void registerWorkspaceType(String key, Class<? extends WorkspaceType> type);
    }

    @Target (ElementType.TYPE)
    @Retention (RetentionPolicy.RUNTIME)
    @interface Details {

        String id();

        String desc();
    }
}
