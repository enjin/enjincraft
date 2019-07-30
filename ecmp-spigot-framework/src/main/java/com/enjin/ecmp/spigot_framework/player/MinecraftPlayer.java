package com.enjin.ecmp.spigot_framework.player;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.ecmp.spigot_framework.wallet.LegacyWallet;
import com.enjin.ecmp.spigot_framework.wallet.TokenWallet;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.balances.Balance;
import com.enjin.enjincoin.sdk.model.service.balances.GetBalances;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.model.service.users.GetUsers;
import com.enjin.enjincoin.sdk.model.service.users.User;
import com.enjin.ecmp.spigot_framework.event.IdentityLoadedEvent;
import com.enjin.ecmp.spigot_framework.trade.TradeView;
import com.enjin.ecmp.spigot_framework.util.MessageUtils;
import com.enjin.enjincoin.sdk.service.notifications.NotificationsService;
import com.enjin.java_commons.StringUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MinecraftPlayer {

    // Bukkit Fields
    private BasePlugin plugin;
    private Player bukkitPlayer;

    // User Data
    private Integer userId;

    // Identity Data
    private Integer identityId;
    private String ethereumAddress;
    private String linkingCode;
    private BigDecimal enjBalance;
    private BigDecimal ethBalance;
    private BigInteger enjAllowance;
    private TokenWallet tokenWallet;

    // dep
    private LegacyWallet wallet;

    // State Fields
    private boolean userLoaded;
    private boolean identityLoaded;

    // Scoreboard
    private boolean showScoreboard;
    private PlayerScoreboard scoreboard;

    // Trade Fields
    private List<MinecraftPlayer> sentTradeInvites = new ArrayList<>();
    private List<MinecraftPlayer> receivedTradeInvites = new ArrayList<>();
    private TradeView activeTradeView;

    public MinecraftPlayer(BasePlugin plugin, Player player) {
        this.plugin = plugin;
        this.bukkitPlayer = player;
        this.showScoreboard = true;
        this.scoreboard = new PlayerScoreboard(this);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> scoreboard.setEnabled(showScoreboard), 1);
    }

    public Player getBukkitPlayer() {
        return bukkitPlayer;
    }

    public void loadUser(User user) {
        if (user == null) {
            userId = null;
            userLoaded = false;
        } else {
            userId = user.getId();
            userLoaded = true;

            Optional<Identity> optionalIdentity = user.getIdentities().stream()
                    .filter(identity -> identity.getAppId().intValue() == plugin.getBootstrap().getConfig().getAppId())
                    .findFirst();
            optionalIdentity.ifPresent(identity -> identityId = identity.getId());
        }
    }

    public void loadIdentity(Identity identity) {
        if (identity == null) {
            identityId = null;
            ethereumAddress = null;
            linkingCode = null;
            identityLoaded = false;
            tokenWallet = null;
        } else {
            identityId = identity.getId();
            ethereumAddress = identity.getEthereumAddress();
            linkingCode = identity.getLinkingCode();
            ethBalance = identity.getEthBalance();
            enjBalance = identity.getEnjBalance();
            enjAllowance = identity.getEnjAllowance();
            identityLoaded = true;

            wallet = new LegacyWallet(plugin, bukkitPlayer.getUniqueId());
            wallet.populate(identity.getTokens());

            NotificationsService service = plugin.getBootstrap().getNotificationsService();
            boolean listening = service.isSubscribedToIdentity(identityId);

            if (linkingCode != null && !listening) {
                service.subscribeToIdentity(identityId);
            } else if (linkingCode == null && listening) {
                service.unsubscribeToIdentity(identityId);
            }

            scoreboard.update();

            Bukkit.getPluginManager().callEvent(new IdentityLoadedEvent(this));
        }

        if (isLinked()) {
            if (identity.getEnjAllowance() == null || identity.getEnjAllowance().doubleValue() <= 0.0) {
                TextComponent text = TextComponent.builder()
                        .content("Before you can send or trade items with other players you must approve the enj " +
                                "request in your wallet app.")
                        .color(TextColor.GOLD)
                        .build();
                MessageUtils.sendMessage(getBukkitPlayer(), text);
            }

            initWallet();
        }
    }

    public void initWallet() {
        if (StringUtils.isEmpty(ethereumAddress)) return;

        // populate wallet;
        TrustedPlatformClient client = plugin.getBootstrap().getTrustedPlatformClient();
        try {
            HttpResponse<GraphQLResponse<List<Balance>>> networkResponse = client.getBalancesService()
                    .getBalancesSync(new GetBalances().ethAddr(ethereumAddress));
            if (networkResponse.isSuccess()) {
                GraphQLResponse<List<Balance>> response = networkResponse.body();
                if (response.isSuccess()) {
                    List<Balance> balances = response.getData();
                    tokenWallet = new TokenWallet(plugin.getBootstrap(), balances);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadUser() {
        TrustedPlatformClient client = plugin.getBootstrap().getTrustedPlatformClient();
        // Fetch the User for the Player in question
        try {
            HttpResponse<GraphQLResponse<List<User>>> networkResponse = client.getUsersService()
                    .getUsersSync(new GetUsers().name(bukkitPlayer.getUniqueId().toString()));

            User user = null;
            // we likely need a legit reload function for the wallet to repopulate it.
            wallet = new LegacyWallet(plugin, bukkitPlayer.getUniqueId());

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

    public boolean isUserLoaded() {
        return userLoaded;
    }

    public boolean isIdentityLoaded() {
        return identityLoaded;
    }

    public boolean isLoaded() {
        return isUserLoaded() && isIdentityLoaded();
    }

    public boolean isLinked() {
        return isIdentityLoaded() && ethereumAddress != null;
    }

    protected void cleanUp() {
        PlayerInitializationTask.cleanUp(bukkitPlayer.getUniqueId());

        if (showScoreboard) {
            scoreboard.setEnabled(false);
        }

        plugin.getBootstrap().getNotificationsService().unsubscribeToIdentity(identityId);
        bukkitPlayer = null;
    }

    public List<MinecraftPlayer> getSentTradeInvites() {
        return sentTradeInvites;
    }

    public List<MinecraftPlayer> getReceivedTradeInvites() {
        return receivedTradeInvites;
    }

    public boolean showingScoreboard() {
        return showScoreboard;
    }

    public void showScoreboard(boolean showScoreboard) {
        this.showScoreboard = showScoreboard;
        scoreboard.setEnabled(showScoreboard);
    }

    public TradeView getActiveTradeView() {
        return activeTradeView;
    }

    public void setActiveTradeView(TradeView activeTradeView) {
        this.activeTradeView = activeTradeView;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getIdentityId() {
        return identityId;
    }

    public String getEthereumAddress() {
        return ethereumAddress;
    }

    public String getLinkingCode() {
        return linkingCode;
    }

    public BigDecimal getEnjBalance() {
        return enjBalance;
    }

    public BigDecimal getEthBalance() {
        return ethBalance;
    }

    public BigInteger getEnjAllowance() {
        return enjAllowance;
    }

    public LegacyWallet getWallet() {
        return wallet;
    }

    public TokenWallet getTokenWallet() {
        return tokenWallet;
    }
}
