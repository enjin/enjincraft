package com.enjin.enjincraft.spigot.cmd.player;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

public class CmdUnlink extends EnjCommand {

    public CmdUnlink(EnjCommand parent) {
        super(parent);
        this.aliases.add("unlink");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_UNLINK)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        EnjPlayer senderEnjPlayer = getValidSenderEnjPlayer(context);
        if (senderEnjPlayer == null)
            return;

        Bukkit.getScheduler().runTaskAsynchronously(bootstrap.plugin(), () -> {
            try {
                senderEnjPlayer.unlink();
            } catch (Exception ex) {
                bootstrap.log(ex);
                Translation.ERRORS_EXCEPTION.send(context.sender(), ex.getMessage());
            }
        });
    }

    @Override
    protected EnjPlayer getValidSenderEnjPlayer(CommandContext context) {
        Player sender = Objects.requireNonNull(context.player(), "Expected context to have non-null player as sender");

        EnjPlayer senderEnjPlayer = context.enjinPlayer();
        if (senderEnjPlayer == null) {
            Translation.ERRORS_PLAYERNOTREGISTERED.send(sender, sender.getName());
            return null;
        } else if (!senderEnjPlayer.isLoaded()) {
            Translation.IDENTITY_NOTLOADED.send(sender);
            return null;
        } else if (!senderEnjPlayer.isLinked()) {
            Translation.WALLET_NOTLINKED_SELF.send(sender);
            return null;
        }

        return senderEnjPlayer;
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_UNLINK_DESCRIPTION;
    }

}
