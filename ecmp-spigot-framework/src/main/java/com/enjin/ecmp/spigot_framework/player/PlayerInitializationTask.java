package com.enjin.ecmp.spigot_framework.player;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.identities.CreateIdentity;
import com.enjin.enjincoin.sdk.model.service.identities.GetIdentities;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.model.service.users.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInitializationTask extends BukkitRunnable {

    public static final Long TASK_DELAY = 1L;
    public static final Long TASK_PERIOD = 20L;

    private static final Map<UUID, PlayerInitializationTask> PLAYER_TASKS = new ConcurrentHashMap<>();

    private BasePlugin plugin;
    private EnjinCoinPlayer enjinCoinPlayer;

    private boolean inProgress = false;

    protected PlayerInitializationTask(BasePlugin plugin, EnjinCoinPlayer enjinCoinPlayer) {
        this.plugin = plugin;
        this.enjinCoinPlayer = enjinCoinPlayer;
    }

    @Override
    public void run() {
        if (this.inProgress || isCancelled()) {
            return;
        }

        this.inProgress = true;

        if (this.enjinCoinPlayer.getBukkitPlayer() == null || !this.enjinCoinPlayer.getBukkitPlayer().isOnline()) {
            cancel();
        } else {
            try {
                if (!this.enjinCoinPlayer.isUserLoaded()) {
                    User user = getUser(enjinCoinPlayer.getBukkitPlayer().getUniqueId());
                    if (user != null) {
                        // An existing user has been found or a new user has been created
                        this.enjinCoinPlayer.loadUser(user);
                    }
                }

                if (this.enjinCoinPlayer.isUserLoaded() && !this.enjinCoinPlayer.isIdentityLoaded()) {
                    Identity identity = getIdentity();
                    if (identity != null) {
                        // A new identity has been created
                        this.enjinCoinPlayer.loadIdentity(identity);
                        cancel();
                    }
                } else if (this.enjinCoinPlayer.isLoaded() && !isCancelled()) {
                    cancel();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        if (this.minecraftPlayer.getWallet() != null)
//            this.minecraftPlayer.getWallet().getCheckoutManager().populate(this.plugin, minecraftPlayer.getBukkitPlayer(), this.minecraftPlayer.getWallet());

        this.inProgress = false;
    }

    private User getUser(UUID playerUuid) throws IOException {
        User user = fetchExistingUser(playerUuid);

        if (user == null) {
            user = createUser(playerUuid);
        }

        return user;
    }

    private User fetchExistingUser(UUID playerUuid) throws IOException {
        TrustedPlatformClient client = this.plugin.getBootstrap().getTrustedPlatformClient();
        // Fetch the User for the Player in question
        HttpResponse<GraphQLResponse<List<User>>> networkResponse = client.getUsersService()
                .getUsersSync(new GetUsers().name(playerUuid.toString()));

        User user = null;

        if (networkResponse.body() != null) {
            GraphQLResponse<List<User>> response = networkResponse.body();
            if (!response.isEmpty()) {
                List<User> data = response.getData();
                if (data != null && !data.isEmpty()) {
                    user = data.get(0);
                }
            }
        }

        return user;
    }

    private User createUser(UUID playerUuid) throws IOException {
        TrustedPlatformClient client = this.plugin.getBootstrap().getTrustedPlatformClient();
        // Create the User for the Player in question
        HttpResponse<GraphQLResponse<User>> networkResponse = client.getUsersService()
                .createUserSync(new CreateUser().name(playerUuid.toString()));

        User user = null;

        if (networkResponse.body() != null) {
            GraphQLResponse<User> response = networkResponse.body();
            if (!response.isEmpty()) {
                user = response.getData();
            }
        }

        return user;
    }

    private Identity getIdentity() throws IOException {
        Identity identity = null;

        if (enjinCoinPlayer.getIdentityId() == null) {
            identity = createIdentity();
        } else {
            TrustedPlatformClient client = plugin.getBootstrap().getTrustedPlatformClient();
            HttpResponse<GraphQLResponse<List<Identity>>> networkResponse = client.getIdentitiesService()
                    .getIdentitiesSync(new GetIdentities().identityId(enjinCoinPlayer.getIdentityId()));

            if (networkResponse.isSuccess()) {
                GraphQLResponse<List<Identity>> response = networkResponse.body();
                if (response.isSuccess()) {
                    List<Identity> identities = response.getData();
                    if (identities.size() > 0) {
                        identity = identities.get(0);
                    }
                } else {
                    identity = null;
                }
            } else {
                identity = null;
            }
        }

        return identity;
    }

    private Identity createIdentity() throws IOException {
        TrustedPlatformClient client = this.plugin.getBootstrap().getTrustedPlatformClient();
        // Create the Identity for the App ID and Player in question
        HttpResponse<GraphQLResponse<Identity>> networkResponse = client.getIdentitiesService()
                .createIdentitySync(new CreateIdentity().userId(this.enjinCoinPlayer.getUserId()));

        Identity identity = null;

        if (networkResponse.body() != null) {
            GraphQLResponse<Identity> response = networkResponse.body();
            if (!response.isEmpty()) {
                identity = response.getData();
            }
        }

        return identity;
    }

    public static void create(BasePlugin plugin, EnjinCoinPlayer enjinCoinPlayer) {
        cleanUp(enjinCoinPlayer.getBukkitPlayer().getUniqueId());

        PlayerInitializationTask task = new PlayerInitializationTask(plugin, enjinCoinPlayer);
        // Note: TASK_PERIOD is measured in server ticks 20 ticks / second.
        task.runTaskTimerAsynchronously(plugin, TASK_DELAY, TASK_PERIOD);
    }

    public static void cleanUp(UUID playerUuid) {
        PlayerInitializationTask task = PLAYER_TASKS.remove(playerUuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

}
