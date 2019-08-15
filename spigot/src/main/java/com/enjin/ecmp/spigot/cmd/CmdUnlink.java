package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.TokenUtils;
import com.enjin.enjincoin.sdk.model.service.identities.DeleteIdentity;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public class CmdUnlink extends EnjCommand {

    public CmdUnlink(SpigotBootstrap bootstrap) {
        super(bootstrap);
        setAllowedSenderTypes(SenderType.PLAYER);
        this.aliases.add("unlink");
    }

    @Override
    public void execute(CommandContext context) {
        EnjPlayer enjPlayer = context.enjPlayer;

        if (enjPlayer == null || !enjPlayer.isLoaded()) return;

        if (enjPlayer.isLinked()) {
            Bukkit.getScheduler().runTaskAsynchronously(bootstrap.plugin(), () -> {
                try {
                    handleUnlinking(context.sender, enjPlayer.getIdentityId());
                    enjPlayer.reloadIdentity();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else {
            handleUnlinked(context.sender, enjPlayer.getEthereumAddress());
        }
    }

    private void handleUnlinked(CommandSender sender, String address) {
        TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
        MessageUtils.sendComponent(sender, text);
        text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
        MessageUtils.sendComponent(sender, text);
    }

    private void handleUnlinking(CommandSender sender, int id) throws IOException {
        bootstrap.getTrustedPlatformClient().getIdentitiesService()
                .deleteIdentitySync(DeleteIdentity.unlink(id));

        final TextComponent notice = TextComponent.of("Wallet successfully unlinked. To re-link use the /enj link command to generate a new Linking Code.")
                .color(TextColor.GOLD);

        MessageUtils.sendComponent(sender, notice);

        Bukkit.getScheduler().runTask(bootstrap.plugin(), () -> {
            Player player = (Player) sender;
            Inventory inventory = player.getInventory();

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack is = inventory.getItem(i);
                if (is != null && TokenUtils.getTokenID(is) != null) {
                    inventory.setItem(i, null);
                }
            }
        });
    }

}
