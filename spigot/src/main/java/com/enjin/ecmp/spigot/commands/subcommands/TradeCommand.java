package com.enjin.ecmp.spigot.commands.subcommands;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.EnjinCoinPlayer;
import com.enjin.ecmp.spigot.player.PlayerManager;
import com.enjin.ecmp.spigot.trade.TradeManager;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.model.service.identities.GetIdentities;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.service.identities.IdentitiesService;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class TradeCommand {

    private SpigotBootstrap bootstrap;

    public TradeCommand(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
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
                    PlayerManager playerManager = bootstrap.getPlayerManager();
                    EnjinCoinPlayer senderMP = playerManager.getPlayer(sender.getUniqueId());

                    if (!senderMP.isLinked()) {
                        MessageUtils.sendComponent(sender, TextComponent.of("You must link your wallet before using this command."));
                        return;
                    }

                    EnjinCoinPlayer targetMP = playerManager.getPlayer(target.getUniqueId());

                    if (targetMP == null || !targetMP.isLinked()) {
                        MessageUtils.sendComponent(sender, TextComponent
                                .of(String.format("%s has not linked their wallet yet, a request has been sent.",
                                        target.getName())));
                        MessageUtils.sendComponent(target, TextComponent
                                .of(String.format("%s wants to trade with you. Please link your wallet to begin trading",
                                        sender.getName())));
                        return;
                    }

                    IdentitiesService service = bootstrap.getTrustedPlatformClient().getIdentitiesService();
                    service.getIdentitiesAsync(new GetIdentities().identityId(senderMP.getIdentityId()), response -> {
                        if (response.isSuccess()) {
                            GraphQLResponse<List<Identity>> body = response.body();
                            if (body.isSuccess()) {
                                List<Identity> data = body.getData();
                                if (data != null && !data.isEmpty()) {
                                    Identity identity = data.get(0);
                                    BigInteger allowance = identity.getEnjAllowance();

                                    if (allowance == null || allowance.equals(BigInteger.ZERO)) {
                                        MessageUtils.sendComponent(sender, TextComponent
                                                .of(String.format("%s has not approved the wallet allowance yet, a request has been sent.",
                                                        target.getName())));
                                        MessageUtils.sendComponent(target, TextComponent
                                                .of(String.format("%s wants to trade with you. Please confirm the balance approval notification in your wallet.",
                                                        sender.getName())));
                                    } else {
                                        invite(senderMP, targetMP);
                                    }
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    private void invite(EnjinCoinPlayer sender, EnjinCoinPlayer target) {
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

    private void inviteAccept(Player sender, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                EnjinCoinPlayer senderMP = bootstrap.getPlayerManager().getPlayer(target.getUniqueId());
                EnjinCoinPlayer targetMP = bootstrap.getPlayerManager().getPlayer(sender.getUniqueId());

                boolean result = bootstrap.getTradeManager().acceptInvite(senderMP, targetMP);

                if (!result) {
                    // TODO: No open invite or player is already in a trade
                }
            }
        }
    }

    private void inviteDecline(Player sender, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);

            if (target != null) {
                EnjinCoinPlayer senderMP = bootstrap.getPlayerManager().getPlayer(target.getUniqueId());
                EnjinCoinPlayer targetMP = bootstrap.getPlayerManager().getPlayer(sender.getUniqueId());

                boolean result = bootstrap.getTradeManager().declineInvite(senderMP, targetMP);

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
                    MessageUtils.sendComponent(sender, inviteTargetText);

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
