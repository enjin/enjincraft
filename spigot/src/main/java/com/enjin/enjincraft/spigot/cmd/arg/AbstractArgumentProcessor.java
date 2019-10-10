package com.enjin.enjincraft.spigot.cmd.arg;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractArgumentProcessor<T> implements ArgumentProcessor<T> {

    @Override
    public List<String> tab(CommandSender sender, String arg) {
        return new ArrayList<>();
    }

}
