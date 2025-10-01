/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wstool.wrapper;

import net.covers1624.quack.maven.MavenNotation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 20/8/20.
 */
public final class WrapperProperties {

    public final MavenNotation artifact;
    public final String mirror;

    private WrapperProperties(MavenNotation artifact, String mirror) {
        if (!mirror.endsWith("/")) {
            mirror += "/";
        }

        this.artifact = artifact;
        this.mirror = mirror;
    }

    public static WrapperProperties load(Path overrides) throws IOException {
        Properties props = new Properties();
        try (InputStream is = WrapperProperties.class.getResourceAsStream("/wrapper.properties")) {
            props.load(is);
        }

        if (Files.exists(overrides)) {
            try (InputStream is = Files.newInputStream(overrides)) {
                props.load(is);
            }
        }

        return new WrapperProperties(
                MavenNotation.parse(requireNonNull(props.getProperty("artifact"))),
                requireNonNull(props.getProperty("mirror"))
        );
    }
}
