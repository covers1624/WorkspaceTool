package net.covers1624.wt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Required on all Extension classes.
 * Provides the user some information on what the Extension is.
 *
 * Created by covers1624 on 17/6/19.
 */
@Target (ElementType.TYPE)
@Retention (RetentionPolicy.RUNTIME)
public @interface ExtensionDetails {

    String name();

    String desc();
}
