package com.enjin.ecmp.spigot_framework.commands.subcommands;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.ecmp.spigot_framework.player.PlayerManager;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.model.service.identities.GetIdentities;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.service.identities.IdentitiesService;
import com.enjin.ecmp.spigot_framework.player.EnjinCoinPlayer;
import com.enjin.ecmp.spigot_framework.trade.TradeManager;
import com.enjin.ecmp.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

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
                    EnjinCoinPlayer senderMP = playerManager.getPlayer(sender.getUniqueId());

                    if (!senderMP.isLinked()) {
                        MessageUtils.sendMessage(sender, TextComponent.of("You must link your wallet before using this command."));
                        return;
                    }

                    EnjinCoinPlayer targetMP = playerManager.getPlayer(target.getUniqueId());

                    if (targetMP == null || !targetMP.isLinked()) {
                        MessageUtils.sendMessage(sender, TextComponent
                                .of(String.format("%s has not linked their wallet yet, a request has been sent.",
                                        target.getName())));
                        MessageUtils.sendMessage(target, TextComponent
                                .of(String.format("%s wants to trade with you. Please link your wallet to begin trading",
                                        sender.getName())));
                        return;
                    }

                    IdentitiesService service = plugin.getBootstrap().getTrustedPlatformClient().getIdentitiesService();
                    service.getIdentitiesAsync(new GetIdentities().identityId(senderMP.getIdentityId()), response -> {
                        if (response.isSuccess()) {
                            GraphQLResponse<List<Identity>> body = response.body();
                            if (body.isSuccess()) {
                                List<Identity> data = body.getData();
                                if (data != null && !data.isEmpty()) {
                                    Identity identity = data.get(0);
                                    BigInteger allowance = identity.getEnjAllowance();

                                    if (allowance == null || allowance.equals(BigInteger.ZERO)) {
                                        MessageUtils.sendMessage(sender, TextComponent
                                                .of(String.format("%s has not approved the wallet allowance yet, a request has been sent.",
                                                        target.getName())));
                                        MessageUtils.sendMessage(target, TextComponent
                                                .of(String.format("%s wants to trade with you. Please confirm the balance approval notification in your wallet.",
                                                        sender.getName())));
                                    } else {
                                        invite(senderMP, targetMP);
                                    }
                                }
                            }
                        }
                    });
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

    private void invite(EnjinCoinPlayer sender, EnjinCoinPlayer target) {
        TradeManager tradeManager = this.plugin.getBootstrap().getTradeManager();
        boolean result = tradeManager.addInvite(sender, target);

        if (result) {
            MessageUtils.sendMessage(sender.getBukkitPlayer(), TextComponent.builder()
                    .content(String.format("Trade invite with %s has been sent!", target.getBukkitPlayer().getName()))
                    .color(TextColor.GREEN)
                    .build());
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
            MessageUtils.sendMessage(sender.getBukkitPlayer(), TextComponent.builder()
                    .content(String.format("You have already invited %s to trade.", target.getBukkitPlayer().getName()))
                    .color(TextColor.RED)
                    .build());
        }
    }

    private void inviteAccept(Player sender, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
                TradeManager tradeManager = this.plugin.getBootstrap().getTradeManager();
                EnjinCoinPlayer senderMP = playerManager.getPlayer(target.getUniqueId());
                EnjinCoinPlayer targetMP = playerManager.getPlayer(sender.getUniqueId());

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
                EnjinCoinPlayer senderMP = playerManager.getPlayer(target.getUniqueId());
                EnjinCoinPlayer targetMP = playerManager.getPlayer(sender.getUniqueId());

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
