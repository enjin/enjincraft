package com.enjin.ecmp.spigot.player;

import com.enjin.ecmp.spigot.GraphQLException;
import com.enjin.ecmp.spigot.NetworkException;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.identities.CreateIdentity;
import com.enjin.enjincoin.sdk.model.service.identities.GetIdentities;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.model.service.users.CreateUser;
import com.enjin.enjincoin.sdk.model.service.users.GetUsers;
import com.enjin.enjincoin.sdk.model.service.users.User;
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

    private SpigotBootstrap bootstrap;
    private EnjPlayer player;

    private boolean inProgress = false;

    protected PlayerInitializationTask(SpigotBootstrap bootstrap, EnjPlayer player) {
        this.bootstrap = bootstrap;
        this.player = player;
    }

    @Override
    public void run() {
        if (this.inProgress || isCancelled()) return;

        this.inProgress = true;

        if (this.player.getBukkitPlayer() == null || !this.player.getBukkitPlayer().isOnline()) {
            cancel();
        } else {
            try {
                if (!this.player.isUserLoaded()) {
                    User user = getUser(player.getBukkitPlayer().getUniqueId());
                    if (user != null) {
                        // An existing user has been found or a new user has been created
                        this.player.loadUser(user);
                    }
                }

                if (this.player.isUserLoaded() && !this.player.isIdentityLoaded()) {
                    Identity identity = getIdentity();
                    if (identity != null) {
                        // A new identity has been created
                        this.player.loadIdentity(identity);
                        cancel();
                    }
                } else if (this.player.isLoaded() && !isCancelled()) {
                    cancel();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

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
        // Fetch the User for the Player in question
        HttpResponse<GraphQLResponse<List<User>>> networkResponse = bootstrap.getTrustedPlatformClient()
                .getUsersService().getUsersSync(new GetUsers()
                        .name(playerUuid.toString()));

        User user = null;

        if (networkResponse.isSuccess()) {
            GraphQLResponse<List<User>> graphQLResponse = networkResponse.body();
            if (graphQLResponse.isSuccess() && graphQLResponse.getData().size() > 0) {
                user = graphQLResponse.getData().get(0);
            } else {
                throw new GraphQLException(graphQLResponse.getErrors());
            }
        } else {
            throw new NetworkException(networkResponse.code());
        }

        return user;
    }

    private User createUser(UUID playerUuid) throws IOException {
        // Create the User for the Player in question
        HttpResponse<GraphQLResponse<User>> networkResponse = bootstrap.getTrustedPlatformClient()
                .getUsersService().createUserSync(new CreateUser()
                        .name(playerUuid.toString()));

        User user = null;

        if (networkResponse.isSuccess()) {
            GraphQLResponse<User> graphQLResponse = networkResponse.body();
            if (graphQLResponse.isSuccess()) {
                user = graphQLResponse.getData();
            } else {
                throw new GraphQLException(graphQLResponse.getErrors());
            }
        } else {
            throw new NetworkException(networkResponse.code());
        }

        return user;
    }

    private Identity getIdentity() throws IOException {
        Identity identity = null;

        if (player.getIdentityId() == null) {
            identity = createIdentity();
        } else {
            HttpResponse<GraphQLResponse<List<Identity>>> networkResponse = bootstrap.getTrustedPlatformClient()
                    .getIdentitiesService().getIdentitiesSync(new GetIdentities()
                            .identityId(player.getIdentityId()));

            if (networkResponse.isSuccess()) {
                GraphQLResponse<List<Identity>> graphQLResponse = networkResponse.body();
                if (graphQLResponse.isSuccess() && graphQLResponse.getData().size() > 0) {
                    identity = graphQLResponse.getData().get(0);
                } else {
                    throw new GraphQLException(graphQLResponse.getErrors());
                }
            } else {
                throw new NetworkException(networkResponse.code());
            }
        }

        return identity;
    }

    private Identity createIdentity() throws IOException {
        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        // Create the Identity for the App ID and Player in question
        HttpResponse<GraphQLResponse<Identity>> networkResponse = client.getIdentitiesService()
                .createIdentitySync(new CreateIdentity().userId(this.player.getUserId()));

        Identity identity = null;

        if (networkResponse.isSuccess()) {
            GraphQLResponse<Identity> graphQLResponse = networkResponse.body();
            if (graphQLResponse.isSuccess()) {
                identity = graphQLResponse.getData();
            } else {
                throw new GraphQLException(graphQLResponse.getErrors());
            }
        } else {
            throw new NetworkException(networkResponse.code());
        }

        return identity;
    }

    public static void create(SpigotBootstrap bootstrap, EnjPlayer player) {
        cleanUp(player.getBukkitPlayer().getUniqueId());

        PlayerInitializationTask task = new PlayerInitializationTask(bootstrap, player);
        // Note: TASK_PERIOD is measured in server ticks 20 ticks / second.
        task.runTaskTimerAsynchronously(bootstrap.plugin(), TASK_DELAY, TASK_PERIOD);
    }

    public static void cleanUp(UUID playerUuid) {
        PlayerInitializationTask task = PLAYER_TASKS.remove(playerUuid);
        if (task != null && !task.isCancelled()) task.cancel();
    }

}
