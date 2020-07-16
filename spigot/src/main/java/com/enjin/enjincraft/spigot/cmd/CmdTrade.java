package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.arg.PlayerArgumentProcessor;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CmdTrade extends EnjCommand {

    private static final String PLAYER_ARG = "player";

    public CmdTrade(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("trade");
        this.requiredArgs.add("action");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_TRADE)
                .build();
        this.addSubCommand(new CmdInvite());
        this.addSubCommand(new CmdAccept());
        this.addSubCommand(new CmdDecline());
    }

    @Override
    public void execute(CommandContext context) {
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TRADE_DESCRIPTION;
    }

    public class CmdInvite extends EnjCommand {

        public CmdInvite() {
            super(CmdTrade.this.bootstrap, CmdTrade.this);
            this.aliases.add("invite");
            this.requiredArgs.add(PLAYER_ARG);
            this.requirements = new CommandRequirements.Builder()
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .withPermission(Permission.CMD_TRADE_INVITE)
                    .build();
        }

        @Override
        public List<String> tab(CommandContext context) {
            if (context.args.size() == 1)
                return PlayerArgumentProcessor.INSTANCE.tab(context.sender, context.args.get(0));
            return new ArrayList<>(0);
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.isEmpty())
                return;

            EnjPlayer senderEnjPlayer = context.enjPlayer;

            if (!senderEnjPlayer.isLinked()) {
                Translation.WALLET_NOTLINKED_SELF.send(context.sender);
                return;
            }

            Player sender = context.player;
            Optional<Player> optionalTarget = context.argToPlayer(0);

            if (!optionalTarget.isPresent() || !optionalTarget.get().isOnline()) {
                Translation.ERRORS_PLAYERNOTONLINE.send(context.sender, context.args.get(0));
                return;
            }

            Player target = optionalTarget.get();

            if (sender == target) {
                Translation.ERRORS_CHOOSEOTHERPLAYER.send(sender);
                return;
            }

            Optional<EnjPlayer> optionalEnjTarget = bootstrap.getPlayerManager().getPlayer(target);
            if (!optionalEnjTarget.isPresent())
                return;
            EnjPlayer targetEnjPlayer = optionalEnjTarget.get();

            if (!targetEnjPlayer.isLinked()) {
                Translation.WALLET_NOTLINKED_OTHER.send(sender, target.getName());
                Translation.COMMAND_TRADE_WANTSTOTRADE.send(target, sender.getName());
                Translation.HINT_LINK.send(target);
                return;
            }

            if (BigInteger.ZERO.equals(senderEnjPlayer.getEnjAllowance())) {
                Translation.WALLET_ALLOWANCENOTSET.send(context.sender);
                return;
            }

            invite(senderEnjPlayer, targetEnjPlayer);
        }

        private void invite(EnjPlayer sender, EnjPlayer target) {
            boolean result = bootstrap.getTradeManager().addInvite(sender, target);

            if (!result) {
                Translation.COMMAND_TRADE_ALREADYINVITED.send(sender.getBukkitPlayer(), target.getBukkitPlayer().getName());
                return;
            }

            Translation.COMMAND_TRADE_INVITESENT.send(sender.getBukkitPlayer(), target.getBukkitPlayer().getName());

            Translation.COMMAND_TRADE_INVITEDTOTRADE.send(target.getBukkitPlayer(), sender.getBukkitPlayer().getName());
            TextComponent.Builder inviteMessageBuilder = TextComponent.builder("")
                    .append(TextComponent.builder("Accept")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/enj trade accept %s", sender.getBukkitPlayer().getName())))
                            .build())
                    .append(TextComponent.of(" | ").color(TextColor.GRAY))
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
            this.requiredArgs.add(PLAYER_ARG);
            this.requirements = new CommandRequirements.Builder()
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .withPermission(Permission.CMD_TRADE_ACCEPT)
                    .build();
        }

        @Override
        public List<String> tab(CommandContext context) {
            if (context.args.size() == 1)
                return PlayerArgumentProcessor.INSTANCE.tab(context.sender, context.args.get(0));
            return new ArrayList<>(0);
        }

        @Override
        public void execute(CommandContext context) {
            Player sender = context.player;
            Optional<Player> target = context.argToPlayer(0);

            if (!target.isPresent() || !target.get().isOnline()) {
                Translation.ERRORS_PLAYERNOTONLINE.send(context.sender, context.args.get(0));
                return;
            }

            if (sender == target.get()) {
                Translation.ERRORS_CHOOSEOTHERPLAYER.send(sender);
                return;
            }

            EnjPlayer targetEnjPlayer = bootstrap.getPlayerManager().getPlayer(target.get()).orElse(null);
            EnjPlayer senderEnjPlayer = context.enjPlayer;

            boolean result = bootstrap.getTradeManager().acceptInvite(targetEnjPlayer, senderEnjPlayer);
            if (!result)
                Translation.COMMAND_TRADE_NOOPENINVITE.send(sender, target.get().getName());
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
            this.requiredArgs.add(PLAYER_ARG);
            this.requirements = new CommandRequirements.Builder()
                    .withAllowedSenderTypes(SenderType.PLAYER)
                    .withPermission(Permission.CMD_TRADE_DECLINE)
                    .build();
        }

        @Override
        public List<String> tab(CommandContext context) {
            if (context.args.size() == 1)
                return PlayerArgumentProcessor.INSTANCE.tab(context.sender, context.args.get(0));
            return new ArrayList<>(0);
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.isEmpty())
                return;

            Player sender = context.player;
            Optional<Player> target = context.argToPlayer(0);

            if (!target.isPresent() || !target.get().isOnline()) {
                Translation.ERRORS_PLAYERNOTONLINE.send(sender, context.args.get(0));
                return;
            }

            if (sender == target.get()) {
                Translation.ERRORS_CHOOSEOTHERPLAYER.send(sender);
                return;
            }

            EnjPlayer targetEnjPlayer = bootstrap.getPlayerManager().getPlayer(target.get()).orElse(null);
            EnjPlayer senderEnjPlayer = context.enjPlayer;

            boolean result = bootstrap.getTradeManager().declineInvite(targetEnjPlayer, senderEnjPlayer);
            if (result) {
                Translation.COMMAND_TRADE_DECLINED_SENDER.send(sender, target.get().getName());
                Translation.COMMAND_TRADE_DECLINED_TARGET.send(target.get(), sender.getName());
            } else {
                Translation.COMMAND_TRADE_NOOPENINVITE.send(sender, target.get().getName());
            }
        }

        @Override
        public Translation getUsageTranslation() {
            return Translation.COMMAND_TRADE_DECLINE_DESCRIPTION;
        }

    }

}
