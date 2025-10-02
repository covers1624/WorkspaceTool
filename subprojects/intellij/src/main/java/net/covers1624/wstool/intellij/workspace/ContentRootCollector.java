package net.covers1624.wstool.intellij.workspace;

import net.covers1624.quack.collection.FastStream;

import java.nio.file.Path;
import java.util.*;

/**
 * Created by covers1624 on 10/2/25.
 */
public class ContentRootCollector {

    private final Map<Path, Integer> weightMap = new HashMap<>();

    private Path findRoot(Path path) {
        while (weightMap.containsKey(path.getParent()) && weightMap.get(path.getParent()) <= 1) {
            path = path.getParent();
        }
        return path;
    }

    private void addWeights(Iterable<Path> itr) {
        itr.forEach(s -> weightMap.compute(s, (e, w) -> w != null ? w + 1 : 1));
    }

    public void processModule(IJModule module) {
        if (module instanceof IJModuleWithPath pathModule) {
            addWeights(getPaths(pathModule.rootDir()));
        }
        addWeights(FastStream.of(module.getContentPaths())
                .flatMap(e -> getPaths(e.path()))
        );
    }

    public List<ContentRoot> buildRoots(IJModule module) {
        Map<Path, List<MutableRoot>> roots = new LinkedHashMap<>();
        for (ContentPath contentPath : module.getContentPaths()) {
            Path rootPath = findRoot(contentPath.path().getParent());

            MutableRoot found = null;
            for (Path path : roots.keySet().toArray(Path[]::new)) {
                if (!rootPath.equals(path) && rootPath.startsWith(path)) {
                    var paths = roots.remove(path);
                    if (paths != null) {
                        roots.computeIfAbsent(rootPath, e -> new ArrayList<>())
                                .addAll(paths);
                    }
                } else if (path.startsWith(rootPath)) {
                    var paths = roots.get(path);
                    found = FastStream.of(paths)
                            .filter(e -> e.root.equals(path)) // Prefer exact
                            .firstOrDefault(paths.getFirst()); // Otherwise meh
                    break;
                }
                // Pointless
                if (roots.size() == 1) break;
            }

            if (found == null) {
                found = new MutableRoot(rootPath, new ArrayList<>());
                roots.computeIfAbsent(rootPath, e -> new ArrayList<>())
                        .add(found);
            }
            found.paths.add(contentPath);
        }

        // If there are no roots we have found a group or root module with no excludes, etc.
        // Intellij still requires these modules get a content root otherwise they disappear in the project view.
        // If they aren't backed by a path, we can't do anything (current this means we have a broken source set).
        if (roots.isEmpty()) {
            if (!(module instanceof IJModuleWithPath moduleWithPath)) throw new RuntimeException("Unable to build content root for an empty common parent.");

            return List.of(new ContentRoot(moduleWithPath.rootDir(), List.of()));
        }

        return FastStream.of(roots.entrySet())
                .map(e ->
                        new ContentRoot(
                                e.getKey(),
                                FastStream.of(e.getValue())
                                        .flatMap(e2 -> e2.paths)
                                        .toList())
                )
                .toList();
    }

    private Set<Path> getPaths(Path path) {
        Set<Path> paths = new LinkedHashSet<>();
        while (path != null) {
            paths.add(path);
            path = path.getParent();
        }
        return paths;
    }

    private record MutableRoot(Path root, List<ContentPath> paths) { }
}
