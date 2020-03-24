package com.enjin.enjincraft.spigot.player;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.trade.TradeView;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import com.enjin.enjincraft.spigot.wallet.TokenWallet;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.http.HttpResponse;
import com.enjin.sdk.models.balance.Balance;
import com.enjin.sdk.models.balance.GetBalances;
import com.enjin.sdk.models.identity.GetIdentities;
import com.enjin.sdk.models.identity.Identity;
import com.enjin.sdk.models.identity.UnlinkIdentity;
import com.enjin.sdk.models.user.User;
import com.enjin.sdk.models.wallet.Wallet;
import com.enjin.sdk.services.notification.NotificationsService;
import com.enjin.java_commons.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnjPlayer {

    // Bukkit Fields
    private SpigotBootstrap bootstrap;
    private Player          bukkitPlayer;

    // User Data
    private Integer userId;

    // Identity Data
    private Integer     identityId;
    private Wallet      wallet;
    private String      linkingCode;
    private TokenWallet tokenWallet;

    // State Fields
    private boolean userLoaded;
    private boolean identityLoaded;

    // Trade Fields
    private List<EnjPlayer> sentTradeInvites     = new ArrayList<>();
    private List<EnjPlayer> receivedTradeInvites = new ArrayList<>();
    private TradeView       activeTradeView;

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
                                                      .filter(identity -> identity.getAppId()
                                                                                  .intValue() == bootstrap.getConfig()
                                                                                                          .getAppId())
                                                      .findFirst();
            optionalIdentity.ifPresent(identity -> identityId = identity.getId());
        }
    }

    public void loadIdentity(Identity identity) {
        if (identity == null) {
            identityId = null;
            wallet = null;
            linkingCode = null;
            identityLoaded = false;
            tokenWallet = null;
            return;
        }

        identityId = identity.getId();
        wallet = identity.getWallet();
        linkingCode = identity.getLinkingCode();

        identityLoaded = true;

        NotificationsService service   = bootstrap.getNotificationsService();
        boolean              listening = service.isSubscribedToIdentity(identityId);

        if (!listening) { service.subscribeToIdentity(identityId); }

        if (!isLinked()) {
            Bukkit.getScheduler().runTask(bootstrap.plugin(), this::removeTokenizedItems);
            return;
        }

        if (identity.getWallet().getEnjAllowance() == null || identity.getWallet()
                                                                      .getEnjAllowance()
                                                                      .doubleValue() <= 0.0) {
            Translation.WALLET_ALLOWANCENOTSET.send(bukkitPlayer);
        }

        initWallet();
    }

    public void initWallet() {
        if (wallet == null || StringUtils.isEmpty(wallet.getEthAddress())) { return; }

        try {
            HttpResponse<GraphQLResponse<List<Balance>>> networkResponse;
            networkResponse = bootstrap.getTrustedPlatformClient()
                                       .getBalanceService()
                                       .getBalancesSync(new GetBalances().ethAddress(wallet.getEthAddress()));
            if (!networkResponse.isSuccess()) { throw new NetworkException(networkResponse.code()); }

            GraphQLResponse<List<Balance>> graphQLResponse = networkResponse.body();
            if (!graphQLResponse.isSuccess()) { throw new GraphQLException(graphQLResponse.getErrors()); }

            tokenWallet = new TokenWallet(bootstrap, graphQLResponse.getData());
            validateInventory();
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    public void validateInventory() {
        tokenWallet.getBalances().forEach(MutableBalance::reset);
        if (bukkitPlayer == null) { return; }
        PlayerInventory inventory = bukkitPlayer.getInventory();
        for (int i = inventory.getSize() - 1; i >= 0; i--) {
            ItemStack is = inventory.getItem(i);
            String    id = TokenUtils.getTokenID(is);
            if (StringUtils.isEmpty(id)) { continue; }

            MutableBalance balance = tokenWallet.getBalance(id);
            if (balance == null) { continue; }

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

    public void reloadIdentity() {
        try {
            HttpResponse<GraphQLResponse<List<Identity>>> networkResponse;
            networkResponse = bootstrap.getTrustedPlatformClient()
                                       .getIdentityService()
                                       .getIdentitiesSync(new GetIdentities().identityId(identityId)
                                                                             .withLinkingCode()
                                                                             .withWallet());
            if (!networkResponse.isSuccess()) { throw new NetworkException(networkResponse.code()); }

            GraphQLResponse<List<Identity>> graphQLResponse = networkResponse.body();
            if (!graphQLResponse.isSuccess()) { throw new GraphQLException(graphQLResponse.getErrors()); }

            Identity identity = null;
            if (!graphQLResponse.getData().isEmpty()) { identity = graphQLResponse.getData().get(0); }

            loadIdentity(identity);
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    public void unlink() throws IOException {
        if (!isLinked()) { return; }

        bootstrap.getTrustedPlatformClient().getIdentityService()
                 .unlinkIdentitySync(new UnlinkIdentity().id(identityId));
    }

    public void unlinked() {
        if (!isLinked()) { return; }

        Translation.COMMAND_UNLINK_SUCCESS.send(bukkitPlayer);
        Translation.HINT_LINK.send(bukkitPlayer);

        Bukkit.getScheduler().runTask(bootstrap.plugin(), this::removeTokenizedItems);
        reloadIdentity();
    }

    public void removeTokenizedItems() {
        Inventory inventory = bukkitPlayer.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack is = inventory.getItem(i);
            if (is == null || is.getType() == Material.AIR) { continue; }

            String tokenId = TokenUtils.getTokenID(is);
            if (!StringUtils.isEmpty(tokenId)) { inventory.setItem(i, null); }
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
        return isIdentityLoaded() && wallet != null && !StringUtils.isEmpty(wallet.getEthAddress());
    }

    protected void cleanUp() {
        PlayerInitializationTask.cleanUp(bukkitPlayer.getUniqueId());
        NotificationsService service   = bootstrap.getNotificationsService();
        boolean              listening = service.isSubscribedToIdentity(identityId);
        if (listening) { service.unsubscribeToIdentity(identityId); }
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
        return wallet == null ? "" : wallet.getEthAddress();
    }

    public String getLinkingCode() {
        return linkingCode;
    }

    public BigDecimal getEnjBalance() {
        return wallet == null ? BigDecimal.ZERO : wallet.getEnjBalance();
    }

    public BigDecimal getEthBalance() {
        return wallet == null ? BigDecimal.ZERO : wallet.getEthBalance();
    }

    public BigDecimal getEnjAllowance() {
        return wallet == null ? BigDecimal.ZERO : wallet.getEnjAllowance();
    }

    public TokenWallet getTokenWallet() {
        return tokenWallet;
    }
}
