package com.enjin.enjincraft.spigot.dependencies.classloader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class ReflectionClassLoader implements PluginClassLoader {

    private static final Method ADD_URL_METHOD;

    static {
        try {
            ADD_URL_METHOD = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            ADD_URL_METHOD.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private final URLClassLoader classLoader;

    public ReflectionClassLoader(Object plugin) throws IllegalStateException {
        ClassLoader classLoader = plugin.getClass().getClassLoader();

        if (!(classLoader instanceof URLClassLoader))
            throw new IllegalStateException("ClassLoader is not instance of URLClassLoader");

        this.classLoader = (URLClassLoader) classLoader;
    }

    @Override
    public void loadJar(Path path) {
        try {
            ADD_URL_METHOD.invoke(this.classLoader, path.toUri().toURL());
        } catch (IllegalAccessException | InvocationTargetException | MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

}
