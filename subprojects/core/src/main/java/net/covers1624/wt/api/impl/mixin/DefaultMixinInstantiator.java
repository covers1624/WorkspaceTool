/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.mixin;

import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.util.ClassSmusher;
import net.covers1624.wt.util.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 8/8/19.
 */
public class DefaultMixinInstantiator implements MixinInstantiator {

    private final Map<Class<?>, MixinTarget> mixinTargets = new HashMap<>();
    private final ClassSmusher classSmusher = new ClassSmusher();

    @Override
    public void addMixinTarget(Class<?> targetIFace, Class<?> targetImpl) {
        if (mixinTargets.containsKey(targetIFace)) {
            throw new RuntimeException("Mixin target already registered. " + targetIFace);
        }
        mixinTargets.put(targetIFace, new MixinTarget(targetIFace, targetImpl));
    }

    @Override
    public void addMixinClass(Class<?> targetIFace, Class<?> iFace, Class<?> template) {
        MixinTarget mixinTarget = mixinTargets.get(targetIFace);
        if (mixinTarget == null) {
            throw new RuntimeException("Mixin target not registered." + targetIFace);
        }
        if (mixinTarget.mixins.containsKey(iFace)) {
            throw new RuntimeException("Mixin already registered for interface. " + iFace);
        }
        mixinTarget.mixins.put(iFace, template);
    }

    @Override
    public <T> T instantiate(Class<T> clazz) {
        MixinTarget mixinTarget = mixinTargets.get(clazz);
        if (mixinTarget == null) {
            throw new RuntimeException("No mixin registered for class." + clazz);
        }
        try {
            return Utils.unsafeCast(mixinTarget.getMixinClass().newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to instantiate mixin class.");
        }
    }

    private class MixinTarget {

        private final Class<?> targetIFace;
        private final Class<?> targetImpl;

        private final Map<Class<?>, Class<?>> mixins = new HashMap<>();

        private Class<?> generated;

        private MixinTarget(Class<?> targetIFace, Class<?> targetImpl) {
            this.targetIFace = targetIFace;
            this.targetImpl = targetImpl;
        }

        public Class<?> getMixinClass() {
            if (generated == null) {
                generated = classSmusher.createMixinClass(targetIFace, targetImpl, mixins);
            }
            return generated;
        }

    }

}
