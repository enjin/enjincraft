package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.player.PlayerManager;
import com.enjin.ecmp.spigot.trade.TradeManager;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.model.service.identities.GetIdentities;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.List;

public class CmdTrade extends EnjCommand {

    private SpigotBootstrap bootstrap;

    public CmdTrade(SpigotBootstrap bootstrap) {
        super();
        this.bootstrap = bootstrap;
        this.aliases.add("trade");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.enjPlayer == null) return;

        if (context.args.size() > 0) {
            String sub = context.args.get(0);
            context.args.remove(0);
            if (sub.equalsIgnoreCase("invite")) {
                invite(context);
            } else if (sub.equalsIgnoreCase("accept")) {
                inviteAccept(context);
            } else if (sub.equalsIgnoreCase("decline")) {
                inviteDecline(context);
            }
        }
    }

    private void invite(CommandContext context) {
        if (context.args.size() > 0) {
            Player target = Bukkit.getPlayer(context.args.get(0));
            if (target != null) {
                if (target != context.sender) {
                    PlayerManager playerManager = bootstrap.getPlayerManager();
                    EnjPlayer senderEnjPlayer = context.enjPlayer;

                    if (!senderEnjPlayer.isLinked()) {
                        MessageUtils.sendComponent(context.sender, TextComponent.of("You must link your wallet before using this command."));
                        return;
                    }

                    EnjPlayer targetEnjPlayer = playerManager.getPlayer(target);

                    if (targetEnjPlayer == null || !targetEnjPlayer.isLinked()) {
                        MessageUtils.sendComponent(context.sender, TextComponent
                                .of(String.format("%s has not linked their wallet yet, a request has been sent.",
                                        target.getName())));
                        MessageUtils.sendComponent(target, TextComponent
                                .of(String.format("%s wants to trade with you. Please link your wallet to begin trading",
                                        context.sender.getName())));
                        return;
                    }

                    bootstrap.getTrustedPlatformClient().getIdentitiesService()
                            .getIdentitiesAsync(new GetIdentities().identityId(senderEnjPlayer.getIdentityId()), response -> {
                                if (response.isSuccess()) {
                                    GraphQLResponse<List<Identity>> body = response.body();
                                    if (body.isSuccess()) {
                                        List<Identity> data = body.getData();
                                        if (data != null && !data.isEmpty()) {
                                            Identity identity = data.get(0);
                                            BigInteger allowance = identity.getEnjAllowance();

                                            if (allowance == null || allowance.equals(BigInteger.ZERO)) {
                                                MessageUtils.sendComponent(context.sender, TextComponent
                                                        .of(String.format("%s has not approved the wallet allowance yet, a request has been sent.",
                                                                target.getName())));
                                                MessageUtils.sendComponent(target, TextComponent
                                                        .of(String.format("%s wants to trade with you. Please confirm the balance approval notification in your wallet.",
                                                                context.sender.getName())));
                                            } else {
                                                invite(senderEnjPlayer, targetEnjPlayer);
                                            }
                                        }
                                    }
                                }
                            });
                }
            }
        }
    }

    private void invite(EnjPlayer sender, EnjPlayer target) {
        TradeManager tradeManager = bootstrap.getTradeManager();
        boolean result = tradeManager.addInvite(sender, target);

        if (result) {
            MessageUtils.sendComponent(sender.getBukkitPlayer(), TextComponent.builder()
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
                            .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/enj trade accept %s", sender.getBukkitPlayer().getName())))
                            .build())
                    .append(TextComponent.of(" | "))
                    .append(TextComponent.builder("Decline")
                            .color(TextColor.RED)
                            .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/enj trade decline %s", sender.getBukkitPlayer().getName())))
                            .build());
            MessageUtils.sendComponent(target.getBukkitPlayer(), inviteMessageBuilder.build());
        } else {
            MessageUtils.sendComponent(sender.getBukkitPlayer(), TextComponent.builder()
                    .content(String.format("You have already invited %s to trade.", target.getBukkitPlayer().getName()))
                    .color(TextColor.RED)
                    .build());
        }
    }

    private void inviteAccept(CommandContext context) {
        if (context.args.size() > 0) {
            Player target = Bukkit.getPlayer(context.args.get(0));
            if (target != null) {
                EnjPlayer senderEnjPlayer = bootstrap.getPlayerManager().getPlayer(target);
                EnjPlayer targetEnjPlayer = context.enjPlayer;

                boolean result = bootstrap.getTradeManager().acceptInvite(senderEnjPlayer, targetEnjPlayer);

                if (!result) {
                    // TODO: No open invite or player is already in a trade
                }
            }
        }
    }

    private void inviteDecline(CommandContext context) {
        if (context.args.size() > 0) {
            Player target = Bukkit.getPlayer(context.args.get(0));

            if (target != null) {
                EnjPlayer senderEnjPlayer = bootstrap.getPlayerManager().getPlayer(target);
                EnjPlayer targetEnjPlayer = context.enjPlayer;

                boolean result = bootstrap.getTradeManager().declineInvite(senderEnjPlayer, targetEnjPlayer);

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
                    MessageUtils.sendComponent(context.sender, inviteTargetText);

                    TextComponent inviteSenderText = TextComponent.builder()
                            .content("")
                            .color(TextColor.GRAY)
                            .append(TextComponent.builder()
                                    .content(target.getName())
                                    .color(TextColor.GOLD)
                                    .build())
                            .append(TextComponent.of(" has declined your trade invite."))
                            .build();
                    MessageUtils.sendComponent(target, inviteSenderText);
                }
            }
        }
    }

}
