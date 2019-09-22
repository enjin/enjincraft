package com.enjin.ecmp.spigot.dependencies;

import org.bukkit.configuration.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class DependencyConfig {

    private List<String> repositories;
    private List<Dependency> dependencies;

    private DependencyConfig(Configuration configuration) {
        this.repositories = Collections.unmodifiableList(getRepositories(configuration));
        this.dependencies = Collections.unmodifiableList(getDependencies(configuration));
    }

    public List<String> getRepositories() {
        return repositories;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public List<URL> getArtifactUrls(Dependency dependency) {
        List<URL> urls = new ArrayList<>();

        for (String repository : repositories) {
            StringBuilder builder = new StringBuilder(repository);

            if (!repository.endsWith("/"))
                builder.append('/');

            builder.append(dependency.getPath());

            try {
                urls.add(new URL(builder.toString()));
            } catch (MalformedURLException ignored) {
                // Do Nothing
            }
        }

        return urls;
    }

    public static DependencyConfig create(Configuration configuration) {
        return new DependencyConfig(configuration);
    }

    private static List<String> getRepositories(Configuration configuration) {
        if (!configuration.isList("repositories"))
            return new ArrayList<>(0);

        return configuration.getStringList("repositories");
    }

    private static List<Dependency> getDependencies(Configuration configuration) {
        if (!configuration.isConfigurationSection("dependencies"))
            return new ArrayList<>(0);

        return Dependency.process(Objects.requireNonNull(configuration.getConfigurationSection("dependencies")));
    }
}
