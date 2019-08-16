package com.enjin.ecmp.spigot.player;

import com.enjin.ecmp.spigot.GraphQLException;
import com.enjin.ecmp.spigot.NetworkException;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.events.IdentityLoadedEvent;
import com.enjin.ecmp.spigot.trade.TradeView;
import com.enjin.ecmp.spigot.util.MessageUtils;
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

public class EnjPlayer {

    // Bukkit Fields
    private SpigotBootstrap bootstrap;
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
    private List<EnjPlayer> sentTradeInvites = new ArrayList<>();
    private List<EnjPlayer> receivedTradeInvites = new ArrayList<>();
    private TradeView activeTradeView;

    public EnjPlayer(SpigotBootstrap bootstrap, Player player) {
        this.bootstrap = bootstrap;
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
                    .filter(identity -> identity.getAppId().intValue() == bootstrap.getConfig().getAppId())
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
            return;
        }

        identityId = identity.getId();
        ethereumAddress = identity.getEthereumAddress();
        linkingCode = identity.getLinkingCode();
        ethBalance = identity.getEthBalance();
        enjBalance = identity.getEnjBalance();
        enjAllowance = identity.getEnjAllowance();
        identityLoaded = true;

        NotificationsService service = bootstrap.getNotificationsService();
        boolean listening = service.isSubscribedToIdentity(identityId);

        if (linkingCode != null && !listening) {
            service.subscribeToIdentity(identityId);
        } else if (linkingCode == null && listening) {
            service.unsubscribeToIdentity(identityId);
        }

        Bukkit.getPluginManager().callEvent(new IdentityLoadedEvent(this));

        if (!isLinked()) return;

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

    public void initWallet() {
        if (StringUtils.isEmpty(ethereumAddress)) return;

        try {
            HttpResponse<GraphQLResponse<List<Balance>>> networkResponse = bootstrap.getTrustedPlatformClient()
                    .getBalancesService().getBalancesSync(new GetBalances()
                            .ethAddr(ethereumAddress));
            if (!networkResponse.isSuccess())
                throw new NetworkException(networkResponse.code());

            GraphQLResponse<List<Balance>> graphQLResponse = networkResponse.body();
            if (!graphQLResponse.isSuccess())
                throw new GraphQLException(graphQLResponse.getErrors());

            tokenWallet = new TokenWallet(bootstrap, graphQLResponse.getData());
            validateInventory();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void validateInventory() {
        tokenWallet.getBalances().forEach(MutableBalance::reset);
        PlayerInventory inventory = bukkitPlayer.getInventory();
        for (int i = inventory.getSize() - 1; i >= 0; i--) {
            ItemStack is = inventory.getItem(i);
            String id = TokenUtils.getTokenID(is);
            if (StringUtils.isEmpty(id)) continue;

            MutableBalance balance = tokenWallet.getBalance(id);
            if (balance == null) continue;

            if (balance.amountAvailableForWithdrawal() == 0) {
                inventory.clear(i);
            } else {
                if (balance.amountAvailableForWithdrawal() < is.getAmount())
                    is.setAmount(balance.amountAvailableForWithdrawal());

                balance.withdraw(is.getAmount());
            }
        }
    }

    public void reloadUser() {
        try {
            HttpResponse<GraphQLResponse<List<User>>> networkResponse = bootstrap.getTrustedPlatformClient()
                    .getUsersService().getUsersSync(new GetUsers()
                            .name(bukkitPlayer.getUniqueId().toString()));
            if (!networkResponse.isSuccess())
                throw new NetworkException(networkResponse.code());

            GraphQLResponse<List<User>> graphQLResponse = networkResponse.body();
            if (!graphQLResponse.isSuccess())
                throw new GraphQLException(graphQLResponse.getErrors());

            User user = null;
            if (!graphQLResponse.getData().isEmpty())
                user = graphQLResponse.getData().get(0);

            loadUser(user);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void reloadIdentity() {
        try {
            HttpResponse<GraphQLResponse<List<Identity>>> networkResponse = bootstrap.getTrustedPlatformClient()
                    .getIdentitiesService().getIdentitiesSync(new GetIdentities()
                            .identityId(identityId));
            if (!networkResponse.isSuccess())
                throw new NetworkException(networkResponse.code());

            GraphQLResponse<List<Identity>> graphQLResponse = networkResponse.body();
            if (!graphQLResponse.isSuccess())
                throw new GraphQLException(graphQLResponse.getErrors());

            Identity identity = null;
            if (!graphQLResponse.getData().isEmpty())
                identity = graphQLResponse.getData().get(0);

            loadIdentity(identity);
        } catch (IOException ex) {
            ex.printStackTrace();
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
        bootstrap.getNotificationsService().unsubscribeToIdentity(identityId);
        bukkitPlayer = null;
    }

    public List<EnjPlayer> getSentTradeInvites() {
        return sentTradeInvites;
    }

    public List<EnjPlayer> getReceivedTradeInvites() {
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
