package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.client.model.body.GraphQLResponse;
import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.enjin.enjincoin.spigot_framework.util.UuidUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import retrofit2.Response;

import java.io.IOException;
import java.util.UUID;

/**
 * <p>Link command handler.</p>
 */
public class UnlinkCommand {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin plugin;

    /**
     * <p>Empty line helper for MessageUtils.sendMessage</p>
     */
    private final TextComponent newline = TextComponent.of("");

    /**
     * <p>Link command handler constructor.</p>
     *
     * @param plugin the Spigot plugin
     */
    public UnlinkCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * <p>Executes and performs operations defined for the command.</p>
     *
     * @param sender the command sender
     * @param args   the command arguments
     *
     * @since 1.0
     */
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
            MinecraftPlayer minecraftPlayer = this.plugin.getBootstrap().getPlayerManager().getPlayer(uuid);
            if (minecraftPlayer != null) {
                if (minecraftPlayer.isLoaded()) {
                    if (minecraftPlayer.getIdentityData().getEthereumAddress() == null || minecraftPlayer.getIdentityData().getEthereumAddress().isEmpty()) {
                        if (minecraftPlayer.getIdentity().getLinkingCode() != null)
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> handleUnlinked(sender, minecraftPlayer.getIdentity().getLinkingCode()));
                    } else {
                        // reload the identity for the existing user post unlink.
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                handleUnlinking(sender, minecraftPlayer.getIdentityData().getId());
                                minecraftPlayer.reloadUser();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } else {
                    // TODO: Warn sender that the online player has not fully loaded
                    System.out.println("player not fully loaded..");
                }
            } else {
                // TODO: Fetch Identity for the provided UUID
                System.out.println("need to fetch identity via uuid...");
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

    /**
     * method should just provide user feedback that the player's identity is already unlinked then provide
     * the linking code for them to use to link a wallet.
     *
     * @param sender
     * @param address
     */
    private void handleUnlinked(CommandSender sender, String address) {
        TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
        MessageUtils.sendMessage(sender, text);
        text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
        MessageUtils.sendMessage(sender, text);
    }

    /**
     * method should execute the unlink action for the given identity
     *
     * @param sender
     * @param id
     */
    private void handleUnlinking(CommandSender sender, Integer id) throws IOException {
        System.out.println("unlinking for id " + id);

        Response<GraphQLResponse<Identity>> response = this.plugin.getBootstrap().getSdkController().getClient().getIdentitiesService().unlinkIdentitySync(id, true);

        String code = "";
        if (response.isSuccessful()) {
            if (response.body().getData() != null) {
                code = response.body().getData().getLinkingCode();
            } else {
                System.out.println("Error unlinking: " + response.errorBody().string());
            }
        }

        final TextComponent notice = TextComponent.of("Wallet successfully unlinked. To re-link use the /enj link command to generate a new Linking Code.")
                .color(TextColor.GOLD);

        MessageUtils.sendMessage(sender, newline);
        MessageUtils.sendMessage(sender, notice);
    }

}
