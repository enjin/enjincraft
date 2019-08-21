package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.i18n.Translation;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.java_commons.StringUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
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
            Messages.identityNotLoaded(sender);
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
            MessageUtils.sendString(sender, "&cUnable to display your wallet address.");
        else
            MessageUtils.sendString(sender, String.format("&aYour account is linked to the wallet address: &6%s", address));
    }

    private void linkInstructions(CommandSender sender, String code) {
        if (StringUtils.isEmpty(code))
            MessageUtils.sendString(sender, "&cUnable to display your identity linking code.");
        else {
            MessageUtils.sendString(sender,"&6To link your account follow the steps below:");
            MessageUtils.sendString(sender, "&7Download the Enjin Wallet for Android or iOS");
            MessageUtils.sendString(sender, "&7Browse to the Linked Apps section");
            MessageUtils.sendString(sender, "&7Click the 'LINK APP' button");
            MessageUtils.sendString(sender, "&7Select the wallet you wish to link");
            MessageUtils.sendString(sender, "&7Enter the identity linking code shown below");
            MessageUtils.sendString(sender, String.format("&aIdentity Code: &6%s", code));
        }
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_LINK_DESCRIPTION;
    }

}
