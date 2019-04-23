package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.Client;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.Result;
import com.enjin.enjincoin.sdk.model.service.identities.CreateIdentity;
import com.enjin.enjincoin.sdk.model.service.identities.CreateIdentityResult;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.model.service.users.*;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import org.bukkit.scheduler.BukkitRunnable;

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

        if (this.minecraftPlayer.getBukkitPlayer() == null || !this.minecraftPlayer.getBukkitPlayer().isOnline()) {
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

        if (this.minecraftPlayer.getWallet() != null)
            this.minecraftPlayer.getWallet().getCheckoutManager().populate(this.plugin, minecraftPlayer.getBukkitPlayer(), this.minecraftPlayer.getWallet());

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
        Result<GraphQLResponse<GetUsersResult>> networkResponse = client.getUsersService()
                .getUsersSync(new GetUsers().withName(playerUuid.toString()));

        User user = null;

        if (networkResponse.body() != null) {
            GraphQLResponse<GetUsersResult> response = networkResponse.body();
            if (!response.isEmpty()) {
                GetUsersResult data = response.getData();
                if (data.getUsers() != null && !data.getUsers().isEmpty()) {
                    user = data.getUsers().get(0);
                }
            }
        }

        return user;
    }

    private User createUser(UUID playerUuid) throws IOException {
        Client client = this.plugin.getBootstrap().getSdkController().getClient();
        // Create the User for the Player in question
        Result<GraphQLResponse<CreateUserResult>> networkResponse = client.getUsersService()
                .createUserSync(new CreateUser().withName(playerUuid.toString()));

        User user = null;

        if (networkResponse.body() != null) {
            GraphQLResponse<CreateUserResult> response = networkResponse.body();
            if (!response.isEmpty()) {
                CreateUserResult data = response.getData();
                user = data.getUser();
            }
        }

        return user;
    }

    private Identity createIdentity() throws IOException {
        Client client = this.plugin.getBootstrap().getSdkController().getClient();
        // Create the Identity for the App ID and Player in question
        Result<GraphQLResponse<CreateIdentityResult>> networkResponse = client.getIdentitiesService()
                .createIdentitySync(new CreateIdentity().withUserId(this.minecraftPlayer.getUserData().getId()));

        Identity identity = null;

        if (networkResponse.body() != null) {
            GraphQLResponse<CreateIdentityResult> response = networkResponse.body();
            if (!response.isEmpty()) {
                CreateIdentityResult data = response.getData();
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
