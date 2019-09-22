package com.enjin.ecmp.spigot.dependencies.classloader;

import java.nio.file.Path;

public interface PluginClassLoader {

    void loadJar(Path path);

}
