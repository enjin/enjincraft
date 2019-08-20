package com.enjin.ecmp.spigot.enums;

public enum CommandProcess {

    EXECUTE(true),
    TAB(false);

    boolean showErrorMessages;

    CommandProcess(boolean showErrorMessages) {
        showErrorMessages = showErrorMessages;
    }

    public boolean showErrorMessages() {
        return showErrorMessages;
    }
}
