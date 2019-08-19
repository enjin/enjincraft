package com.enjin.ecmp.spigot.enums;

import org.bukkit.command.CommandSender;

public enum Permission {

    CMD_BALANCE("balance"),
    CMD_HELP("help"),
    CMD_LINK("link"),
    CMD_SEND("send"),
    CMD_TRADE("trade"),
    CMD_UNLINK("unlink"),
    CMD_WALLET("wallet");

    private String node;

    Permission(String node) {
        this.node = String.format("ecmp.%s", node);
    }

    public String node() {
        return node;
    }

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(node);
    }
}
