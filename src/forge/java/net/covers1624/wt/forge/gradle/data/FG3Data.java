package net.covers1624.wt.forge.gradle.data;

import net.covers1624.wt.api.data.ExtraData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by covers1624 on 9/8/19.
 */
public class FG3Data implements ExtraData {

    public List<File> accessTransformers = new ArrayList<>();
    public List<File> sideAnnotationStrippers = new ArrayList<>();
}
