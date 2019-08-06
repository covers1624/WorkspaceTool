package net.covers1624.wt.api.data;

import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.event.VersionedClass;

import java.io.Serializable;

/**
 * Super interface for all data extracted by ExtraModelBuilders
 *
 * @see GradleManager
 *
 * Created by covers1624 on 18/6/19.
 */
@VersionedClass (1)
public interface ExtraData extends Serializable {}
