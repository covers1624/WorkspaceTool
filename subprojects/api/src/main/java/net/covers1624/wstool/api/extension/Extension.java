package net.covers1624.wstool.api.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by covers1624 on 22/9/24.
 */
public interface Extension {

    default void registerConfigTypes(ConfigTypeRegistry registry) { }

    interface ConfigTypeRegistry {

        void registerFramework(String key, Class<? extends Framework> type);

        void registerWorkspace(String key, Class<? extends Workspace> type);
    }

    @Target (ElementType.TYPE)
    @Retention (RetentionPolicy.RUNTIME)
    @interface Details {

        String id();

        String desc();
    }
}
