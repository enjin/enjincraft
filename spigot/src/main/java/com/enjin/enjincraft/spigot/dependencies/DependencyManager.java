package com.enjin.enjincraft.spigot.dependencies;

import com.enjin.enjincraft.spigot.EnjPlugin;
import com.enjin.enjincraft.spigot.dependencies.classloader.ReflectionClassLoader;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DependencyManager {

    private final EnjPlugin plugin;
    private final ReflectionClassLoader classLoader;

    public DependencyManager(EnjPlugin plugin) {
        this.plugin = plugin;
        this.classLoader = new ReflectionClassLoader(plugin);
    }

    public void loadDependencies(DependencyConfig config) {
        List<Path> paths = downloadDependencies(config);
        for (Path path : paths)
            classLoader.loadJar(path);
    }

    public List<Path> downloadDependencies(DependencyConfig config) {
        return downloadDependencies(getSaveDirectory(), config);
    }

    private Path getSaveDirectory() {
        Path saveDirectory = plugin.getDataFolder()
                .toPath()
                .resolve("lib");

        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to create lib directory", ex);
        }

        return saveDirectory;
    }

    private List<Path> downloadDependencies(Path targetDirectory, DependencyConfig config) {
        List<Path> paths = new ArrayList<>();

        for (Dependency dependency : config.getDependencies()) {
            try {
                paths.add(downloadDependency(targetDirectory, config, dependency));
            } catch (IllegalStateException ex) {
                plugin.log(ex);
            }
        }

        return paths;
    }

    private Path downloadDependency(Path targetDirectory, DependencyConfig config, Dependency dependency) {
        Path path = targetDirectory.resolve(dependency.getArtifactName());
        File file = path.toFile();
        if (file.exists())
            return path;

        plugin.getLogger().info("Downloading Dependency: " + dependency.getArtifactName());
        List<URL> urls = config.getArtifactUrls(dependency);

        boolean success = false;

        for (URL url : urls) {
            try {
                URLConnection connection = url.openConnection();

                connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
                connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(10));

                try (InputStream in = connection.getInputStream()) {
                    byte[] bytes = ByteStreams.toByteArray(in);
                    if (bytes.length == 0)
                        continue;

                    Files.write(path, bytes);

                    success = true;
                    break;
                }
            } catch (Exception ignore) {
            }
        }

        if (!success) {
            plugin.getLogger().warning("Dependency not found: " + dependency.getArtifactName());
            return null;
        }

        if (!file.exists())
            throw new IllegalStateException("Dependency not saved: " + dependency.getArtifactName());

        return path;
    }

}
