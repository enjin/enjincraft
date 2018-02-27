package io.enjincoin.spigot_framework;

import io.enjincoin.spigot_framework.controllers.SdkClientController;

/**
 * Interface for a bootstrapped application or plugin.
 */
public interface Bootstrap {

    void setUp();

    void tearDown();

    SdkClientController getSdkController();

}
