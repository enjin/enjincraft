package com.enjin.ecmp.spigot.commands.subcommands;

import com.enjin.ecmp.spigot.BasePlugin;
import com.enjin.ecmp.spigot.player.EnjinCoinPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.UuidUtils;
import com.enjin.java_commons.StringUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LinkCommand {

    private BasePlugin plugin;

    /**
     * <p>Empty line helper for MessageUtils.sendMessage</p>
     */
    private final TextComponent newline = TextComponent.of("");

    /**
     * <p>Link command handler constructor.</p>
     *
     * @param plugin the Spigot plugin
     */
    public LinkCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * <p>Executes and performs operations defined for the command.</p>
     *
     * @param sender the command sender
     * @param args   the command arguments
     * @since 1.0
     */
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
                MessageUtils.sendMessage(sender, text);
            }
        }

        if (uuid != null) {
            EnjinCoinPlayer enjinCoinPlayer = this.plugin.getBootstrap().getPlayerManager().getPlayer(uuid);
            if (enjinCoinPlayer != null) {
                if (enjinCoinPlayer.isLoaded()) {
                    if (StringUtils.isEmpty(enjinCoinPlayer.getLinkingCode()))
                        handleAddress(sender, enjinCoinPlayer.getEthereumAddress());
                    else
                        handleCode(sender, enjinCoinPlayer.getLinkingCode());
                }
            }
        } else {
            errorInvalidUuid(sender);
        }
    }

    private void errorInvalidUuid(CommandSender sender) {
        final TextComponent text = TextComponent.of("The UUID provided is invalid.")
                .color(TextColor.RED);

        MessageUtils.sendMessage(sender, newline);
        MessageUtils.sendMessage(sender, text);
    }

    private void handleAddress(CommandSender sender, String address) {
        if (address == null || address.isEmpty()) {

            final TextComponent text = TextComponent.of("Could not acquire ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of("Ethereum Address.")
                            .color(TextColor.GOLD));

            MessageUtils.sendMessage(sender, newline);
            MessageUtils.sendMessage(sender, text);
        } else {
            final TextComponent text = TextComponent.of("Player account already linked to address: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of(address)
                            .color(TextColor.GOLD));

            MessageUtils.sendMessage(sender, newline);
            MessageUtils.sendMessage(sender, text);
        }
    }

    private void handleCode(CommandSender sender, String code) {
        if (code == null || code.isEmpty()) {
            final TextComponent text = TextComponent.of("Could not acquire a player identity code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of("code not present or already linked.")
                            .color(TextColor.GOLD));

            MessageUtils.sendMessage(sender, newline);
            MessageUtils.sendMessage(sender, text);
        } else {
            final TextComponent notice = TextComponent.of("Please link your account by downloading the Enjin Wallet for Android or iOS and browsing to Linked Apps. Enter the Identity Code shown below:")
                    .color(TextColor.GOLD);

            final TextComponent text = TextComponent.of("Identity Code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of(code)
                            .color(TextColor.GOLD));

            MessageUtils.sendMessage(sender, newline);
            MessageUtils.sendMessage(sender, notice);
            MessageUtils.sendMessage(sender, newline);
            MessageUtils.sendMessage(sender, text);
        }
    }

}
