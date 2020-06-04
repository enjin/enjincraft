package com.enjin.enjincraft.spigot.player;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.http.HttpResponse;
import com.enjin.sdk.models.identity.CreateIdentity;
import com.enjin.sdk.models.identity.GetIdentities;
import com.enjin.sdk.models.identity.Identity;
import com.enjin.sdk.models.user.CreateUser;
import com.enjin.sdk.models.user.GetUsers;
import com.enjin.sdk.models.user.User;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInitializationTask extends BukkitRunnable {

    public static final Long TASK_DELAY  = 1L;
    public static final Long TASK_PERIOD = 20L;

    private static final Map<UUID, PlayerInitializationTask> PLAYER_TASKS = new ConcurrentHashMap<>();

    private SpigotBootstrap bootstrap;
    private EnjPlayer       player;

    private boolean inProgress = false;

    protected PlayerInitializationTask(SpigotBootstrap bootstrap, EnjPlayer player) {
        this.bootstrap = bootstrap;
        this.player = player;
    }

    @Override
    public void run() {
        if (this.inProgress || isCancelled()) { return; }

        this.inProgress = true;

        if (this.player.getBukkitPlayer() == null || !this.player.getBukkitPlayer().isOnline()) {
            cancel();
        } else {
            try {
                if (!this.player.isUserLoaded()) { loadUser(); }

                if (this.player.isUserLoaded() && !this.player.isIdentityLoaded()) {
                    loadIdentity();
                } else if (this.player.isLoaded() && !isCancelled()) { cancel(); }
            } catch (Exception ex) {
                bootstrap.log(ex);
            }
        }

        this.inProgress = false;
    }

    private void loadUser() {
        User user = getUser(player.getBukkitPlayer().getUniqueId());
        if (user != null) { this.player.loadUser(user); }
    }

    private void loadIdentity() {
        Identity identity = getIdentity();
        if (identity != null) {
            // A new identity has been created
            this.player.loadIdentity(identity);
            cancel();
        }
    }

    private User getUser(UUID playerUuid) {
        User user = fetchExistingUser(playerUuid);

        if (user == null) { user = createUser(playerUuid); }

        return user;
    }

    private User fetchExistingUser(UUID playerUuid) {
        // Fetch the User for the Player in question
        HttpResponse<GraphQLResponse<List<User>>> networkResponse = bootstrap.getTrustedPlatformClient()
                                                                             .getUserService()
                                                                             .getUsersSync(new GetUsers()
                                                                                                   .name(playerUuid.toString())
                                                                                                   .withUserIdentities()
                                                                                                   .withLinkingCode()
                                                                                                   .withLinkingCodeQr()
                                                                                                   .withWallet());
        if (!networkResponse.isSuccess()) { throw new NetworkException(networkResponse.code()); }

        GraphQLResponse<List<User>> graphQLResponse = networkResponse.body();
        if (!graphQLResponse.isSuccess()) { throw new GraphQLException(graphQLResponse.getErrors()); }


        User user = null;
        if (!graphQLResponse.getData().isEmpty()) { user = graphQLResponse.getData().get(0); }

        return user;
    }

    private User createUser(UUID playerUuid) {
        // Create the User for the Player in question
        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        HttpResponse<GraphQLResponse<User>> networkResponse = client
                .getUserService().createUserSync(new CreateUser()
                                                         .name(playerUuid.toString())
                                                         .withUserIdentities()
                                                         .withLinkingCode()
                                                         .withLinkingCodeQr());
        if (!networkResponse.isSuccess()) { throw new NetworkException(networkResponse.code()); }

        GraphQLResponse<User> graphQLResponse = networkResponse.body();
        if (!graphQLResponse.isSuccess()) { throw new GraphQLException(graphQLResponse.getErrors()); }

        return graphQLResponse.getData();
    }

    private Identity getIdentity() {
        Identity identity = null;

        if (player.getIdentityId() == null) {
            identity = createIdentity();
        } else {
            HttpResponse<GraphQLResponse<List<Identity>>> networkResponse;
            networkResponse = bootstrap.getTrustedPlatformClient()
                                       .getIdentityService()
                                       .getIdentitiesSync(new GetIdentities()
                                                                  .identityId(player.getIdentityId())
                                                                  .withLinkingCode()
                                                                  .withLinkingCodeQr()
                                                                  .withWallet());
            if (!networkResponse.isSuccess()) { throw new NetworkException(networkResponse.code()); }

            GraphQLResponse<List<Identity>> graphQLResponse = networkResponse.body();
            if (!graphQLResponse.isSuccess()) { throw new GraphQLException(graphQLResponse.getErrors()); }

            if (!graphQLResponse.getData().isEmpty()) { identity = graphQLResponse.getData().get(0); }
        }

        return identity;
    }

    private Identity createIdentity() {
        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        // Create the Identity for the App ID and Player in question
        HttpResponse<GraphQLResponse<Identity>> networkResponse = client.getIdentityService()
                                                                        .createIdentitySync(new CreateIdentity()
                                                                                                    .appId(client.getAppId())
                                                                                                    .userId(this.player.getUserId())
                                                                                                    .withLinkingCode()
                                                                                                    .withLinkingCodeQr());
        if (!networkResponse.isSuccess()) { throw new NetworkException(networkResponse.code()); }

        GraphQLResponse<Identity> graphQLResponse = networkResponse.body();
        if (!graphQLResponse.isSuccess()) { throw new GraphQLException(graphQLResponse.getErrors()); }

        return graphQLResponse.getData();
    }

    public static void create(SpigotBootstrap bootstrap, EnjPlayer player) {
        cleanUp(player.getBukkitPlayer().getUniqueId());

        PlayerInitializationTask task = new PlayerInitializationTask(bootstrap, player);
        // Note: TASK_PERIOD is measured in server ticks 20 ticks / second.
        task.runTaskTimerAsynchronously(bootstrap.plugin(), TASK_DELAY, TASK_PERIOD);
    }

    public static void cleanUp(UUID playerUuid) {
        PlayerInitializationTask task = PLAYER_TASKS.remove(playerUuid);
        if (task != null && !task.isCancelled()) { task.cancel(); }
    }

}
