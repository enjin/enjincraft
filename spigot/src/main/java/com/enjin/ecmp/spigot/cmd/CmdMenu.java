package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;

public class CmdMenu extends EnjCommand {

    public CmdMenu(SpigotBootstrap bootstrap) {
        super(bootstrap);
        setAllowedSenderTypes(SenderType.PLAYER);
        this.aliases.add("menu");
    }

    @Override
    public void execute(CommandContext context) {
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
        MessageUtils.sendComponent(context.sender, component.build());
    }

}
