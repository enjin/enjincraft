package io.enjincoin.spigot_framework.commands.subcommands;

import io.enjincoin.sdk.client.Client;
import io.enjincoin.sdk.client.service.identities.IdentitiesService;
import io.enjincoin.sdk.client.vo.identity.CreateIdentityResponseVO;
import io.enjincoin.sdk.client.vo.identity.ImmutableCreateIdentityRequestVO;
import io.enjincoin.sdk.client.vo.identity.ImmutableGetIdentityRequestVO;
import io.enjincoin.spigot_framework.BasePlugin;
import io.enjincoin.spigot_framework.Bootstrap;
import io.enjincoin.spigot_framework.controllers.SdkClientController;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class LinkCommand {

    private BasePlugin main;

    public LinkCommand(BasePlugin main) {
        this.main = main;
    }

    public void execute(CommandSender sender, String[] args) {
        UUID uuid = null;

        if (sender instanceof Player) {
            Player player = (Player) sender;
            uuid = player.getUniqueId();
        } else {
            if (args.length >= 1) {
                try {
                    uuid = UUID.fromString(args[0]);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + e.getMessage());
                }
            } else {
                sender.sendMessage(ChatColor.RED + "UUID argument missing.");
            }
        }

        if (uuid != null) {
            linkIdentity(sender, uuid);
        }
    }

    private void linkIdentity(CommandSender sender, UUID uuid) {
        Bootstrap bootstrap = this.main.getBootstrap();
        SdkClientController controller = bootstrap.getSdkController();
        Client client = controller.getClient();
        IdentitiesService service = client.getIdentitiesService();

        service.getIdentitiesAsync(ImmutableGetIdentityRequestVO.builder()
                .setIdentityMap(new HashMap<String, Object>() {{
                    put("uuid", uuid);
                }})
                .build())
                .whenComplete((array, ex) -> {
                    if (ex == null) {
                        if (array == null || array.length == 0) {
                            service.createIdentityAsync(ImmutableCreateIdentityRequestVO.builder()
                                    .setAuth("xxxxxxxx")
                                    .setIdentityMap(new HashMap<String, Object>() {{
                                        put("uuid", uuid);
                                    }}).build())
                                    .whenComplete((response, ex2) -> {
                                        if (ex2 == null) {
                                            handleCreateIdentityResponse(sender, response);
                                        } else {
                                            errorCreatingIdentity(sender, ex2);
                                        }
                                    });
                        }
                    } else {
                        errorRequestingIdentities(sender, ex);
                    }
                });
    }

    private void errorRequestingIdentities(CommandSender sender, Throwable e) {
        this.main.getLogger().log(Level.WARNING, e.getMessage(), e);
        sender.sendMessage(ChatColor.RED + "Error occurred while requesting identities. Please try again later.");
    }

    private void errorCreatingIdentity(CommandSender sender, Throwable e) {
        this.main.getLogger().log(Level.WARNING, e.getMessage(), e);
        sender.sendMessage(ChatColor.RED + "Error occurred while creating an identity. Please try again later.");
    }

    private void handleCreateIdentityResponse(CommandSender sender, CreateIdentityResponseVO response) {
        if (sender instanceof Player && !((Player) sender).isOnline())
            return;

        Optional<String> optional = response.getIdentityCode();
        if (optional.isPresent()) {
            sender.sendMessage(ChatColor.GREEN + "Identity Code: " + ChatColor.GOLD + optional.get());
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to request identity code: " + ChatColor.GOLD + "code not present.");
        }
    }

}
