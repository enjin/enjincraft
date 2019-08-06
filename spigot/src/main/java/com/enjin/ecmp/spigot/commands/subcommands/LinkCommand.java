package com.enjin.ecmp.spigot.commands.subcommands;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.ECPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.UuidUtils;
import com.enjin.java_commons.StringUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LinkCommand {

    private SpigotBootstrap bootstrap;
    private final TextComponent newline = TextComponent.of("");

    public LinkCommand(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void execute(CommandSender sender, String[] args) {
        UUID uuid = null;

        if (sender instanceof Player) {
            Player player = (Player) sender;
            uuid = player.getUniqueId();
        } else {
            if (args.length >= 1) {
                try {
                    uuid = UuidUtils.stringToUuid(args[0]);
                } catch (IllegalArgumentException e) {
                    errorInvalidUuid(sender);
                }
            } else {
                final TextComponent text = TextComponent.of("UUID argument required.")
                        .color(TextColor.RED);
                MessageUtils.sendComponent(sender, text);
            }
        }

        if (uuid != null) {
            ECPlayer ecPlayer = bootstrap.getPlayerManager().getPlayer(uuid);
            if (ecPlayer != null) {
                if (ecPlayer.isLoaded()) {
                    if (StringUtils.isEmpty(ecPlayer.getLinkingCode()))
                        handleAddress(sender, ecPlayer.getEthereumAddress());
                    else
                        handleCode(sender, ecPlayer.getLinkingCode());
                }
            }
        } else {
            errorInvalidUuid(sender);
        }
    }

    private void errorInvalidUuid(CommandSender sender) {
        final TextComponent text = TextComponent.of("The UUID provided is invalid.")
                .color(TextColor.RED);

        MessageUtils.sendComponent(sender, newline);
        MessageUtils.sendComponent(sender, text);
    }

    private void handleAddress(CommandSender sender, String address) {
        if (address == null || address.isEmpty()) {

            final TextComponent text = TextComponent.of("Could not acquire ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of("Ethereum Address.")
                            .color(TextColor.GOLD));

            MessageUtils.sendComponent(sender, newline);
            MessageUtils.sendComponent(sender, text);
        } else {
            final TextComponent text = TextComponent.of("Player account already linked to address: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of(address)
                            .color(TextColor.GOLD));

            MessageUtils.sendComponent(sender, newline);
            MessageUtils.sendComponent(sender, text);
        }
    }

    private void handleCode(CommandSender sender, String code) {
        if (code == null || code.isEmpty()) {
            final TextComponent text = TextComponent.of("Could not acquire a player identity code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of("code not present or already linked.")
                            .color(TextColor.GOLD));

            MessageUtils.sendComponent(sender, newline);
            MessageUtils.sendComponent(sender, text);
        } else {
            final TextComponent notice = TextComponent.of("Please link your account by downloading the Enjin Wallet for Android or iOS and browsing to Linked Apps. Enter the Identity Code shown below:")
                    .color(TextColor.GOLD);

            final TextComponent text = TextComponent.of("Identity Code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of(code)
                            .color(TextColor.GOLD));

            MessageUtils.sendComponent(sender, newline);
            MessageUtils.sendComponent(sender, notice);
            MessageUtils.sendComponent(sender, newline);
            MessageUtils.sendComponent(sender, text);
        }
    }

}
