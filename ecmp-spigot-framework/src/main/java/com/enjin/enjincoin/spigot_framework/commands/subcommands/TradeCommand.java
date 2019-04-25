package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.http.Callback;
import com.enjin.enjincoin.sdk.http.Result;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.service.ethereum.EthereumService;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.player.PlayerManager;
import com.enjin.enjincoin.spigot_framework.trade.TradeManager;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

public class TradeCommand {

    private BasePlugin plugin;

    private final TextComponent newline = TextComponent.of("");

    public TradeCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(Player sender, String[] args) {
        if (args.length > 0) {
            String sub = args[0];
            String[] subArgs = args.length == 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
            if (sub.equalsIgnoreCase("invite")) {
                invite(sender, subArgs);
            } else if (sub.equalsIgnoreCase("accept")) {
                inviteAccept(sender, subArgs);
            } else if (sub.equalsIgnoreCase("decline")) {
                inviteDecline(sender, subArgs);
            }
        }
    }

    private void invite(Player sender, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                if (target != sender) {
                    PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
                    MinecraftPlayer senderMP = playerManager.getPlayer(sender.getUniqueId());

                    if (senderMP == null || !senderMP.isLoaded()) {
                        MessageUtils.sendMessage(sender, TextComponent.of("You must link your wallet before using this command."));
                        return;
                    }

                    MinecraftPlayer targetMP = playerManager.getPlayer(target.getUniqueId());

                    if (targetMP == null || !targetMP.isLoaded()) {
                        MessageUtils.sendMessage(sender, TextComponent.of("That player has not linked a wallet."));
                        return;
                    }

                    EthereumService ethereumService = this.plugin.getBootstrap().getSdkController().getClient()
                            .getEthereumService();

                    ethereumService.getAllowanceAsync(senderMP.getIdentity().getEthereumAddress(), r1 -> {
                        if (r1.isSuccess()) {
                            if (r1.body() == null || r1.body().equals(BigInteger.ZERO)) {
                                MessageUtils.sendMessage(sender, TextComponent.of("Your allowance is not set. Please confirm the request in your wallet app."));
                            } else {
                                try {
                                    Result<BigInteger> r2 = ethereumService.getAllowanceSync(targetMP.getIdentity().getEthereumAddress());
                                    if (r2.body() == null || r2.body().equals(BigInteger.ZERO)) {
                                        MessageUtils.sendMessage(sender, TextComponent.of("The other player is not capable of trading at this time."));
                                    } else {
                                        invite(senderMP, targetMP);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                    invite(senderMP, targetMP);
                } else {
                    // TODO: Error: cannot invite yourself to trade!
                }
            } else {
                // TODO: Error: could not find the player you specified!
            }
        } else {
            // TODO: Display some form of player selection ui.
        }
    }

    private void invite(MinecraftPlayer sender, MinecraftPlayer target) {
        TradeManager tradeManager = this.plugin.getBootstrap().getTradeManager();
        boolean result = tradeManager.addInvite(sender, target);

        if (result) {
            final TextComponent.Builder inviteMessageBuilder = TextComponent.builder("")
                    .color(TextColor.GRAY)
                    .append(TextComponent.builder(String.format("%s", sender.getBukkitPlayer().getName()))
                            .color(TextColor.GOLD)
                            .build())
                    .append(TextComponent.of(" has invited you to trade. "))
                    .append(TextComponent.builder("Accept")
                            .color(TextColor.GREEN)
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/enj trade accept %s", sender.getBukkitPlayer().getName())))
                            .build())
                    .append(TextComponent.of(" | "))
                    .append(TextComponent.builder("Decline")
                            .color(TextColor.RED)
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/enj trade decline %s", sender.getBukkitPlayer().getName())))
                            .build());
            MessageUtils.sendMessage(target.getBukkitPlayer(), inviteMessageBuilder.build());
        } else {
            // TODO: Info: a trade invite with that player is already open!
        }
    }

    private void inviteAccept(Player sender, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
                TradeManager tradeManager = this.plugin.getBootstrap().getTradeManager();
                MinecraftPlayer senderMP = playerManager.getPlayer(target.getUniqueId());
                MinecraftPlayer targetMP = playerManager.getPlayer(sender.getUniqueId());

                boolean result = tradeManager.acceptInvite(senderMP, targetMP);

                if (!result) {
                    // TODO: No open invite or player is already in a trade
                }
            }
        } else {
            // TODO: Error: no player name was provided!
        }
    }

    private void inviteDecline(Player sender, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);

            if (target != null) {
                PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
                TradeManager tradeManager = this.plugin.getBootstrap().getTradeManager();
                MinecraftPlayer senderMP = playerManager.getPlayer(target.getUniqueId());
                MinecraftPlayer targetMP = playerManager.getPlayer(sender.getUniqueId());

                boolean result = tradeManager.declineInvite(senderMP, targetMP);

                if (result) {
                    TextComponent inviteTargetText = TextComponent.builder()
                            .content("You have declined ")
                            .color(TextColor.GRAY)
                            .append(TextComponent.builder()
                                    .content(target.getName())
                                    .color(TextColor.GOLD)
                                    .build())
                            .append(TextComponent.of("'s trade invite."))
                            .build();
                    MessageUtils.sendMessage(sender, inviteTargetText);

                    TextComponent inviteSenderText = TextComponent.builder()
                            .content("")
                            .color(TextColor.GRAY)
                            .append(TextComponent.builder()
                                    .content(target.getName())
                                    .color(TextColor.GOLD)
                                    .build())
                            .append(TextComponent.of(" has declined your trade invite."))
                            .build();
                    MessageUtils.sendMessage(target, inviteSenderText);
                } else {
                    // TODO: No trades from sender found
                }
            }
        } else {
            // TODO: Error: no player name was provided!
        }
    }
}
