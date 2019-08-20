package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.cmd.arg.PlayerArgumentProcessor;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.i18n.Translation;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CmdTrade extends EnjCommand {

    protected CmdInvite cmdInvite;
    protected CmdAccept cmdAccept;
    protected CmdDecline cmdDecline;

    public CmdTrade(SpigotBootstrap bootstrap, CmdEnj parent) {
        super(bootstrap, parent);
        this.aliases.add("trade");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_TRADE)
                .build();
        this.addSubCommand(cmdInvite = new CmdInvite());
        this.addSubCommand(cmdAccept = new CmdAccept());
        this.addSubCommand(cmdDecline = new CmdDecline());
    }

    @Override
    public void execute(CommandContext context) {
        MessageUtils.sendString(context.sender, cmdInvite.getUsage());
        MessageUtils.sendString(context.sender, cmdAccept.getUsage());
        MessageUtils.sendString(context.sender, cmdDecline.getUsage());
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TRADE_DESCRIPTION;
    }

    public class CmdInvite extends EnjCommand {

        public CmdInvite() {
            super(CmdTrade.this.bootstrap, CmdTrade.this);
            this.aliases.add("invite");
            this.requiredArgs.add("player");
            this.requirements = new CommandRequirements.Builder()
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .withPermission(Permission.CMD_TRADE_INVITE)
                    .build();
        }

        @Override
        public List<String> tab(CommandContext context) {
            List<String> result = new ArrayList<>();

            if (context.args.size() ==  1) {
                result.addAll(PlayerArgumentProcessor.INSTANCE.tab());
            }

            return result;
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() == 0) return;

            EnjPlayer senderEnjPlayer = context.enjPlayer;

            if (!senderEnjPlayer.isLinked()) {
                Messages.identityNotLinked(context.sender);
                return;
            }

            Player sender = context.player;
            Optional<Player> target = context.argToPlayer(0);

            if (!target.isPresent() || !target.get().isOnline()) {
                MessageUtils.sendString(sender, String.format("&6%s &cis not online.", context.args.get(0)));
                return;
            }

            if (sender == target.get()) {
                MessageUtils.sendString(sender, "&cYou must specify a player other than yourself.");
                return;
            }

            EnjPlayer targetEnjPlayer = bootstrap.getPlayerManager().getPlayer(target.get()).orElse(null);

            if (!targetEnjPlayer.isLinked()) {
                MessageUtils.sendString(sender, String.format("&6%s &chas not linked a wallet and cannot trade.", targetEnjPlayer.getBukkitPlayer().getName()));
                MessageUtils.sendString(targetEnjPlayer.getBukkitPlayer(), String.format("&6%s &awants to trade with you.", sender.getName()));
                Messages.linkInstructions(targetEnjPlayer.getBukkitPlayer());
                return;
            }

            if (BigInteger.ZERO.equals(senderEnjPlayer.getEnjAllowance())) {
                Messages.allowanceNotSet(sender);
                return;
            }

            invite(senderEnjPlayer, targetEnjPlayer);
        }

        private void invite(EnjPlayer sender, EnjPlayer target) {
            boolean result = bootstrap.getTradeManager().addInvite(sender, target);

            if (!result) {
                MessageUtils.sendString(sender.getBukkitPlayer(),
                        String.format("You have already invited &6%s &cto trade.", target.getBukkitPlayer().getName()));
                return;
            }

            MessageUtils.sendString(sender.getBukkitPlayer(),
                    String.format("&aTrade invite sent to &6%s!", target.getBukkitPlayer().getName()));

            TextComponent.Builder inviteMessageBuilder = TextComponent.builder("")
                    .color(TextColor.GRAY)
                    .append(TextComponent.builder(sender.getBukkitPlayer().getName())
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
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TRADE_INVITE_DESCRIPTION;
        }

    }

    public class CmdAccept extends EnjCommand {

        public CmdAccept() {
            super(CmdTrade.this.bootstrap, CmdTrade.this);
            this.aliases.add("accept");
            this.requiredArgs.add("player");
            this.requirements = new CommandRequirements.Builder()
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .withPermission(Permission.CMD_TRADE_ACCEPT)
                    .build();
        }

        @Override
        public List<String> tab(CommandContext context) {
            List<String> result = new ArrayList<>();

            if (context.args.size() ==  1) {
                result.addAll(PlayerArgumentProcessor.INSTANCE.tab());
            }

            return result;
        }

        @Override
        public void execute(CommandContext context) {
            Player sender = context.player;
            Optional<Player> target = context.argToPlayer(0);

            if (!target.isPresent() || !target.get().isOnline()) {
                MessageUtils.sendString(sender, String.format("&6%s &cis not online.", context.args.get(0)));
                return;
            }

            if (sender == target.get()) {
                MessageUtils.sendString(sender, "&cYou must specify a player other than yourself.");
                return;
            }

            EnjPlayer targetEnjPlayer = bootstrap.getPlayerManager().getPlayer(target.get()).orElse(null);
            EnjPlayer senderEnjPlayer = context.enjPlayer;

            boolean result = bootstrap.getTradeManager().acceptInvite(targetEnjPlayer, senderEnjPlayer);
            if (!result) {
                MessageUtils.sendString(sender, String.format("&cNo open trade invites from &6%s.", senderEnjPlayer.getBukkitPlayer().getName()));
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TRADE_ACCEPT_DESCRIPTION;
        }

    }

    public class CmdDecline extends EnjCommand {

        public CmdDecline() {
            super(CmdTrade.this.bootstrap, CmdTrade.this);
            this.aliases.add("decline");
            this.requiredArgs.add("player");
            this.requirements = new CommandRequirements.Builder()
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .withPermission(Permission.CMD_TRADE_DECLINE)
                    .build();
        }

        @Override
        public List<String> tab(CommandContext context) {
            List<String> result = new ArrayList<>();

            if (context.args.size() ==  1) {
                result.addAll(PlayerArgumentProcessor.INSTANCE.tab());
            }

            return result;
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() == 0) return;

            Player sender = context.player;
            Optional<Player> target = context.argToPlayer(0);

            if (!target.isPresent() || !target.get().isOnline()) {
                MessageUtils.sendString(sender, String.format("&6%s &cis not online.", context.args.get(0)));
                return;
            }

            if (sender == target.get()) {
                MessageUtils.sendString(sender, "&cYou must specify a player other than yourself.");
                return;
            }

            EnjPlayer targetEnjPlayer = bootstrap.getPlayerManager().getPlayer(target.get()).orElse(null);
            EnjPlayer senderEnjPlayer = context.enjPlayer;

            boolean result = bootstrap.getTradeManager().declineInvite(targetEnjPlayer, senderEnjPlayer);
            if (result) {
                MessageUtils.sendString(sender, String.format("&aYou have declined &6%s's &atrade invite.",
                        senderEnjPlayer.getBukkitPlayer().getName()));
                MessageUtils.sendString(targetEnjPlayer.getBukkitPlayer(), String.format("&6%s &chas declined your trade invite.",
                        targetEnjPlayer.getBukkitPlayer().getName()));
            } else {
                MessageUtils.sendString(sender, String.format("&cNo open trade invites from &6%s.",
                        senderEnjPlayer.getBukkitPlayer().getName()));
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TRADE_DECLINE_DESCRIPTION;
        }

    }

}
