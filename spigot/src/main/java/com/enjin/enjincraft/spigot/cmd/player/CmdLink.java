package com.enjin.enjincraft.spigot.cmd.player;

import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.StringUtils;
import lombok.NonNull;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class CmdLink extends EnjCommand {

    public CmdLink(EnjCommand parent) {
        super(parent);
        this.aliases.add("link");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_LINK)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        EnjPlayer senderEnjPlayer = getValidSenderEnjPlayer(context);
        if (senderEnjPlayer != null && senderEnjPlayer.isLinked())
            existingLink(context.sender(), senderEnjPlayer.getEthereumAddress());
        else if (senderEnjPlayer != null)
            linkInstructions(context.sender(), senderEnjPlayer.getLinkingCode());
    }

    @Override
    protected EnjPlayer getValidSenderEnjPlayer(@NonNull CommandContext context) throws NullPointerException {
        Player sender = Objects.requireNonNull(context.player(), "Expected context to have non-null player as sender");

        EnjPlayer senderEnjPlayer = context.enjinPlayer();
        if (senderEnjPlayer == null) {
            Translation.ERRORS_PLAYERNOTREGISTERED.send(sender, sender.getName());
            return null;
        } else if (!senderEnjPlayer.isLoaded()) {
            Translation.IDENTITY_NOTLOADED.send(sender);
            return null;
        }

        return senderEnjPlayer;
    }

    private void existingLink(CommandSender sender, String address) {
        if (StringUtils.isEmpty(address))
            Translation.COMMAND_LINK_NULLWALLET.send(sender);
        else
            Translation.COMMAND_LINK_SHOWWALLET.send(sender, address);
    }

    private void linkInstructions(CommandSender sender, String code) {
        if (StringUtils.isEmpty(code)) {
            Translation.COMMAND_LINK_NULLCODE.send(sender);
        } else {
            Translation.COMMAND_LINK_INSTRUCTIONS_1.send(sender);
            Translation.COMMAND_LINK_INSTRUCTIONS_2.send(sender);
            Translation.COMMAND_LINK_INSTRUCTIONS_3.send(sender);
            Translation.COMMAND_LINK_INSTRUCTIONS_4.send(sender);
            Translation.COMMAND_LINK_INSTRUCTIONS_5.send(sender);
            Translation.COMMAND_LINK_INSTRUCTIONS_6.send(sender);
            Translation.COMMAND_LINK_INSTRUCTIONS_7.send(sender, code);
        }
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_LINK_DESCRIPTION;
    }

}
