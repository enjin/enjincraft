package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.client.Client;
import com.enjin.enjincoin.sdk.client.model.body.GraphQLResponse;
import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.sdk.client.service.users.vo.User;
import com.enjin.enjincoin.sdk.client.service.users.vo.data.UsersData;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MinecraftPlayer {

    // Bukkit Objects
    private BasePlugin plugin;
    private Player bukkitPlayer;

    // Trusted Platform Data
    private UserData userData;
    private Identity identity;
    private IdentityData identityData;
    private Wallet wallet;

    // State Fields
    private boolean userLoaded;
    private boolean identityLoaded;

    // Helper Objects
    private User user;

    // Scoreboard for the given player
    private Scoreboard board;

    public MinecraftPlayer(BasePlugin plugin, Player player) {
        this.plugin = plugin;
        this.bukkitPlayer = player;
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
    }

    public Player getBukkitPlayer() {
        return this.bukkitPlayer;
    }

    public UserData getUserData() {
        return this.userData;
    }

    public IdentityData getIdentityData() {
        return this.identityData;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Identity getIdentity() { return this.identity; }

    public boolean isUserLoaded() {
        return this.userLoaded;
    }

    public void loadUser(User user) {
        if (user == null) {
            return;
        }

        this.user = user;
        this.userData = new UserData(user);
        this.userLoaded = true;

        Integer appId = this.plugin.getBootstrap().getAppId();
        Optional<Identity> optionalIdentity = user.getIdentities().stream()
                .filter(identity -> identity.getAppId() == appId)
                .findFirst();
        optionalIdentity.ifPresent(this::loadIdentity);
    }

    public User getUser() { return this.user; }

    public void reloadUser() {
        Client client = this.plugin.getBootstrap().getSdkController().getClient();
        // Fetch the User for the Player in question
        try {
            Response<GraphQLResponse<UsersData>> networkResponse = client.getUsersService()
                    .getUsersSync(null, bukkitPlayer.getUniqueId().toString(), null);

            User user = null;
            // we likely need a legit reload function for the wallet to repopulate it.
            this.wallet = new Wallet(bukkitPlayer.getUniqueId());

            if (networkResponse.isSuccessful()) {
                GraphQLResponse<UsersData> response = networkResponse.body();
                if (!response.isEmpty()) {
                    UsersData data = response.getData();
                    if (!data.isEmpty()) {
                        user = data.getUsers().get(0);
                    }
                }
            }

            loadUser(user);
        } catch (Exception e) {
            System.out.println("Failed to reload user");
        }
    }

    public boolean isIdentityLoaded() {
        return this.identityLoaded;
    }

    public void loadIdentity(Identity identity) {
        if (identity == null) {
            return;
        }

        this.identity = identity;

        this.identityData = new IdentityData(identity);
        this.wallet = new Wallet(bukkitPlayer.getUniqueId());
        this.identityLoaded = true;

        this.wallet.populate(identity.getTokens());
    }

    public boolean isLoaded() {
        return isUserLoaded() && isIdentityLoaded();
    }

    protected void cleanUp() {
        this.bukkitPlayer = null;
    }

    public void setScoreBoard() {
        Objective obj = board.registerNewObjective("ENJ", "Wallet");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName("ENJ Wallet ");

        Score score11x = obj.getScore("§2");
        score11x.setScore(1);
        Score score = obj.getScore(ChatColor.WHITE + "ENJ: " + ChatColor.LIGHT_PURPLE + identity.getEnjBalance());
        score.setScore(1);
    }

    public void refresh() {
//        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
//            public void run() {
//                setScoreBoard();
//            }
//        });
    }
}
