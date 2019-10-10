package com.enjin.enjincraft.spigot.dependencies.classloader;

import java.nio.file.Path;

public interface PluginClassLoader {

    void loadJar(Path path);

}
