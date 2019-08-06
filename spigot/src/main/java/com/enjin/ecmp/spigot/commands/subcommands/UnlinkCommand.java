package com.enjin.ecmp.spigot.commands.subcommands;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.ECPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.TokenUtils;
import com.enjin.ecmp.spigot.util.UuidUtils;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.identities.DeleteIdentity;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.service.identities.IdentitiesService;
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

    private SpigotBootstrap bootstrap;

    private final TextComponent newline = TextComponent.of("");

    public UnlinkCommand(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
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
                MessageUtils.sendComponent(sender, text);
            }
        }

        if (uuid != null) {
            ECPlayer enjinCoinPlayer = bootstrap.getPlayerManager().getPlayer(uuid);
            if (enjinCoinPlayer != null) {
                if (enjinCoinPlayer.isLoaded()) {
                    if (!enjinCoinPlayer.isLinked()) {
                        if (enjinCoinPlayer.getLinkingCode() != null)
                            Bukkit.getScheduler().runTask(bootstrap.plugin(), () -> handleUnlinked(sender, enjinCoinPlayer.getLinkingCode()));
                    } else {
                        // reload the identity for the existing user post unlink.
                        Bukkit.getScheduler().runTaskAsynchronously(bootstrap.plugin(), () -> {
                            try {
                                handleUnlinking(sender, enjinCoinPlayer.getIdentityId());
                                enjinCoinPlayer.reloadIdentity();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
        } else {
            errorInvalidUuid(sender);
        }
    }

    private void errorInvalidUuid(CommandSender sender) {
        final TextComponent text = TextComponent.of("The UUID provided is invalid.")
                .color(TextColor.RED);

        MessageUtils.sendComponent(sender, newline);
        MessageUtils.sendComponent(sender, text);
    }

    private void handleUnlinked(CommandSender sender, String address) {
        TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
        MessageUtils.sendComponent(sender, text);
        text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
        MessageUtils.sendComponent(sender, text);
    }

    private void handleUnlinking(CommandSender sender, int id) throws IOException {
        IdentitiesService service = bootstrap.getTrustedPlatformClient().getIdentitiesService();
        HttpResponse<GraphQLResponse<Identity>> response = service.deleteIdentitySync(DeleteIdentity.unlink(id));

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
