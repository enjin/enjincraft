package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.i18n.Translation;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.TokenUtils;
import com.enjin.enjincoin.sdk.model.service.identities.DeleteIdentity;
import com.enjin.java_commons.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public class CmdUnlink extends EnjCommand {

    public CmdUnlink(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("unlink");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_UNLINK)
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
            Bukkit.getScheduler().runTaskAsynchronously(bootstrap.plugin(), () -> {
                try {
                    unlink(context.sender, enjPlayer.getIdentityId());
                    enjPlayer.reloadIdentity();
                } catch (Exception ex) {
                    Messages.error(sender, ex);
                }
            });
        } else {
            Messages.identityNotLinked(sender);
        }
    }

    private void unlink(CommandSender sender, int id) throws IOException {
        bootstrap.getTrustedPlatformClient().getIdentitiesService()
                .deleteIdentitySync(DeleteIdentity.unlink(id));

        MessageUtils.sendString(sender, "&aThe wallet has been unlinked from your account.");
        Messages.linkInstructions(sender);

        Bukkit.getScheduler().runTask(bootstrap.plugin(), () -> {
            Player player = (Player) sender;
            Inventory inventory = player.getInventory();

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack is = inventory.getItem(i);
                if (is == null || is.getType() == Material.AIR) continue;
                String tokenId = TokenUtils.getTokenID(is);
                if (!StringUtils.isEmpty(tokenId)) inventory.setItem(i, null);
            }
        });
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_UNLINK_DESCRIPTION;
    }

}
