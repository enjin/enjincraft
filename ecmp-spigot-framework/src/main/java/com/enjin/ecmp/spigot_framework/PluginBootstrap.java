package com.enjin.ecmp.spigot_framework;

/**
 * Abstract bootstrap to allow for optional set up and tear down.
 *
 * @since 1.0
 */
public abstract class PluginBootstrap implements Bootstrap {

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
    }

}
