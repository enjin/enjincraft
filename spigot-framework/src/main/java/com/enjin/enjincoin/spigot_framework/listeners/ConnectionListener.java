package com.enjin.enjincoin.spigot_framework.listeners;

import com.enjin.enjincoin.sdk.client.model.body.GraphQLResponse;
import com.enjin.enjincoin.sdk.client.service.users.vo.User;
import com.enjin.enjincoin.sdk.client.service.users.vo.data.UsersData;
import com.enjin.java_commons.ExceptionUtils;
import com.enjin.enjincoin.sdk.client.Client;
import com.enjin.enjincoin.sdk.client.service.identities.IdentitiesService;
import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.sdk.client.service.identities.vo.IdentityField;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
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

/**
 * <p>A listener for handling events in which a connection to
 * the server is created or terminated.</p>
 *
 * @since 1.0
 */
public class ConnectionListener implements Listener {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    /**
     * <p>Listener constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public ConnectionListener(BasePlugin main) {
        this.main = main;
    }

    /**
     * <p>Subscription to {@link PlayerJoinEvent} with the
     * normal priority.</p>
     *
     * @param event the event
     *
     * @since 1.0
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (main.getBootstrap().getSdkController() == null)
            return;

        final Player player = event.getPlayer();
        final Client client = this.main.getBootstrap().getSdkController().getClient();
        final IdentitiesService service = client.getIdentitiesService();



        // TODO: Integrate post integration logic into new MinecraftPlayer system
        // E.g. send linking notification to a player after loading their Identity
        // This class is ultimately being replaced by the classes in the player package

        Callback<GraphQLResponse<UsersData>> callback = new Callback<GraphQLResponse<UsersData>>() {
            @Override
            public void onResponse(Call<GraphQLResponse<UsersData>> call, Response<GraphQLResponse<UsersData>> response) {
                if (response.isSuccessful()) {
                    GraphQLResponse<UsersData> usersData = response.body();
                    List<Identity> identities = new ArrayList<>();
                    // get all identities from query matched user data
                    for (User user : usersData.getData().getUsers()) {
                        identities.addAll(usersData.getData().getUsers().get(0).getIdentities());
                    }

                    if (identities.size() == 0) {
                        linkCommandNotification(player);
                    } else {
                        for (Identity identity : identities) {
                            String rawUuid = null;

                            for (IdentityField field : identity.getFields()) {
                                if (field.getKey().equalsIgnoreCase("uuid")) {
                                    // Set raw uuid to discovered field's value
                                    rawUuid = field.getFieldValue();
                                    break;
                                }
                            }

                            // check if identity has mathcing uuid.
                            if (rawUuid != null && player.getUniqueId().toString().equalsIgnoreCase(rawUuid))
                                continue;

                            // Check if the app associated with this identity matches the configured app.
                            String appId = main.getBootstrap().getConfig().get("appId").getAsString();
                            if (identity.getAppId() != null && !identity.getAppId().toString().equals(appId))
                                continue;

                            if (identity.getLinkingCode() != null && !identity.getLinkingCode().isEmpty()) {
                                linkCommandNotification(player);
                            } else {
                                // Add valid identity to identities cache.
//                                main.getBootstrap().getIdentities().put(player.getUniqueId(), identity);
                            }

                            break;
                        }
                    }
                } else {
                    main.getLogger().warning("Unable to successfully fetch identities for " + player.getName());
                }
            }

            @Override
            public void onFailure(Call<GraphQLResponse<UsersData>> call, Throwable t) {
                TextComponent text = TextComponent.of("An error occurred while getting an identity for ")
                        .append(TextComponent.of(player.getName()))
                        .append(TextComponent.of(":\n"))
                        .append(TextComponent.of(ExceptionUtils.throwableToString(t))
                                .color(TextColor.RED));
                MessageUtils.sendMessage(Bukkit.getConsoleSender(), text);
            }
        };
    }

    /**
     * <p>Send a player a notification that they have yet to link their
     * identity to an Enjin wallet along with instructions how to start
     * the process.</p>
     *
     * @param player the player
     *
     * @since 1.0
     */
    private void linkCommandNotification(Player player) {
        List<TextComponent> components = new ArrayList<TextComponent>(){{
            add(TextComponent.of("You have not linked an Enjin Coin wallet.").color(TextColor.GRAY));
            add(TextComponent.of("To link, type '").color(TextColor.GRAY)
                    .append(TextComponent.of("/enj link").color(TextColor.LIGHT_PURPLE))
                    .append(TextComponent.of("'.").color(TextColor.GRAY)));
        }};
        MessageUtils.sendMessages(player, components);
    }

}
