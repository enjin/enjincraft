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
import com.enjin.sdk.model.service.balances.Balance;
import com.enjin.sdk.model.service.balances.GetBalances;
import com.enjin.sdk.model.service.identities.DeleteIdentity;
import com.enjin.sdk.model.service.identities.GetIdentities;
import com.enjin.sdk.model.service.identities.Identity;
import com.enjin.sdk.model.service.users.GetUsers;
import com.enjin.sdk.model.service.users.User;
import com.enjin.sdk.service.notifications.NotificationsService;
import com.enjin.java_commons.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
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

        if (!listening)
            service.subscribeToIdentity(identityId);

        if (!isLinked())
            return;

        if (identity.getEnjAllowance() == null || identity.getEnjAllowance().doubleValue() <= 0.0)
            Translation.WALLET_ALLOWANCENOTSET.send(bukkitPlayer);

        initWallet();
    }

    public void initWallet() {
        if (StringUtils.isEmpty(ethereumAddress))
            return;

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
            bootstrap.log(ex);
        }
    }

    public void validateInventory() {
        tokenWallet.getBalances().forEach(MutableBalance::reset);
        if (bukkitPlayer == null)
            return;
        PlayerInventory inventory = bukkitPlayer.getInventory();
        for (int i = inventory.getSize() - 1; i >= 0; i--) {
            ItemStack is = inventory.getItem(i);
            String id = TokenUtils.getTokenID(is);
            if (StringUtils.isEmpty(id))
                continue;

            MutableBalance balance = tokenWallet.getBalance(id);
            if (balance == null)
                continue;

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
            bootstrap.log(ex);
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
            bootstrap.log(ex);
        }
    }

    public void unlink() throws IOException {
        if (!isLinked())
            return;

        bootstrap.getTrustedPlatformClient().getIdentitiesService()
                .deleteIdentitySync(DeleteIdentity.unlink(identityId));

        unlinked();
    }

    public void unlinked() {
        if (!isLinked())
            return;

        Translation.COMMAND_UNLINK_SUCCESS.send(bukkitPlayer);
        Translation.HINT_LINK.send(bukkitPlayer);

        Bukkit.getScheduler().runTask(bootstrap.plugin(), () -> {
            Inventory inventory = bukkitPlayer.getInventory();

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack is = inventory.getItem(i);
                if (is == null || is.getType() == Material.AIR)
                    continue;
                String tokenId = TokenUtils.getTokenID(is);
                if (!StringUtils.isEmpty(tokenId))
                    inventory.setItem(i, null);
            }
        });

        reloadIdentity();
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
        NotificationsService service = bootstrap.getNotificationsService();
        boolean listening = service.isSubscribedToIdentity(identityId);
        if (listening)
            service.unsubscribeToIdentity(identityId);
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
