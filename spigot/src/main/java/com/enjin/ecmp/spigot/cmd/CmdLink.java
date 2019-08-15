package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.java_commons.StringUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;

public class CmdLink extends EnjCommand {

    public CmdLink(SpigotBootstrap bootstrap) {
        super(bootstrap);
        setAllowedSenderTypes(SenderType.PLAYER);
        this.aliases.add("link");
    }

    @Override
    public void execute(CommandContext context) {
        EnjPlayer enjPlayer = context.enjPlayer;

        if (enjPlayer == null || !enjPlayer.isLoaded()) return;

        if (enjPlayer.isLinked()) {
            handleAddress(context.sender, enjPlayer.getEthereumAddress());
        } else {
            handleCode(context.sender, enjPlayer.getLinkingCode());
        }
    }

    private void handleAddress(CommandSender sender, String address) {
        if (StringUtils.isEmpty(address)) {
            TextComponent text = TextComponent.of("Could not acquire ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of("Ethereum Address.")
                            .color(TextColor.GOLD));

            MessageUtils.sendComponent(sender, TextComponent.of(""));
            MessageUtils.sendComponent(sender, text);
        } else {
            TextComponent text = TextComponent.of("Player account already linked to address: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of(address)
                            .color(TextColor.GOLD));

            MessageUtils.sendComponent(sender, TextComponent.of(""));
            MessageUtils.sendComponent(sender, text);
        }
    }

    private void handleCode(CommandSender sender, String code) {
        if (StringUtils.isEmpty(code)) {
            TextComponent text = TextComponent.of("Could not acquire a player identity code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of("code not present or already linked.")
                            .color(TextColor.GOLD));

            MessageUtils.sendComponent(sender, TextComponent.of(""));
            MessageUtils.sendComponent(sender, text);
        } else {
            TextComponent notice = TextComponent.of("Please link your account by downloading the Enjin Wallet for Android or iOS and browsing to Linked Apps. Enter the Identity Code shown below:")
                    .color(TextColor.GOLD);
            TextComponent text = TextComponent.of("Identity Code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of(code)
                            .color(TextColor.GOLD));

            MessageUtils.sendComponent(sender, TextComponent.of(""));
            MessageUtils.sendComponent(sender, notice);
            MessageUtils.sendComponent(sender, TextComponent.of(""));
            MessageUtils.sendComponent(sender, text);
        }
    }

}
