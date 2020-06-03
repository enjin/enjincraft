package com.enjin.enjincraft.spigot.player;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.token.TokenPermissionGraph;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.trade.TradeView;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import com.enjin.enjincraft.spigot.wallet.TokenWallet;
import com.enjin.enjincraft.spigot.wallet.TokenWalletView;
import com.enjin.minecraft_commons.spigot.ui.AbstractMenu;
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
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class EnjPlayer implements Listener {

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
    private EnjPermissionAttachment globalAttachment;
    private EnjPermissionAttachment worldAttachment;
    private Map<String, Set<String>> worldPermissionMap = new HashMap<>();

    // Trade Fields
    private List<EnjPlayer> sentTradeInvites     = new ArrayList<>();
    private List<EnjPlayer> receivedTradeInvites = new ArrayList<>();
    private TradeView       activeTradeView;

    // Wallet Fields
    private TokenWalletView activeWalletView;

    public EnjPlayer(SpigotBootstrap bootstrap, Player player) {
        this.bootstrap = bootstrap;
        this.bukkitPlayer = player;
        this.globalAttachment = new EnjPermissionAttachment(player, bootstrap.plugin());
        this.worldAttachment = new EnjPermissionAttachment(player, bootstrap.plugin());
        bootstrap.plugin().getServer().getPluginManager().registerEvents(this, bootstrap.plugin());
    }

    @EventHandler
    public void onPlayerWorldChanged(PlayerChangedWorldEvent event) {
        setWorldAttachment(bukkitPlayer.getWorld().getName());
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
        identityId = null;      // Assume player has no identity
        wallet = null;          //
        linkingCode = null;     //
        identityLoaded = false; //
        tokenWallet = null;     //
        globalAttachment.clear();   // Clears all permissions
        worldAttachment.clear();    //
        worldPermissionMap.clear(); //

        if (identity == null)
            return;

        identityId = identity.getId();
        wallet = identity.getWallet();
        linkingCode = identity.getLinkingCode();

        identityLoaded = true;

        NotificationsService service   = bootstrap.getNotificationsService();
        boolean              listening = service.isSubscribedToIdentity(identityId);

        if (!listening) { service.subscribeToIdentity(identityId); }

        if (isLinked()) {
          Bukkit.getScheduler().runTask(bootstrap.plugin(), this::addLinkPermissions);
        } else {
          Bukkit.getScheduler().runTask(bootstrap.plugin(), this::removeTokenizedItems);
          Bukkit.getScheduler().runTask(bootstrap.plugin(), this::removeLinkPermissions);
          return;
        }

        if (identity.getWallet().getEnjAllowance() == null || identity.getWallet()
                                                                      .getEnjAllowance()
                                                                      .doubleValue() <= 0.0) {
            Translation.WALLET_ALLOWANCENOTSET.send(bukkitPlayer);
        }

        initWallet();
        initPermissions();
    }

    public void initWallet() {
        if (wallet == null || StringUtils.isEmpty(wallet.getEthAddress())) { return; }

        try {
            HttpResponse<GraphQLResponse<List<Balance>>> networkResponse;
            networkResponse = bootstrap.getTrustedPlatformClient()
                                       .getBalanceService()
                                       .getBalancesSync(new GetBalances()
                                               .valGt(0)
                                               .ethAddress(wallet.getEthAddress()));
            if (!networkResponse.isSuccess()) { throw new NetworkException(networkResponse.code()); }

            GraphQLResponse<List<Balance>> graphQLResponse = networkResponse.body();
            if (!graphQLResponse.isSuccess()) { throw new GraphQLException(graphQLResponse.getErrors()); }

            tokenWallet = new TokenWallet(graphQLResponse.getData());
            validateInventory();
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    public void validateInventory() {
        if (bukkitPlayer == null)
            return;

        tokenWallet.getBalances().forEach(MutableBalance::reset);

        validateInventory(bukkitPlayer.getInventory());

        if (AbstractMenu.hasAnyMenu(bukkitPlayer)) {
            if (activeTradeView != null)
                activeTradeView.validateInventory();
            else if (activeWalletView != null)
                activeWalletView.validateInventory();
        }

        // Handles tokens in the player's cursor
        InventoryView view = bukkitPlayer.getOpenInventory();
        ItemStack     is   = view.getCursor();
        String        id   = TokenUtils.getTokenID(is);
        if (!StringUtils.isEmpty(id)) {
            MutableBalance balance = tokenWallet.getBalance(id);
            if (balance == null || balance.amountAvailableForWithdrawal() == 0) {
                view.setCursor(null);
            } else {
                if (balance.amountAvailableForWithdrawal() < is.getAmount()) {
                    is.setAmount(balance.amountAvailableForWithdrawal());
                }

                balance.withdraw(is.getAmount());

                TokenModel tokenModel = bootstrap.getTokenManager().getToken(id);
                String itemNBT = NBTItem.convertItemtoNBT(is).toString();

                if (!itemNBT.equals(tokenModel.getNbt())) {
                    ItemStack newStack = tokenModel.getItemStack();
                    newStack.setAmount(is.getAmount());
                    view.setCursor(newStack);
                }
            }
        }
    }

    private void validateInventory(PlayerInventory inventory) {
        if (inventory == null)
            return;

        for (int i = inventory.getSize() - 1; i >= 0; i--) {
            ItemStack is = inventory.getItem(i);
            String    id = TokenUtils.getTokenID(is);
            if (StringUtils.isEmpty(id))
                continue;

            MutableBalance balance = tokenWallet.getBalance(id);
            if (balance == null || balance.amountAvailableForWithdrawal() == 0) {
                inventory.clear(i);
            } else {
                if (balance.amountAvailableForWithdrawal() < is.getAmount()) {
                    is.setAmount(balance.amountAvailableForWithdrawal());
                }

                balance.withdraw(is.getAmount());

                TokenModel tokenModel = bootstrap.getTokenManager().getToken(id);
                String itemNBT = NBTItem.convertItemtoNBT(is).toString();

                if (!itemNBT.equals(tokenModel.getNbt())) {
                    ItemStack newStack = tokenModel.getItemStack();
                    newStack.setAmount(is.getAmount());
                    inventory.setItem(i, newStack);
                }
            }
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack is = getEquipment(slot);
            String    id = TokenUtils.getTokenID(is);
            if (StringUtils.isEmpty(id))
                continue;

            MutableBalance balance = tokenWallet.getBalance(id);
            if (balance == null || balance.amountAvailableForWithdrawal() == 0) {
                setEquipment(slot, null);
            } else {
                if (balance.amountAvailableForWithdrawal() < is.getAmount()) {
                    is.setAmount(balance.amountAvailableForWithdrawal());
                }

                balance.withdraw(is.getAmount());

                TokenModel tokenModel = bootstrap.getTokenManager().getToken(id);
                String itemNBT = NBTItem.convertItemtoNBT(is).toString();

                if (!itemNBT.equals(tokenModel.getNbt())) {
                    ItemStack newStack = tokenModel.getItemStack();
                    newStack.setAmount(is.getAmount());
                    setEquipment(slot, newStack);
                }
            }
        }
    }

    private ItemStack getEquipment(EquipmentSlot slot) {
        PlayerInventory inventory = bukkitPlayer.getInventory();
        ItemStack is = null;
        switch (slot) {
            case HAND:
                break;
            case OFF_HAND:
                is = inventory.getItemInOffHand();
                break;
            case CHEST:
                is = inventory.getChestplate();
                break;
            case LEGS:
                is = inventory.getLeggings();
                break;
            case HEAD:
                is = inventory.getHelmet();
                break;
            case FEET:
                is = inventory.getBoots();
                break;
            default:
                bootstrap.debug(String.format("Unsupported equipment slot type \"%s\"", slot.name()));
                break;
        }

        return is;
    }

    private void setEquipment(EquipmentSlot slot, ItemStack is) {
        PlayerInventory inventory = bukkitPlayer.getInventory();
        switch (slot) {
            case HAND:
                break;
            case OFF_HAND:
                inventory.setItemInOffHand(is);
                break;
            case CHEST:
                inventory.setChestplate(is);
                break;
            case LEGS:
                inventory.setLeggings(is);
                break;
            case HEAD:
                inventory.setHelmet(is);
                break;
            case FEET:
                inventory.setBoots(is);
                break;
            default:
                bootstrap.debug(String.format("Unsupported equipment slot type \"%s\"", slot.name()));
                break;
        }
    }

    public void updateToken(String id) {
        if (bukkitPlayer == null || tokenWallet == null)
            return;

        TokenModel tokenModel = bootstrap.getTokenManager().getToken(id);

        if (tokenModel == null)
            return;

        MutableBalance balance = tokenWallet.getBalance(tokenModel.getId());

        if (balance == null || balance.withdrawn() == 0)
            return;

        PlayerInventory inventory = bukkitPlayer.getInventory();

        // Updates any token in storage
        for (int i = 0; i < inventory.getStorageContents().length; i++) {
            ItemStack is = inventory.getItem(i);
            id           = TokenUtils.getTokenID(is);

            if (!StringUtils.isEmpty(id) && id.equals(tokenModel.getId())) {
                String itemNBT = NBTItem.convertItemtoNBT(is).toString();

                if (!itemNBT.equals(tokenModel.getNbt())) {
                    ItemStack newStack = tokenModel.getItemStack();
                    int amount = is.getAmount();

                    if (amount > newStack.getMaxStackSize()) {
                        balance.deposit(amount - newStack.getMaxStackSize());
                        amount = newStack.getMaxStackSize();
                    }

                    newStack.setAmount(amount);
                    inventory.setItem(i, newStack);
                }
            }
        }

        // Updates any token in equipment
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack is = getEquipment(slot);
            id           = TokenUtils.getTokenID(is);

            if (!StringUtils.isEmpty(id) && id.equals(tokenModel.getId())) {
                String itemNBT = NBTItem.convertItemtoNBT(is).toString();

                if (!itemNBT.equals(tokenModel.getNbt())) {
                    ItemStack newStack = tokenModel.getItemStack();
                    int amount = is.getAmount();

                    if (amount > newStack.getMaxStackSize()) {
                        balance.deposit(amount - newStack.getMaxStackSize());
                        amount = newStack.getMaxStackSize();
                    }

                    newStack.setAmount(amount);

                    if (slot == EquipmentSlot.OFF_HAND || slot == EquipmentSlot.HAND || is.getType() == newStack.getType()) {
                        setEquipment(slot, newStack);
                    } else {
                        setEquipment(slot, null);
                        balance.deposit(newStack.getAmount());
                    }
                }
            }
        }

        // Updates any token in cursor
        InventoryView view = bukkitPlayer.getOpenInventory();
        ItemStack is = view.getCursor();
        id           = TokenUtils.getTokenID(is);
        if (!StringUtils.isEmpty(id) && id.equals(tokenModel.getId())) {
            String itemNBT = NBTItem.convertItemtoNBT(is).toString();

            if (!itemNBT.equals(tokenModel.getNbt())) {
                ItemStack newStack = tokenModel.getItemStack();
                int amount = is.getAmount();

                if (amount > newStack.getMaxStackSize()) {
                    balance.deposit(amount - newStack.getMaxStackSize());
                    amount = newStack.getMaxStackSize();
                }

                newStack.setAmount(amount);
                view.setCursor(newStack);
            }
        }
    }

    public void initPermissions() {
        if (tokenWallet == null)
            return;

        // Assigns the permissions to the Enjin player
        TokenPermissionGraph graph = bootstrap.getTokenManager().getTokenPermissions();
        for (String tokenId : tokenWallet.getBalancesMap().keySet()) {
            Map<String, Set<String>> worldPerms = graph.getTokenPermissions(tokenId);

            // Checks if the token has assigned permissions
            if (worldPerms == null)
                continue;

            // Assigns global and world permissions
            worldPerms.forEach((world, perm) -> {
                if (world.equals(TokenManager.GLOBAL)) {
                    globalAttachment.addPermissions(perm);
                } else {
                    Set<String> perms = worldPermissionMap.computeIfAbsent(world, k -> new HashSet<>());
                    perms.addAll(perm);
                }
            });
        }

        setWorldAttachment(bukkitPlayer.getWorld().getName());
    }

    private void setWorldAttachment(String world) {
        worldAttachment.clear();

        Set<String> perms = worldPermissionMap.get(world);

        if (perms != null)
            worldAttachment.addPermissions(perms);
    }

    public void addPermission(String perm, String tokenId, String world) {
        if (tokenWallet == null)
            return;

        if (bootstrap.getTokenManager().getToken(tokenId) == null)
            return;

        if (world.equals(TokenManager.GLOBAL)) {
            addGlobalPermission(perm, tokenId);
            // Tries to remove any world permission, since global
            worldPermissionMap.keySet().forEach(nonGlobal -> removeWorldPermission(perm, nonGlobal));
        } else {
            addWorldPermission(perm, tokenId, world);
            // Tries to remove any global permission, since local to world
            removeGlobalPermission(perm);
        }
    }

    private void addGlobalPermission(String perm, String tokenId) {
        Map<String, Set<String>> worldPerms = bootstrap.getTokenManager().getTokenPermissions().getPermissionTokens(TokenManager.GLOBAL);

        if (worldPerms == null)
            return;

        // Gets the tokens with the given permission from the permission graph
        Set<String> permTokens = worldPerms.get(perm);

        if (permTokens == null)
            return;

        // Checks if the player needs to be given the permission
        if (!globalAttachment.hasPermission(perm) && permTokens.contains(tokenId))
            globalAttachment.setPermission(perm);
    }

    private void addWorldPermission(String perm, String tokenId, String world) {
        Map<String, Set<String>> worldPerms = bootstrap.getTokenManager().getTokenPermissions().getPermissionTokens(world);

        // Gets the tokens with the given permission from the permission graph
        Set<String> permTokens = worldPerms.get(perm);

        if (permTokens == null)
            return;

        // Checks if the player needs to be given the permission
        Set<String> perms = worldPermissionMap.computeIfAbsent(world, k -> new HashSet<>());
        if (!perms.contains(perm) && permTokens.contains(tokenId)) {
            perms.add(perm);

            String currentWorld = bukkitPlayer.getWorld().getName();
            if (currentWorld.equals(world))
                worldAttachment.setPermission(perm);
        }
    }

    public void removePermission(String perm, String world) {
        if (tokenWallet == null)
            return;

        if (world.equals(TokenManager.GLOBAL)) {
            removeGlobalPermission(perm);
            // Tries to remove any world permission too
            worldPermissionMap.keySet().forEach(nonGlobal -> removeWorldPermission(perm, nonGlobal));
        } else {
            removeWorldPermission(perm, world);
        }
    }

    private void removeGlobalPermission(String perm) {
        Map<String, Set<String>> worldPerms = bootstrap.getTokenManager().getTokenPermissions().getPermissionTokens(TokenManager.GLOBAL);

        if (worldPerms == null)
            return;

        // Gets the tokens with the given permission from the permission graph
        Set<String> permTokens = worldPerms.get(perm);

        if (permTokens == null)
            return;

        Set<String> tokens = tokenWallet.getBalancesMap().keySet();

        // Retains only the player's tokens with the permission
        Set<String> intersect = new HashSet<>(tokens);
        intersect.retainAll(permTokens);

        // Checks if the permission needs to be removed from the player
        if (globalAttachment.hasPermission(perm) && intersect.size() <= 0)
            globalAttachment.unsetPermission(perm);
    }

    private void removeWorldPermission(String perm, String world) {
        Map<String, Set<String>> worldPerms = bootstrap.getTokenManager().getTokenPermissions().getPermissionTokens(world);

        if (worldPerms == null)
            return;

        // Gets the tokens with the given permission from the permission graph
        Set<String> permTokens = worldPerms.get(perm);

        if (permTokens == null)
            return;

        Set<String> tokens = tokenWallet.getBalancesMap().keySet();

        // Retains only the player's tokens with the permission
        Set<String> intersect = new HashSet<>(tokens);
        intersect.retainAll(permTokens);

        // Checks if the permission needs to be removed from the player
        Set<String> perms = worldPermissionMap.computeIfAbsent(world, k -> new HashSet<>());
        if (perms.contains(perm) && intersect.size() <= 0) {
            perms.remove(perm);

            String currentWorld = bukkitPlayer.getWorld().getName();
            if (currentWorld.equals(world))
                worldAttachment.unsetPermission(perm);
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
            ItemStack is      = inventory.getItem(i);
            String    tokenId = TokenUtils.getTokenID(is);
            if (!StringUtils.isEmpty(tokenId)) { inventory.setItem(i, null); }
        }
    }

    public void addLinkPermissions() {
        bootstrap.getConfig().getLinkPermissions().forEach(perm -> globalAttachment.setPermission(perm));
    }

    public void removeLinkPermissions() {
        bootstrap.getConfig().getLinkPermissions().forEach(perm -> globalAttachment.unsetPermission(perm));
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
        if (identityId != null) {
            boolean listening = service.isSubscribedToIdentity(identityId);
            if (listening)
                service.unsubscribeToIdentity(identityId);
        }
        PlayerChangedWorldEvent.getHandlerList().unregister(this);
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

    public TokenWalletView getActiveWalletView() {
        return activeWalletView;
    }

    public void setActiveWalletView(TokenWalletView activeWalletView) {
        this.activeWalletView = activeWalletView;
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
