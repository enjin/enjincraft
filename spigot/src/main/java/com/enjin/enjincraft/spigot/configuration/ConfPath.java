package com.enjin.enjincraft.spigot.configuration;

public enum ConfPath {

    BASE_URL("platform.base-url"),
    APP_ID("platform.app.id"),
    APP_SECRET("platform.app.secret"),
    DEV_ADDRESS("platform.app.dev-address"),
    LOCALE("locale"),
    DEBUG_SDK("debug.sdk"),
    DEBUG_PLUGIN("debug.plugin"),
    SENTRY_URL("debug.sentry"),
    TRANSLATE_CONSOLE_MESSAGES("experimental.translate-console-messages"),
    SHOW_ID_LORE("plugin.show-id-in-lore"),
    PERMISSION_BLACKLIST("permission-blacklist"),
    LINK_PERMISSIONS("link-permissions");

    private final String path;

    ConfPath(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

}
