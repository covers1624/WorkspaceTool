/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on classes provided to {@link net.covers1624.wt.event.ModuleHashCheckEvent#putVersionedClass},
 * see {@link net.covers1624.wt.event.ModuleHashCheckEvent#putVersionedClass} for more information.
 * <p>
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
