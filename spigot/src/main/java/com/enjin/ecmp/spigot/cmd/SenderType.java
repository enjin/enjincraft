package com.enjin.ecmp.spigot.cmd;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public enum SenderType {

    BLOCK(BlockCommandSender.class),
    CONSOLE(ConsoleCommandSender.class),
    REMOTE_CONSOLE(RemoteConsoleCommandSender.class),
    PLAYER(Player.class),
    ANY(CommandSender.class);

    private Class<? extends CommandSender> instanceSuperClass;

    SenderType(Class<? extends CommandSender> instanceSuperClass) {
        this.instanceSuperClass = instanceSuperClass;
    }

    SenderType() {
        this(null);
    }

    public static SenderType type(CommandSender sender) {
        for (SenderType type : values()) {
            if (type.instanceSuperClass.isInstance(sender)) return type;
        }
        return ANY;
    }

}
