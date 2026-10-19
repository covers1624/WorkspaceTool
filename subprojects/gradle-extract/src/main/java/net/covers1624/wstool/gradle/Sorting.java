package net.covers1624.wstool.gradle;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A sorting index annotation.
 * <p>
 * No annotation present assumes 1000.
 * <p>
 * Lower values will appear earlier.
 * <p>
 * Things with identical index values will retain their original input
 * ordering.
 * <p>
 * Created by covers1624 on 10/18/26.
 */
@Retention (RetentionPolicy.RUNTIME)
public @interface Sorting {

    int DEFAULT = 1000;

    /**
     * @return an arbitrary index for sorting.
     */
    int value();
}
