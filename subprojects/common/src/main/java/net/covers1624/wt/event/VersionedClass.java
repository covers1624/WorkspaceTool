package net.covers1624.wt.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on classes provided to {@link ModuleHashCheckEvent#putVersionedClass},
 * see {@link ModuleHashCheckEvent#putVersionedClass} for more information.
 *
 * Created by covers1624 on 9/7/19.
 */
@Target (ElementType.TYPE)
@Retention (RetentionPolicy.RUNTIME)
public @interface VersionedClass {

    /**
     * @return An incrementing number, indicating version.
     */
    int value();
}
