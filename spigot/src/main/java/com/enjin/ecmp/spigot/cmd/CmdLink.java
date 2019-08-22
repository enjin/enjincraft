package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.i18n.Translation;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.java_commons.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdLink extends EnjCommand {

    public CmdLink(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("link");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_LINK)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        Player sender = context.player;
        EnjPlayer enjPlayer = context.enjPlayer;

        if (!enjPlayer.isLoaded()) {
            Translation.IDENTITY_NOTLOADED.send(sender);
            return;
        }

        if (enjPlayer.isLinked()) {
            existingLink(context.sender, enjPlayer.getEthereumAddress());
        } else {
            linkInstructions(context.sender, enjPlayer.getLinkingCode());
        }
    }

    private void existingLink(CommandSender sender, String address) {
        if (StringUtils.isEmpty(address))
            Translation.COMMAND_LINK_NULLWALLET.send(sender);
        else
            Translation.COMMAND_LINK_SHOWWALLET.send(sender, address);
    }

    private void linkInstructions(CommandSender sender, String code) {
        if (StringUtils.isEmpty(code))
            Translation.COMMAND_LINK_NULLCODE.send(sender);
        else {
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
