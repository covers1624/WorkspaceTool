package net.covers1624.wstool.test;

import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Created by covers1624 on 5/22/25.
 */
public class TestNeoForge extends TestBase {

    @Test
    public void testNF_1_21_1_ng() throws IOException {
        try (var test = newTestInstance("nf_1_21_1_ng")) {
            test.cloneRepo(
                    "CodeChicken/CodeChickenLib", "https://github.com/TheCBProject/CodeChickenLib", "master", "d6667cb693b2b28fb784a2d42987917b06f5114e"
            );
            test.emitWorkspaceFile("""
                    frameworks:
                      - type: 'neoforge:1.21'
                        path: "NeoForge"
                        url: "https://github.com/neoforged/NeoForge.git"
                        branch: "1.21.x"
                        commit: "f7a5bc85bff4ba5d5a2fd5e521eaa375d52dbadf"
                    
                    modules:
                      - "CodeChicken/**"
                    
                    workspace:
                      type: "intellij"
                      runs:
                      - name: Client
                        run_dir: ./run
                        config:
                          type: 'client'
                      - name: Client Login
                        run_dir: "./run"
                        config:
                          type: 'client'
                          dev_login: 'default'
                    """);
            test.run();
        }
    }

    @Test
    public void testNF_1_21_1_buildSrc() throws IOException {
        try (var test = newTestInstance("nf_1_21_1_buildSrc")) {
            test.cloneRepo(
                    "CodeChicken/CodeChickenLib", "https://github.com/TheCBProject/CodeChickenLib", "master", "d6667cb693b2b28fb784a2d42987917b06f5114e"
            );
            test.emitWorkspaceFile("""
                    frameworks:
                      - type: 'neoforge:1.21'
                        path: "NeoForge"
                        url: "https://github.com/neoforged/NeoForge.git"
                        branch: "1.21.x"
                        commit: "a9a8f46c05f70bdf85f5ba587af83d2b507de449"
                    
                    modules:
                      - "CodeChicken/**"
                    
                    workspace:
                      type: "intellij"
                      runs:
                      - name: Client
                        run_dir: ./run
                        config:
                          type: 'client'
                    """);
            test.run();
        }
    }

    @Test
    public void testNF_1_21_4() throws IOException {
        try (var test = newTestInstance("nf_1_21_4")) {
            test.cloneRepo(
                    "CodeChicken/CodeChickenLib", "https://github.com/TheCBProject/CodeChickenLib", "master", "d6667cb693b2b28fb784a2d42987917b06f5114e"
            );
            test.emitWorkspaceFile("""
                    frameworks:
                      - type: 'neoforge:1.21.4'
                        path: "NeoForge"
                        url: "https://github.com/neoforged/NeoForge.git"
                        branch: "1.21.x"
                        commit: "28b7490e5863f57281f778e40654c710a5306151"
                    
                    modules:
                      - "CodeChicken/**"
                    
                    workspace:
                      type: "intellij"
                      runs:
                      - name: Client
                        run_dir: ./run
                        config:
                          type: 'client'
                    """);
            // TODO, for some unknown reason the coremods project is flaky in CI, i have no idea why.
            test.ignoreFile("modules/nf_1_21_4.NeoForge-Root.neoforge-coremods.main.iml");
            test.run();
        }
    }
}
