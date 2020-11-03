package com.enjin.enjincraft.spigot.cmd.wallet.trade;

import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.cmd.arg.PlayerArgumentProcessor;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.MessageUtils;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class CmdInvite extends EnjCommand {

    public CmdInvite(CmdTrade cmdTrade) {
        super(cmdTrade.bootstrap(), cmdTrade);
        this.aliases.add("invite");
        this.requiredArgs.add(CmdTrade.PLAYER_ARG);
        this.requirements = new CommandRequirements.Builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_TRADE_INVITE)
                .build();
    }

    @Override
    public List<String> tab(CommandContext context) {
        if (context.args().size() == 1)
            return PlayerArgumentProcessor.INSTANCE.tab(context.sender(), context.args().get(0));
        return new ArrayList<>(0);
    }

    @Override
    public void execute(CommandContext context) {
        String target = context.args().get(0);

        EnjPlayer senderEnjPlayer = getValidSenderEnjPlayer(context);
        if (senderEnjPlayer == null)
            return;

        Player targetPlayer = getValidTargetPlayer(context, target);
        if (targetPlayer == null)
            return;

        EnjPlayer targetEnjPlayer = getValidTargetEnjPlayer(context, targetPlayer);
        if (targetEnjPlayer == null)
            return;

        if (BigInteger.ZERO.equals(senderEnjPlayer.getEnjAllowance())) {
            Translation.WALLET_ALLOWANCENOTSET.send(context.sender());
            return;
        }

        invite(senderEnjPlayer, targetEnjPlayer);
    }

    @Override
    protected EnjPlayer getValidTargetEnjPlayer(CommandContext context,
                                                @NonNull Player targetPlayer) throws NullPointerException {
        CommandSender sender = context.sender();

        EnjPlayer targetEnjPlayer = bootstrap.getPlayerManager()
                .getPlayer(targetPlayer)
                .orElse(null);
        if (targetEnjPlayer == null) {
            Translation.ERRORS_PLAYERNOTREGISTERED.send(sender, targetPlayer.getName());
            return null;
        } else if (!targetEnjPlayer.isLinked()) {
            Translation.WALLET_NOTLINKED_OTHER.send(sender, targetPlayer.getName());
            Translation.COMMAND_TRADE_WANTSTOTRADE.send(targetPlayer, sender.getName());
            Translation.HINT_LINK.send(targetPlayer);
            return null;
        }

        return targetEnjPlayer;
    }

    private void invite(EnjPlayer sender, EnjPlayer target) {
        boolean result = bootstrap.getTradeManager().addInvite(sender, target);
        if (!result) {
            Translation.COMMAND_TRADE_ALREADYINVITED.send(sender.getBukkitPlayer(), target.getBukkitPlayer().getName());
            return;
        }

        Translation.COMMAND_TRADE_INVITESENT.send(sender.getBukkitPlayer(), target.getBukkitPlayer().getName());
        Translation.COMMAND_TRADE_INVITEDTOTRADE.send(target.getBukkitPlayer(), sender.getBukkitPlayer().getName());
        TextComponent.Builder inviteMessageBuilder = Component.text()
                .append(Component.text("Accept")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand(String.format("/enj trade accept %s",
                                sender.getBukkitPlayer().getName()))))
                .append(Component.text(" | ").color(NamedTextColor.GRAY))
                .append(Component.text("Decline")
                        .color(NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand(String.format("/enj trade decline %s",
                                sender.getBukkitPlayer().getName()))));
        MessageUtils.sendComponent(target.getBukkitPlayer(), inviteMessageBuilder.build());
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TRADE_INVITE_DESCRIPTION;
    }

}
