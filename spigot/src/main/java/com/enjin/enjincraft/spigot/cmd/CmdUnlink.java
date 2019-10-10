package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.sdk.model.service.identities.DeleteIdentity;
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
            Translation.IDENTITY_NOTLOADED.send(sender);
            return;
        }

        if (!enjPlayer.isLinked()) {
            Translation.WALLET_NOTLINKED_SELF.send(sender);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(bootstrap.plugin(), () -> {
            try {
                unlink(context.sender, enjPlayer.getIdentityId());
                enjPlayer.reloadIdentity();
            } catch (Exception ex) {
                bootstrap.log(ex);
                Translation.ERRORS_EXCEPTION.send(sender, ex.getMessage());
            }
        });
    }

    private void unlink(CommandSender sender, int id) throws IOException {
        bootstrap.getTrustedPlatformClient().getIdentitiesService()
                .deleteIdentitySync(DeleteIdentity.unlink(id));

        Translation.COMMAND_UNLINK_SUCCESS.send(sender);
        Translation.HINT_LINK.send(sender);

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
