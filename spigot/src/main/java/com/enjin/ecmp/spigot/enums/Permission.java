package com.enjin.ecmp.spigot.enums;

import org.bukkit.command.CommandSender;

public enum Permission {

    CMD_BALANCE("balance"),
    CMD_HELP("help"),
    CMD_LINK("link"),
    CMD_SEND("send"),
    CMD_CONF("conf"),
    CMD_CONF_SET("conf.set"),
    CMD_TRADE("trade"),
    CMD_TRADE_INVITE("trade.invite"),
    CMD_TRADE_ACCEPT("trade.accept"),
    CMD_TRADE_DECLINE("trade.decline"),
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
