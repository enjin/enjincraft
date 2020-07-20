package com.enjin.enjincraft.spigot.dependencies;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;

    public Dependency(ConfigurationSection section) {
        groupId = section.getString("groupId");
        artifactId = section.getString("artifactId");
        version = section.getString("version");
        classifier = section.getString("classifier");
    }

    public String getArtifactName() {
        StringBuilder builder = new StringBuilder();

        builder.append(artifactId);
        builder.append('-');
        builder.append(version);

        if (classifier != null) {
            builder.append('-');
            builder.append(classifier);
        }

        builder.append(".jar");

        return builder.toString();
    }

    public String getPath() {
        return groupId.replace(".", "/")
                + '/'
                + artifactId
                + '/'
                + version
                + '/'
                + getArtifactName();
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
