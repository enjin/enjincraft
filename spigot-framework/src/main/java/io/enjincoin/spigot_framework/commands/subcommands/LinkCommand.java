package io.enjincoin.spigot_framework.commands.subcommands;

import io.enjincoin.sdk.client.Client;
import io.enjincoin.sdk.client.service.identities.IdentitiesService;
import io.enjincoin.sdk.client.vo.identity.CreateIdentityResponseVO;
import io.enjincoin.sdk.client.vo.identity.ImmutableCreateIdentityRequestVO;
import io.enjincoin.sdk.client.vo.identity.ImmutableGetIdentityRequestVO;
import io.enjincoin.spigot_framework.BasePlugin;
import io.enjincoin.spigot_framework.Bootstrap;
import io.enjincoin.spigot_framework.controllers.SdkClientController;
import io.enjincoin.spigot_framework.util.MessageUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.ComponentSerializers;
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
                final TextComponent text = TextComponent.of("UUID argument required.")
                        .color(TextColor.RED);
                MessageUtil.sendMessage(sender, text);
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
        final TextComponent text = TextComponent.of("An error occurred while requesting a player identity.")
                .color(TextColor.RED);
        this.main.getLogger().log(Level.WARNING, e.getMessage(), e);
        MessageUtil.sendMessage(sender, text);
    }

    private void errorCreatingIdentity(CommandSender sender, Throwable e) {
        final TextComponent text = TextComponent.of("An error occurred while creating a player identity.");
        this.main.getLogger().log(Level.WARNING, e.getMessage(), e);
        MessageUtil.sendMessage(sender, text);
    }

    private void handleCreateIdentityResponse(CommandSender sender, CreateIdentityResponseVO response) {
        if (sender instanceof Player && !((Player) sender).isOnline())
            return;

        Optional<String> optional = response.getIdentityCode();
        if (optional.isPresent()) {
            final TextComponent text = TextComponent.of("Identity Code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of(optional.get())
                            .color(TextColor.GOLD));
            MessageUtil.sendMessage(sender, text);
        } else {
            final TextComponent text = TextComponent.of("Could not acquire a player identity code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of("code not present.")
                            .color(TextColor.GOLD));
            MessageUtil.sendMessage(sender, text);
        }
    }

}
