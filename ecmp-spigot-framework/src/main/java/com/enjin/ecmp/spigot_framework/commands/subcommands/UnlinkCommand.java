package com.enjin.ecmp.spigot_framework.commands.subcommands;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.identities.DeleteIdentity;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.service.identities.IdentitiesService;
import com.enjin.ecmp.spigot_framework.player.EnjinCoinPlayer;
import com.enjin.ecmp.spigot_framework.util.MessageUtils;
import com.enjin.ecmp.spigot_framework.util.TokenUtils;
import com.enjin.ecmp.spigot_framework.util.UuidUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.UUID;

public class UnlinkCommand {

    private BasePlugin plugin;

    private final TextComponent newline = TextComponent.of("");

    public UnlinkCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender, String[] args) {
        UUID uuid = null;

        if (sender instanceof Player) {
            Player player = (Player) sender;
            uuid = player.getUniqueId();
        } else {
            if (args.length >= 1) {
                try {
                    uuid = UuidUtils.stringToUuid(args[0]);
                } catch (IllegalArgumentException e) {
                    errorInvalidUuid(sender);
                }
            } else {
                final TextComponent text = TextComponent.of("UUID argument required.")
                        .color(TextColor.RED);
                MessageUtils.sendMessage(sender, text);
            }
        }

        if (uuid != null) {
            EnjinCoinPlayer enjinCoinPlayer = this.plugin.getBootstrap().getPlayerManager().getPlayer(uuid);
            if (enjinCoinPlayer != null) {
                if (enjinCoinPlayer.isLoaded()) {
                    if (!enjinCoinPlayer.isLinked()) {
                        if (enjinCoinPlayer.getLinkingCode() != null)
                            Bukkit.getScheduler().runTask(plugin, () -> handleUnlinked(sender, enjinCoinPlayer.getLinkingCode()));
                    } else {
                        // reload the identity for the existing user post unlink.
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                handleUnlinking(sender, enjinCoinPlayer.getIdentityId());
                                enjinCoinPlayer.reloadUser();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } else {
                    // TODO: Warn sender that the online player has not fully loaded
                }
            } else {
                // TODO: Fetch Identity for the provided UUID
                // Only fetch, do not create new Identity instances
            }
        } else {
            errorInvalidUuid(sender);
        }
    }

    private void errorInvalidUuid(CommandSender sender) {
        final TextComponent text = TextComponent.of("The UUID provided is invalid.")
                .color(TextColor.RED);

        MessageUtils.sendMessage(sender, newline);
        MessageUtils.sendMessage(sender, text);
    }

    private void handleUnlinked(CommandSender sender, String address) {
        TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
        MessageUtils.sendMessage(sender, text);
        text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
        MessageUtils.sendMessage(sender, text);
    }

    private void handleUnlinking(CommandSender sender, int id) throws IOException {
        IdentitiesService service = this.plugin.getBootstrap().getTrustedPlatformClient().getIdentitiesService();
        HttpResponse<GraphQLResponse<Identity>> response = service.deleteIdentitySync(DeleteIdentity.unlink(id));

        final TextComponent notice = TextComponent.of("Wallet successfully unlinked. To re-link use the /enj link command to generate a new Linking Code.")
                .color(TextColor.GOLD);

        MessageUtils.sendMessage(sender, notice);

        Bukkit.getScheduler().runTask(plugin, () -> {
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
