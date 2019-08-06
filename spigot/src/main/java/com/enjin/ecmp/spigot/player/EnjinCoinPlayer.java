package com.enjin.ecmp.spigot.player;

import com.enjin.ecmp.spigot.BasePlugin;
import com.enjin.ecmp.spigot.util.TokenUtils;
import com.enjin.ecmp.spigot.wallet.MutableBalance;
import com.enjin.ecmp.spigot.wallet.TokenWallet;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.balances.Balance;
import com.enjin.enjincoin.sdk.model.service.balances.GetBalances;
import com.enjin.enjincoin.sdk.model.service.identities.GetIdentities;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.model.service.users.GetUsers;
import com.enjin.enjincoin.sdk.model.service.users.User;
import com.enjin.ecmp.spigot.events.IdentityLoadedEvent;
import com.enjin.ecmp.spigot.trade.TradeView;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.enjincoin.sdk.service.notifications.NotificationsService;
import com.enjin.java_commons.StringUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnjinCoinPlayer {

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

    // State Fields
    private boolean userLoaded;
    private boolean identityLoaded;

    // Trade Fields
    private List<EnjinCoinPlayer> sentTradeInvites = new ArrayList<>();
    private List<EnjinCoinPlayer> receivedTradeInvites = new ArrayList<>();
    private TradeView activeTradeView;

    public EnjinCoinPlayer(BasePlugin plugin, Player player) {
        this.plugin = plugin;
        this.bukkitPlayer = player;
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
            ethBalance = null;
            enjBalance = null;
            enjAllowance = null;
            tokenWallet = null;
        } else {
            identityId = identity.getId();
            ethereumAddress = identity.getEthereumAddress();
            linkingCode = identity.getLinkingCode();
            ethBalance = identity.getEthBalance();
            enjBalance = identity.getEnjBalance();
            enjAllowance = identity.getEnjAllowance();
            identityLoaded = true;

            NotificationsService service = plugin.getBootstrap().getNotificationsService();
            boolean listening = service.isSubscribedToIdentity(identityId);

            if (linkingCode != null && !listening) {
                service.subscribeToIdentity(identityId);
            } else if (linkingCode == null && listening) {
                service.unsubscribeToIdentity(identityId);
            }

            Bukkit.getPluginManager().callEvent(new IdentityLoadedEvent(this));
        }

        if (isLinked()) {
            if (identity.getEnjAllowance() == null || identity.getEnjAllowance().doubleValue() <= 0.0) {
                TextComponent text = TextComponent.builder()
                        .content("Before you can send or trade items with other players you must approve the enj " +
                                "request in your wallet app.")
                        .color(TextColor.GOLD)
                        .build();
                MessageUtils.sendComponent(getBukkitPlayer(), text);
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
                    PlayerInventory inventory = bukkitPlayer.getInventory();
                    for (int i = inventory.getSize() - 1; i >= 0; i--) {
                        ItemStack is = inventory.getItem(i);
                        String id = TokenUtils.getTokenID(is);
                        if (!StringUtils.isEmpty(id)) {
                            MutableBalance balance = tokenWallet.getBalance(id);
                            if (balance != null) {
                                if (balance.amountAvailableForWithdrawal() == 0) {
                                    inventory.clear(i);
                                } else {
                                    if (balance.amountAvailableForWithdrawal() < is.getAmount()) {
                                        is.setAmount(balance.amountAvailableForWithdrawal());
                                    }

                                    balance.withdraw(is.getAmount());
                                }
                            }
                        }
                    }
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

    public void reloadIdentity() {
        TrustedPlatformClient client = plugin.getBootstrap().getTrustedPlatformClient();

        try {
            HttpResponse<GraphQLResponse<List<Identity>>> networkResponse = client.getIdentitiesService()
                    .getIdentitiesSync(new GetIdentities().identityId(identityId));

            Identity identity = null;
            if (networkResponse.isSuccess()) {
                GraphQLResponse<List<Identity>> response = networkResponse.body();
                if (response.isSuccess()) {
                    List<Identity> data = response.getData();
                    if (!data.isEmpty()) {
                        identity = data.get(0);
                    }
                }
            }

            loadIdentity(identity);
        } catch (IOException e) {
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
        plugin.getBootstrap().getNotificationsService().unsubscribeToIdentity(identityId);
        bukkitPlayer = null;
    }

    public List<EnjinCoinPlayer> getSentTradeInvites() {
        return sentTradeInvites;
    }

    public List<EnjinCoinPlayer> getReceivedTradeInvites() {
        return receivedTradeInvites;
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

    public TokenWallet getTokenWallet() {
        return tokenWallet;
    }
}
