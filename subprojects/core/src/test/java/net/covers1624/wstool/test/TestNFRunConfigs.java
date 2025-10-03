package net.covers1624.wstool.test;

import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Created by covers1624 on 5/22/25.
 */
public class TestNFRunConfigs extends TestBase {

    @Test
    public void testNF_1_21_1() throws IOException {
        try (var test = newTestInstance("nf_1_21_1_runs")) {
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
}
