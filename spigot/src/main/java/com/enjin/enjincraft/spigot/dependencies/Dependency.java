package com.enjin.enjincraft.spigot.dependencies;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
public class Dependency {

    private String groupId;
    private String artifactId;
    private String version;
    private Optional<String> classifier;

    public Dependency(ConfigurationSection section) {
        groupId = section.getString("groupId");
        artifactId = section.getString("artifactId");
        version = section.getString("version");
        classifier = Optional.ofNullable(section.getString("classifier", null));
    }

    public String getArtifactName() {
        StringBuilder builder = new StringBuilder(artifactId)
                .append('-')
                .append(version);

        classifier.ifPresent(classifier -> builder.append('-').append(classifier));

        return builder.append(".jar")
                .toString();
    }

    public String getPath() {
        StringBuilder builder = new StringBuilder(groupId.replace(".", "/"))
                .append('/')
                .append(artifactId)
                .append('/')
                .append(version)
                .append('/')
                .append(getArtifactName());

        return builder.toString();
    }

    public static List<Dependency> process(ConfigurationSection section) {
        List<Dependency> dependencies = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key))
                continue;

            dependencies.add(new Dependency(Objects.requireNonNull(section.getConfigurationSection(key))));
        }

        return dependencies;
    }

}
