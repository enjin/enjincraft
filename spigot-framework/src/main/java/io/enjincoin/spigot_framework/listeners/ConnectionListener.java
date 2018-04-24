package io.enjincoin.spigot_framework.listeners;

import com.enjin.java_commons.ExceptionUtils;
import io.enjincoin.sdk.client.Client;
import io.enjincoin.sdk.client.service.identities.IdentitiesService;
import io.enjincoin.sdk.client.service.identities.vo.Identity;
import io.enjincoin.sdk.client.service.identities.vo.IdentityField;
import io.enjincoin.spigot_framework.BasePlugin;
import io.enjincoin.spigot_framework.util.MessageUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConnectionListener implements Listener {

    private BasePlugin main;

    public ConnectionListener(BasePlugin main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (main.getBootstrap().getSdkController() == null)
            return;

        final Player player = event.getPlayer();
        final Client client = this.main.getBootstrap().getSdkController().getClient();
        final IdentitiesService service = client.getIdentitiesService();

        service.getIdentitiesAsync(new HashMap<String, Object>() {{
            put("uuid", player.getUniqueId().toString());
        }}, new Callback<Identity[]>() {
            @Override
            public void onResponse(Call<Identity[]> call, Response<Identity[]> response) {
                if (response.isSuccessful()) {
                    Identity[] identities = response.body();
                    if (identities.length == 0) {
                        linkCommandNotification(player);
                    } else {
                        for (Identity identity : identities) {
                            String rawUuid = null;

                            // Search for uuid field.
                            for (IdentityField field : identity.getFields()) {
                                if (field.getKey().equalsIgnoreCase("uuid")) {
                                    // Set raw uuid to discovered field's value.
                                    rawUuid = field.getFieldValue();
                                    break;
                                }
                            }

                            // Check if identity has matching uuid.
                            if (rawUuid != null && !player.getUniqueId().toString().equalsIgnoreCase(rawUuid))
                                continue;

                            // Check if the app associated with this identity matches the configured app.
                            String appId = main.getBootstrap().getConfig().get("appId").getAsString();
                            if (identity.getApp() != null && !identity.getApp().getId().equals(appId))
                                continue;

                            if (identity.getLinkingCode() != null && !identity.getLinkingCode().isEmpty()) {
                                linkCommandNotification(player);
                            } else {
                                // Add valid identity to identities cache.
                                main.getBootstrap().getIdentities().put(player.getUniqueId(), identity);
                            }

                            break;
                        }
                    }
                } else {
                    main.getLogger().warning("Unable to successfully fetch identities for " + player.getName());
                }
            }

            @Override
            public void onFailure(Call<Identity[]> call, Throwable t) {
                TextComponent text = TextComponent.of("An error occurred while getting an identity for ")
                        .append(TextComponent.of(player.getName()))
                        .append(TextComponent.of(":\n"))
                        .append(TextComponent.of(ExceptionUtils.throwableToString(t))
                                .color(TextColor.RED));
                MessageUtil.sendMessage(Bukkit.getConsoleSender(), text);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (main.getBootstrap().getSdkController() == null)
            return;

        final Player player = event.getPlayer();
        this.main.getBootstrap().getIdentities().remove(player.getUniqueId());
    }

    private void linkCommandNotification(Player player) {
        List<TextComponent> components = new ArrayList<TextComponent>(){{
            add(TextComponent.of("You have not linked an Enjin Coin wallet.").color(TextColor.GRAY));
            add(TextComponent.of("To link, type '").color(TextColor.GRAY)
                    .append(TextComponent.of("/enj link").color(TextColor.LIGHT_PURPLE))
                    .append(TextComponent.of("'.").color(TextColor.GRAY)));
        }};
        MessageUtil.sendMessages(player, components);
    }

}
