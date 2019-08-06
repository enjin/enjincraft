package com.enjin.ecmp.spigot.commands.subcommands;

import com.enjin.ecmp.spigot.BasePlugin;
import com.enjin.ecmp.spigot.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;

public class MenuCommand {

    private BasePlugin plugin;

    public MenuCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender, String[] args) {
        final TextComponent.Builder component = TextComponent.builder().content("").color(TextColor.GRAY);
        component.append(TextComponent.builder().content(" ").build());
        component.append(TextComponent.builder().content("ENJ MENU").color(TextColor.DARK_PURPLE).build());
        component.append(TextComponent.builder().content(" ").build());
        component.append(TextComponent.builder().content(" Click to: ").build());
        component.append(TextComponent.builder().content("Link Status").color(TextColor.DARK_AQUA)
                .clickEvent(ClickEvent.runCommand("/enj link")).build());
        component.append(TextComponent.builder().content(" | ").build());
        component.append(TextComponent.builder().content("Open Wallet").color(TextColor.DARK_AQUA)
                .clickEvent(ClickEvent.runCommand("/enj wallet")).build());
        component.append(TextComponent.builder().content(" | ").build());
        component.append(TextComponent.builder().content("Show Balance").color(TextColor.DARK_AQUA)
                .clickEvent(ClickEvent.runCommand("/enj balance")).build());
        component.append(TextComponent.builder().content(" | ").build());
        component.append(TextComponent.builder().content("Trade CryptoItem").color(TextColor.DARK_AQUA)
                .clickEvent(ClickEvent.runCommand("/enj trade")).build());

        MessageUtils.sendComponent(sender, component.build());

        return;
    }
}
