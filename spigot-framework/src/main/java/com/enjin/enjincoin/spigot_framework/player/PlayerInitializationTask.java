package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.client.Client;
import com.enjin.enjincoin.sdk.client.model.body.GraphQLResponse;
import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.sdk.client.service.identities.vo.data.CreateIdentityData;
import com.enjin.enjincoin.sdk.client.service.users.vo.User;
import com.enjin.enjincoin.sdk.client.service.users.vo.data.CreateUserData;
import com.enjin.enjincoin.sdk.client.service.users.vo.data.UsersData;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import org.bukkit.scheduler.BukkitRunnable;
import retrofit2.Response;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInitializationTask extends BukkitRunnable {

    public static final Long TASK_DELAY = 1L;
    public static final Long TASK_PERIOD = 20L;

    private static final Map<UUID, PlayerInitializationTask> PLAYER_TASKS = new ConcurrentHashMap<>();

    private BasePlugin plugin;
    private MinecraftPlayer minecraftPlayer;

    private boolean inProgress = false;

    protected PlayerInitializationTask(BasePlugin plugin, MinecraftPlayer minecraftPlayer) {
        this.plugin = plugin;
        this.minecraftPlayer = minecraftPlayer;
    }

    @Override
    public void run() {
        if (this.inProgress || isCancelled()) {
            return;
        }

        this.inProgress = true;

        if (!this.minecraftPlayer.getBukkitPlayer().isOnline()) {
            cancel();
        } else {
            try {
                if (!this.minecraftPlayer.isUserLoaded()) {
                    User user = getUser(minecraftPlayer.getBukkitPlayer().getUniqueId());
                    if (user != null) {
                        // An existing user has been found or a new user has been created
                        this.minecraftPlayer.loadUser(user);
                    }
                }

                if (this.minecraftPlayer.isUserLoaded() && !this.minecraftPlayer.isIdentityLoaded()) {
                    Identity identity = createIdentity();
                    if (identity != null) {
                        // A new identity has been created
                        this.minecraftPlayer.loadIdentity(identity);
                        cancel();
                    }
                } else if (this.minecraftPlayer.isLoaded() && !isCancelled()) {
                    cancel();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.minecraftPlayer.getWallet().accessCheckoutManager().populate(this.plugin, minecraftPlayer.getBukkitPlayer(), this.minecraftPlayer.getWallet() );

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
        Client client = this.plugin.getBootstrap().getSdkController().getClient();
        // Fetch the User for the Player in question
        Response<GraphQLResponse<UsersData>> networkResponse = client.getUsersService()
                .getUsersSync(null, playerUuid.toString(),null);

        User user = null;

        if (networkResponse.isSuccessful()) {
            GraphQLResponse<UsersData> response = networkResponse.body();
            if (!response.isEmpty()) {
                UsersData data = response.getData();
                if (!data.isEmpty()) {
                    user = data.getUsers().get(0);
                }
            }
        }

        return user;
    }

    private User createUser(UUID playerUuid) throws IOException {
        Client client = this.plugin.getBootstrap().getSdkController().getClient();
        // Create the User for the Player in question
        Response<GraphQLResponse<CreateUserData>> networkResponse = client.getUsersService()
                .createUserSync(playerUuid.toString(), null, null);

        User user = null;

        if (networkResponse.isSuccessful()) {
            GraphQLResponse<CreateUserData> response = networkResponse.body();
            if (!response.isEmpty()) {
                CreateUserData data = response.getData();
                user = data.getUser();
            }
        }

        return user;
    }

    private Identity createIdentity() throws IOException {
        Client client = this.plugin.getBootstrap().getSdkController().getClient();
        // Create the Identity for the App ID and Player in question
        Response<GraphQLResponse<CreateIdentityData>> networkResponse = client.getIdentitiesService()
                .createIdentitySync(this.minecraftPlayer.getUserData().getId(), null, null);

        Identity identity = null;

        if (networkResponse.isSuccessful()) {
            GraphQLResponse<CreateIdentityData> response = networkResponse.body();
            if (!response.isEmpty()) {
                CreateIdentityData data = response.getData();
                identity = data.getIdentity();
            }
        }

        return identity;
    }

    public static void create(BasePlugin plugin, MinecraftPlayer minecraftPlayer) {
        cleanUp(minecraftPlayer.getBukkitPlayer().getUniqueId());

        PlayerInitializationTask task = new PlayerInitializationTask(plugin, minecraftPlayer);
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
