package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.model.service.users.GetUsers;
import com.enjin.enjincoin.sdk.model.service.users.User;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.event.IdentityLoadedEvent;
import com.enjin.enjincoin.spigot_framework.trade.TradeView;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.enjin.enjincoin.spigot_framework.wallet.LegacyWallet;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MinecraftPlayer {

    // Bukkit Fields
    private BasePlugin plugin;
    private Player bukkitPlayer;

    // Trusted Platform Data Fields
    private UserData userData;
    private Identity identity;
    private IdentityData identityData;
    private LegacyWallet wallet;

    // State Fields
    private boolean userLoaded;
    private boolean identityLoaded;

    // Scoreboard
    private boolean showScoreboard;
    private ENJScoreboard scoreboard;

    // Helper Fields
    private User user;

    // Trade Fields
    private List<MinecraftPlayer> sentTradeInvites = new ArrayList<>();
    private List<MinecraftPlayer> receivedTradeInvites = new ArrayList<>();
    private TradeView activeTradeView;

    public MinecraftPlayer(BasePlugin plugin, Player player) {
        this.plugin = plugin;
        this.bukkitPlayer = player;
        this.showScoreboard = true;
        this.scoreboard = new ENJScoreboard(this);
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

    public LegacyWallet getWallet() {
        return wallet;
    }

    public Identity getIdentity() {
        return this.identity;
    }

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
                .filter(identity -> identity.getAppId().intValue() == appId.intValue())
                .findFirst();
        optionalIdentity.ifPresent(this::loadIdentity);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> this.scoreboard.setEnabled(this.showScoreboard), 1);
    }

    public User getUser() {
        return this.user;
    }

    public void reloadUser() {
        TrustedPlatformClient client = this.plugin.getBootstrap().getSdkController().getClient();
        // Fetch the User for the Player in question
        try {
            HttpResponse<GraphQLResponse<List<User>>> networkResponse = client.getUsersService()
                    .getUsersSync(new GetUsers().name(bukkitPlayer.getUniqueId().toString()));

            User user = null;
            // we likely need a legit reload function for the wallet to repopulate it.
            this.wallet = new LegacyWallet(plugin, bukkitPlayer.getUniqueId());

            if (networkResponse.body() != null) {
                GraphQLResponse<List<User>> response = networkResponse.body();
                if (!response.isEmpty()) {
                    List<User> data = response.getData();
                    if (data != null && !data.isEmpty()) {
                        user = data.get(0);
                    }
                }
            }

            loadUser(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isIdentityLoaded() {
        return this.identityLoaded;
    }

    public void loadIdentity(Identity identity) {
        if (identity == null) {
            this.plugin.getBootstrap().debug("Failed to load identity: null");
            return;
        }

        this.plugin.getBootstrap().debug("Loading identity: " + identity.toString());
        this.identity = identity;

        this.identityData = new IdentityData(identity);
        this.wallet = new LegacyWallet(plugin, bukkitPlayer.getUniqueId());
        this.identityLoaded = true;

        this.wallet.populate(identity.getTokens());

        boolean listening = this.plugin.getBootstrap().getSdkController().getNotificationsService().isSubscribedToIdentity(identity.getId());

        if (identity.getLinkingCode() != null && !listening) {
            this.plugin.getBootstrap().getSdkController().getNotificationsService().subscribeToIdentity(identity.getId());
        } else if (identity.getLinkingCode() == null && listening) {
            this.plugin.getBootstrap().getSdkController().getNotificationsService().unsubscribeToIdentity(identity.getId());
        }

        if (identity.getLinkingCode() == null && (identity.getEnjAllowance() == null || identity.getEnjAllowance().doubleValue() <= 0.0)) {
            TextComponent text = TextComponent.builder()
                    .content("Before you can send or trade items with other players you must approve the enj " +
                            "request in your wallet app.")
                    .color(TextColor.GOLD)
                    .build();
            MessageUtils.sendMessage(getBukkitPlayer(), text);
        }

        Bukkit.getPluginManager().callEvent(new IdentityLoadedEvent(this));
    }

    public boolean isLoaded() {
        return isUserLoaded() && isIdentityLoaded();
    }

    public boolean isLinked() {
        return isIdentityLoaded() && this.identity.getEthereumAddress() != null;
    }

    protected void cleanUp() {
        PlayerInitializationTask.cleanUp(bukkitPlayer.getUniqueId());

        if (this.showScoreboard) {
            this.scoreboard.setEnabled(false);
        }

        this.plugin.getBootstrap().getSdkController().getNotificationsService().unsubscribeToIdentity(identity.getId());

        this.bukkitPlayer = null;
    }

    public List<MinecraftPlayer> getSentTradeInvites() {
        return sentTradeInvites;
    }

    public List<MinecraftPlayer> getReceivedTradeInvites() {
        return receivedTradeInvites;
    }

    public boolean showScoreboard() {
        return this.showScoreboard;
    }

    public void showScoreboard(boolean showScoreboard) {
        this.showScoreboard = showScoreboard;
        this.scoreboard.setEnabled(showScoreboard);
    }

    public void updateScoreboard() {
        if (showScoreboard) {
            this.scoreboard.update();
        }
    }

    public TradeView getActiveTradeView() {
        return activeTradeView;
    }

    public void setActiveTradeView(TradeView activeTradeView) {
        this.activeTradeView = activeTradeView;
    }
}
