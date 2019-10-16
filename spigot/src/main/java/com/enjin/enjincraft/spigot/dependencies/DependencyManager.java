package com.enjin.enjincraft.spigot.dependencies;

import com.enjin.enjincraft.spigot.dependencies.classloader.ReflectionClassLoader;
import com.google.common.io.ByteStreams;
import org.bukkit.plugin.Plugin;

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

    private Plugin plugin;
    private ReflectionClassLoader classLoader;

    public DependencyManager(Plugin plugin) {
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
        Path saveDirectory = plugin.getDataFolder().toPath().resolve("lib");

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
            } catch (IOException | IllegalStateException ex) {
                ex.printStackTrace();
            }
        }

        return paths;
    }

    private Path downloadDependency(Path targetDirectory, DependencyConfig config, Dependency dependency) throws IOException {
        Path file = targetDirectory.resolve(dependency.getArtifactName());

        if (Files.exists(file))
            return file;

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

                    Files.write(file, bytes);

                    success = true;
                    break;
                }
            } catch (Exception ignore) {
                // Do Nothing
            }
        }

        if (!success) {
            plugin.getLogger().warning("Dependency not found: " + dependency.getArtifactName());
            return null;
        }

        if (!Files.exists(file))
            throw new IllegalStateException("Dependency not saved: " + dependency.getArtifactName());

        return file;
    }

}
