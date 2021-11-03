/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.mixin;

/**
 * Low orbit ion cannon solution to an ant hill.
 * Allows for registering classes that can have mixin components added to them.
 * <p>
 * Created by covers1624 on 8/8/19.
 */
public interface MixinInstantiator {

    /**
     * Adds a MixinTarget.
     * Targets can be instantiated, with any registered mixins applied.
     *
     * @param targetIFace The target interface.
     * @param targetImpl  The template implementation.
     */
    void addMixinTarget(Class<?> targetIFace, Class<?> targetImpl);

    /**
     * Adds an interface to be mixed in using the provided template.
     * Class hierarchy is not analysed, more specifically,
     * the methods that exist in the provided template are pasted
     * into the output class.
     * <p>
     * Duplicate methods and fields are not allowed.
     *
     * @param targetIFace The target to mix-into.
     * @param iFace       The interface to be mixed in.
     * @param template    The template.
     */
    void addMixinClass(Class<?> targetIFace, Class<?> iFace, Class<?> template);

    /**
     * Construct an instance of the specified target.
     *
     * @param clazz The Target.
     * @param <T>   Generics.
     * @return The new instance.
     */
    <T> T instantiate(Class<T> clazz);

}
