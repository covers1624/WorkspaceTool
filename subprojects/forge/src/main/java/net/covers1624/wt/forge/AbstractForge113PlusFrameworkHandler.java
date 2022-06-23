/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.forge.api.script.ForgeFramework;
import net.covers1624.wt.forge.util.AtFile;
import net.covers1624.wt.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

import static net.covers1624.quack.util.SneakyUtils.sneaky;

/**
 * Created by covers1624 on 26/10/21.
 */
public abstract class AbstractForge113PlusFrameworkHandler<T extends ForgeFramework> extends AbstractForgeFrameworkHandler<T> {

    protected AbstractForge113PlusFrameworkHandler(WorkspaceToolContext context) {
        super(context);
    }

    protected void handleAts() {
        Path cachedForgeAt = context.cacheDir.resolve("forge_accesstransformer.cfg");
        Path forgeAt = forgeDir.resolve("src/main/resources/META-INF/accesstransformer.cfg");
        Path mergedAt = context.cacheDir.resolve("merged_at.cfg");
        if (wasCloned || !Files.exists(cachedForgeAt)) {
            sneaky(() -> Files.copy(forgeAt, cachedForgeAt, StandardCopyOption.REPLACE_EXISTING));
        }
        {//AccessTransformers
            Hasher mergedHasher = SHA_256.newHasher();
            List<Path> atFiles = context.modules.parallelStream()
                    .flatMap(e -> e.getSourceSets().values().stream())
                    .flatMap(e -> e.getResources().stream())
                    .filter(Files::exists)
                    .flatMap(e -> sneaky(() -> Files.walk(e).filter(f -> f.getFileName().toString().equals("accesstransformer.cfg"))))
                    .collect(Collectors.toList());
            atFiles.forEach(e -> LOGGER.info("Found AccessTransformer: {}", e));
            atFiles.forEach(e -> Utils.addToHasher(mergedHasher, e));
            Utils.addToHasher(mergedHasher, cachedForgeAt);
            HashCode mergedHash = mergedHasher.hash();
            if (hashContainer.check(HASH_MERGED_AT, mergedHash) || Files.notExists(mergedAt)) {
                needsSetup = true;
                hashContainer.set(HASH_MARKER_SETUP, MARKER_HASH);
                AtFile atFile = new AtFile().useDot();
                atFiles.stream().map(AtFile::new).forEach(atFile::merge);
                atFile.merge(new AtFile(cachedForgeAt));
                atFile.write(mergedAt);
                hashContainer.set(HASH_MERGED_AT, mergedHash);
            }
            sneaky(() -> Files.copy(mergedAt, forgeAt, StandardCopyOption.REPLACE_EXISTING));
        }
    }
}
