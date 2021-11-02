/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.intellij.util

import com.google.common.collect.HashBasedTable
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Table
import net.covers1624.wt.api.workspace.WorkspaceModule
import net.covers1624.wt.util.Utils

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

/**
 * Transcribed from
 * https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/src/org/jetbrains/plugins/gradle/service/project/GradleProjectResolver.java#L581-L709
 *
 * Created by covers1624 on 15/8/19.
 */
//@groovy.transform.CompileStatic
class ContentRootMerger {

    public static final EXCLUDE_MAGIC_TYPE = '$$_$$_excludes'

    /**
     * Takes a List of modules, merges the content root's of each SourceSet.
     *
     * @param modules
     * @return The merged modules. Table<WorkspaceModule.name, ContentRoot, MergedSourceSet> mergeResults
     */
    static Table<String, Path, MergedSourceSet> mergeContentRoots(Iterable<WorkspaceModule> modules) {
        Map<String, AtomicInteger> weightMap = [:]
        Table<String, Path, MergedSourceSet> mergeResults = HashBasedTable.create()
        def getWeight = { path -> weightMap.computeIfAbsent(path, { new AtomicInteger() }) }
        modules.each { module ->

            if (module.path != null) {
                File mPath = module.path.toFile()
                while (mPath != null) {
                    getWeight(mPath.getPath()).getAndIncrement()
                    mPath = mPath.getParentFile()
                }
            }

            def set = new HashSet<String>()
            module.sourceMap.values().each {
                it.each {
                    def file = it.toFile()
                    while (file != null) {
                        set.add(file.getPath())
                        file = file.getParentFile()
                    }
                }
            }
            set.each {
                getWeight(it).getAndIncrement()
            }
        }
        modules.each { module ->
            Multimap<Path, MergedSourceSet> sourceSets = HashMultimap.create()
            if (module.sourceMap.containsKey(EXCLUDE_MAGIC_TYPE)) {
                throw new RuntimeException("Somehow you have a SourceSet the same name as our magic string for excludes: ${module.path}, SS: ${EXCLUDE_MAGIC_TYPE}")
            }
            def sourceTypes = ['resources': module.resources]
            sourceTypes += module.sourceMap
            sourceTypes.each {
                def type = it.key
                for (ssPath in it.value) {
                    def root = ssPath.normalize().toAbsolutePath().toFile()
                    while (weightMap.containsKey(root.getParent()) && weightMap.get(root.getParent()).get() <= 1) {
                        root = root.getParentFile()
                    }

                    Path rootPath = root.toPath().normalize().toAbsolutePath()
                    MergedSourceSet mergedSS
                    def paths = new HashSet<>(sourceSets.keySet())
                    for (path in paths) {
                        if (Utils.isAncestor(rootPath, path, true)) {
                            def vals = sourceSets.removeAll(path)
                            if (vals != null) {
                                sourceSets.putAll(rootPath, vals)
                            }
                        } else if (Utils.isAncestor(path, rootPath, false)) {
                            def roots = sourceSets.get(path)
                            for (mSS in roots) {
                                if (mSS.root == path) {
                                    mergedSS = mSS
                                    break
                                }
                            }
                            if (mergedSS == null) {
                                mergedSS = roots.first()
                            }
                            break
                        }
                        if (sourceSets.size() == 1) break
                    }

                    if (mergedSS == null) {
                        mergedSS = new MergedSourceSet(root: rootPath)
                        sourceSets.put(mergedSS.root, mergedSS)
                    }
                    mergedSS.typeMap.computeIfAbsent(type, { [] }).add(ssPath)
                }
            }
            sourceSets.asMap().entrySet().each { entry ->
                MergedSourceSet mSS = new MergedSourceSet(root: entry.key)
                entry.value.each {
                    it.typeMap.each {
                        mSS.typeMap.computeIfAbsent(it.key, { [] }).addAll(it.value)
                    }
                }
                mergeResults.put(module.name, entry.key, mSS)
            }

        }
        mergeResults
    }

    static class MergedSourceSet {
        Path root
        Map<String, List<Path>> typeMap = [:]
    }
}

