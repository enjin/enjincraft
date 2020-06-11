package com.enjin.enjincraft.spigot.player;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.trade.TradeView;
import com.enjin.enjincraft.spigot.util.QrUtils;
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
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.*;

import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

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
    private Image       linkingCodeQr;
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

    // Mutexes
    protected final Object linkingCodeQrLock = new Object();

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
        identityId     = null;  // Assume player has no identity
        wallet         = null;  //
        linkingCode    = null;  //
        setLinkingCodeQr(null); //
        identityLoaded = false; //
        tokenWallet    = null;  //
        globalAttachment.clear();   // Clears all permissions
        worldAttachment.clear();    //
        worldPermissionMap.clear(); //

        if (identity == null)
            return;

        identityId = identity.getId();
        wallet = identity.getWallet();
        linkingCode = identity.getLinkingCode();
        FetchQrImageTask.fetch(bootstrap, this, identity.getLinkingCodeQr());

        identityLoaded = true;

        NotificationsService service   = bootstrap.getNotificationsService();
        boolean              listening = service.isSubscribedToIdentity(identityId);

        if (!listening) { service.subscribeToIdentity(identityId); }

        if (!isLinked()) {
            Bukkit.getScheduler().runTask(bootstrap.plugin(), this::removeTokenizedItems);
            return;
        }

        removeQrMap();

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

        // Handles any token in the player's cursor
        TokenManager tokenManager = bootstrap.getTokenManager();
        InventoryView view = bukkitPlayer.getOpenInventory();
        ItemStack is    = view.getCursor();
        String    id    = TokenUtils.getTokenID(is);
        String    index = TokenUtils.getTokenIndex(is);
        if (StringUtils.isEmpty(id) ^ StringUtils.isEmpty(index)) {
            view.setCursor(null);
            bootstrap.debug(String.format("Removed corrupted token from %s's cursor", bukkitPlayer.getDisplayName()));
        } else if (!StringUtils.isEmpty(id) && !StringUtils.isEmpty(index)) {
            String         fullId  = TokenUtils.createFullId(id, index);
            MutableBalance balance = tokenWallet.getBalance(fullId);
            if (!tokenManager.hasToken(fullId)
                    || balance == null
                    || balance.amountAvailableForWithdrawal() == 0) {
                view.setCursor(null);
            } else {
                if (balance.amountAvailableForWithdrawal() < is.getAmount()) {
                    is.setAmount(balance.amountAvailableForWithdrawal());
                }

                balance.withdraw(is.getAmount());

                ItemStack newStack = tokenManager.getToken(fullId).getItemStack();
                newStack.setAmount(is.getAmount());

                String newNBT  = NBTItem.convertItemtoNBT(newStack).toString();
                String itemNBT = NBTItem.convertItemtoNBT(is).toString();
                if (!itemNBT.equals(newNBT))
                    view.setCursor(newStack);
            }
        }
    }

    private void validateInventory(PlayerInventory inventory) {
        if (inventory == null)
            return;

        TokenManager tokenManager = bootstrap.getTokenManager();

        // Validates standard inventory
        for (int i = inventory.getSize() - 1; i >= 0; i--) {
            ItemStack is    = inventory.getItem(i);
            String    id    = TokenUtils.getTokenID(is);
            String    index = TokenUtils.getTokenIndex(is);
            if (StringUtils.isEmpty(id) && StringUtils.isEmpty(index)) {
                continue;
            } else if (StringUtils.isEmpty(id) ^ StringUtils.isEmpty(index)) {
                inventory.clear(i);
                bootstrap.debug(String.format("Removed corrupted token from %s's inventory", bukkitPlayer.getDisplayName()));
                continue;
            }

            String         fullId  = TokenUtils.createFullId(id, index);
            MutableBalance balance = tokenWallet.getBalance(fullId);
            if (!tokenManager.hasToken(fullId)
                    || balance == null
                    || balance.amountAvailableForWithdrawal() == 0) {
                inventory.clear(i);
            } else {
                if (balance.amountAvailableForWithdrawal() < is.getAmount())
                    is.setAmount(balance.amountAvailableForWithdrawal());

                balance.withdraw(is.getAmount());

                ItemStack newStack = tokenManager.getToken(fullId).getItemStack();
                newStack.setAmount(is.getAmount());

                String newNBT  = NBTItem.convertItemtoNBT(newStack).toString();
                String itemNBT = NBTItem.convertItemtoNBT(is).toString();
                if (!itemNBT.equals(newNBT)) {
                    if (is.getAmount() > newStack.getMaxStackSize()) {
                        balance.deposit(is.getAmount() - newStack.getMaxStackSize());
                        newStack.setAmount(newStack.getMaxStackSize());
                    }

                    inventory.setItem(i, newStack);
                }
            }
        }

        // Validates equipment slots
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack is    = getEquipment(slot);
            String    id    = TokenUtils.getTokenID(is);
            String    index = TokenUtils.getTokenIndex(is);
            if (StringUtils.isEmpty(id) && StringUtils.isEmpty(index)) {
                continue;
            } else if (StringUtils.isEmpty(id) ^ StringUtils.isEmpty(index)) {
                setEquipment(slot, null);
                bootstrap.debug(String.format("Removed corrupted token from %s's equipment", bukkitPlayer.getDisplayName()));
                continue;
            }

            String         fullId  = TokenUtils.createFullId(id, index);
            MutableBalance balance = tokenWallet.getBalance(fullId);
            if (!tokenManager.hasToken(fullId)
                    || balance == null
                    || balance.amountAvailableForWithdrawal() == 0) {
                setEquipment(slot, null);
            } else {
                if (balance.amountAvailableForWithdrawal() < is.getAmount())
                    is.setAmount(balance.amountAvailableForWithdrawal());

                balance.withdraw(is.getAmount());

                ItemStack newStack = tokenManager.getToken(fullId).getItemStack();
                newStack.setAmount(is.getAmount());

                String newNBT  = NBTItem.convertItemtoNBT(newStack).toString();
                String itemNBT = NBTItem.convertItemtoNBT(is).toString();
                if (!itemNBT.equals(newNBT)) {
                    if (is.getAmount() > newStack.getMaxStackSize()) {
                        balance.deposit(is.getAmount() - newStack.getMaxStackSize());
                        newStack.setAmount(newStack.getMaxStackSize());
                    }

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

        MutableBalance balance = tokenWallet.getBalance(tokenModel.getFullId());
        if (balance == null || balance.withdrawn() == 0)
            return;

        PlayerInventory inventory = bukkitPlayer.getInventory();

        // Updates any token in storage
        for (int i = 0; i < inventory.getStorageContents().length; i++) {
            ItemStack is         = inventory.getItem(i);
            String    tokenId    = TokenUtils.getTokenID(is);
            String    tokenIndex = TokenUtils.getTokenIndex(is);
            if (StringUtils.isEmpty(tokenId) || StringUtils.isEmpty(tokenIndex))
                continue;

            String fullId = TokenUtils.createFullId(tokenId, tokenIndex);
            if (fullId.equals(tokenModel.getFullId())) {
                ItemStack newStack = tokenModel.getItemStack();
                newStack.setAmount(is.getAmount());

                String newNBT  = NBTItem.convertItemtoNBT(newStack).toString();
                String itemNBT = NBTItem.convertItemtoNBT(is).toString();
                if (!itemNBT.equals(newNBT)) {
                    if (is.getAmount() > newStack.getMaxStackSize()) {
                        balance.deposit(is.getAmount() - newStack.getMaxStackSize());
                        newStack.setAmount(newStack.getMaxStackSize());
                    }

                    inventory.setItem(i, newStack);
                }
            }
        }

        // Updates any token in equipment
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack is         = getEquipment(slot);
            String    tokenId    = TokenUtils.getTokenID(is);
            String    tokenIndex = TokenUtils.getTokenIndex(is);
            if (StringUtils.isEmpty(tokenId) || StringUtils.isEmpty(tokenIndex))
                continue;

            String fullId = TokenUtils.createFullId(tokenId, tokenIndex);
            if (fullId.equals(tokenModel.getFullId())) {
                ItemStack newStack = tokenModel.getItemStack();
                newStack.setAmount(is.getAmount());

                String newNBT  = NBTItem.convertItemtoNBT(newStack).toString();
                String itemNBT = NBTItem.convertItemtoNBT(is).toString();
                if (!itemNBT.equals(newNBT)) {
                    if (is.getAmount() > newStack.getMaxStackSize()) {
                        balance.deposit(is.getAmount() - newStack.getMaxStackSize());
                        newStack.setAmount(newStack.getMaxStackSize());
                    }

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
        ItemStack is         = view.getCursor();
        String    tokenId    = TokenUtils.getTokenID(is);
        String    tokenIndex = TokenUtils.getTokenIndex(is);
        if (!StringUtils.isEmpty(tokenId) && !StringUtils.isEmpty(tokenIndex)) {
            String fullId = TokenUtils.createFullId(tokenId, tokenIndex);
            if (fullId.equals(tokenModel.getFullId())) {
                ItemStack newStack = tokenModel.getItemStack();
                newStack.setAmount(is.getAmount());

                String newNBT = NBTItem.convertItemtoNBT(newStack).toString();
                String itemNBT = NBTItem.convertItemtoNBT(is).toString();
                if (!itemNBT.equals(newNBT)) {
                    if (is.getAmount() > newStack.getMaxStackSize()) {
                        balance.deposit(is.getAmount() - newStack.getMaxStackSize());
                        newStack.setAmount(newStack.getMaxStackSize());
                    }

                    view.setCursor(newStack);
                }
            }
        }

        if (activeTradeView != null)
            activeTradeView.updateInventory();
        else if (activeWalletView != null)
            activeWalletView.updateInventory();
    }

    public void initPermissions() {
        if (tokenWallet == null)
            return;

        Set<String> baseFullIds = new HashSet<>();

        for (MutableBalance balance : tokenWallet.getBalances()) {
            if (balance.balance() == 0)
                continue;

            String fullId     = TokenUtils.createFullId(balance.id(), balance.index());
            String baseFullId = TokenUtils.normalizeFullId(fullId);
            if (baseFullId != null && !baseFullId.equals(fullId)) // Collects the ids for non-fungible base models
                baseFullIds.add(baseFullId);

            initPermissions(fullId);
        }

        baseFullIds.forEach(this::initPermissions);

        setWorldAttachment(bukkitPlayer.getWorld().getName());
    }

    private void initPermissions(String fullId) {
        // Checks if the token has assigned permissions
        Map<String, Set<String>> worldPerms = bootstrap.getTokenManager()
                .getTokenPermissions()
                .getTokenPermissions(fullId);
        if (worldPerms == null)
            return;

        // Assigns global and world permissions
        worldPerms.forEach((world, perms) -> {
            if (world.equals(TokenManager.GLOBAL))
                globalAttachment.addPermissions(perms);
            else
                worldPermissionMap.computeIfAbsent(world, k -> new HashSet<>()).addAll(perms);
        });
    }

    private void setWorldAttachment(String world) {
        worldAttachment.clear();

        Set<String> perms = worldPermissionMap.get(world);
        if (perms != null)
            worldAttachment.addPermissions(perms);
    }

    public void addTokenPermissions(TokenModel tokenModel) {
        if (tokenWallet == null || tokenModel == null)
            return;

        MutableBalance balance = tokenWallet.getBalance(tokenModel.getFullId());
        if (balance == null || balance.balance() == 0)
            return;

        tokenModel.getPermissionsMap().forEach((world, perms) -> {
            perms.forEach(perm -> addTokenPermission(tokenModel, perm, world));
        });

        // Adds the permissions from the base model if necessary
        boolean applyBasePermissions = tokenModel.isNonFungibleInstance()
                && !hasNonfungibleInstance(tokenModel.getId(), Collections.singleton(tokenModel.getIndex()));
        if (applyBasePermissions) {
            TokenModel baseModel = bootstrap.getTokenManager().getToken(tokenModel.getId());
            if (baseModel != null) {
                baseModel.getPermissionsMap().forEach((world, perms) -> {
                    perms.forEach(perm -> addTokenPermission(baseModel, perm, world));
                });
            }
        }
    }

    public void addPermission(String perm, String id, String world) {
        if (tokenWallet == null)
            return;

        TokenModel tokenModel = bootstrap.getTokenManager().getToken(id);
        if (tokenModel != null
                && tokenModel.isNonfungible()
                && tokenModel.isBaseModel()
                && hasNonfungibleInstance(id)) { // Checks if base model of non-fungible and if permission should be added
            addTokenPermission(tokenModel, perm, world);
        } else if (tokenModel != null) {
            MutableBalance balance = tokenWallet.getBalance(tokenModel.getFullId());
            if (balance != null && balance.balance() > 0)
                addTokenPermission(tokenModel, perm, world);
        }
    }

    private void addTokenPermission(TokenModel tokenModel, String perm, String world) {
        if (world.equals(TokenManager.GLOBAL)) {
            addGlobalPermission(perm, tokenModel.getFullId());
            // Tries to remove any world permission, since global
            worldPermissionMap.keySet().forEach(nonGlobal -> removeWorldPermission(perm, nonGlobal));
        } else {
            addWorldPermission(perm, tokenModel.getFullId(), world);
            // Tries to remove any global permission, since local to world
            removeGlobalPermission(perm);
        }
    }

    private void addGlobalPermission(String perm, String fullId) {
        Map<String, Set<String>> worldPerms = bootstrap.getTokenManager()
                .getTokenPermissions()
                .getPermissionTokens(TokenManager.GLOBAL);
        if (worldPerms == null)
            return;

        // Gets the tokens with the given permission from the permission graph
        Set<String> permTokens = worldPerms.get(perm);
        if (permTokens == null)
            return;

        // Checks if the player needs to be given the permission
        if (!globalAttachment.hasPermission(perm) && permTokens.contains(fullId))
            globalAttachment.setPermission(perm);
    }

    private void addWorldPermission(String perm, String fullId, String world) {
        Map<String, Set<String>> worldPerms = bootstrap.getTokenManager()
                .getTokenPermissions()
                .getPermissionTokens(world);

        // Gets the tokens with the given permission from the permission graph
        Set<String> permTokens = worldPerms.get(perm);
        if (permTokens == null)
            return;

        // Checks if the player needs to be given the permission
        Set<String> perms = worldPermissionMap.computeIfAbsent(world, k -> new HashSet<>());
        if (!perms.contains(perm) && permTokens.contains(fullId)) {
            perms.add(perm);

            String currentWorld = bukkitPlayer.getWorld().getName();
            if (currentWorld.equals(world))
                worldAttachment.setPermission(perm);
        }
    }

    public void removeTokenPermissions(TokenModel tokenModel) {
        if (tokenWallet == null || tokenModel == null)
            return;

        MutableBalance balance = tokenWallet.getBalance(tokenModel.getFullId());
        if (balance != null && balance.balance() > 0)
            return;

        tokenModel.getPermissionsMap().forEach((world, perms) -> {
            perms.forEach(perm -> removePermission(perm, world));
        });

        // Removes the permissions from the base model if necessary
        boolean removeBasePermissions = tokenModel.isNonFungibleInstance()
                && !hasNonfungibleInstance(tokenModel.getId(), Collections.singleton(tokenModel.getIndex()));
        if (removeBasePermissions) {
            TokenModel baseModel = bootstrap.getTokenManager().getToken(tokenModel.getId());
            if (baseModel != null) {
                baseModel.getPermissionsMap().forEach((world, perms) -> {
                    perms.forEach(perm -> removePermission(perm, world));
                });
            }
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
        Map<String, Set<String>> worldPerms = bootstrap.getTokenManager()
                .getTokenPermissions()
                .getPermissionTokens(TokenManager.GLOBAL);
        if (worldPerms == null)
            return;

        // Gets the tokens with the given permission from the permission graph
        Set<String> permTokens = worldPerms.get(perm);
        if (permTokens == null)
            return;

        Set<String> intersect = retainPermissionTokens(permTokens);

        // Checks if the permission needs to be removed from the player
        if (globalAttachment.hasPermission(perm) && intersect.size() <= 0)
            globalAttachment.unsetPermission(perm);
    }

    private void removeWorldPermission(String perm, String world) {
        Map<String, Set<String>> worldPerms = bootstrap.getTokenManager()
                .getTokenPermissions()
                .getPermissionTokens(world);
        if (worldPerms == null)
            return;

        // Gets the tokens with the given permission from the permission graph
        Set<String> permTokens = worldPerms.get(perm);
        if (permTokens == null)
            return;

        Set<String> intersect = retainPermissionTokens(permTokens);

        // Checks if the permission needs to be removed from the player
        Set<String> perms = worldPermissionMap.computeIfAbsent(world, k -> new HashSet<>());
        if (perms.contains(perm) && intersect.size() <= 0) {
            perms.remove(perm);

            String currentWorld = bukkitPlayer.getWorld().getName();
            if (currentWorld.equals(world))
                worldAttachment.unsetPermission(perm);
        }
    }

    private Set<String> retainPermissionTokens(Set<String> permTokens) {
        Set<String> intersect = new HashSet<>();

        // Collects the full ids of all tokens that the player owns
        for (Map.Entry<String, MutableBalance> entry : tokenWallet.getBalancesMap().entrySet()) {
            String         fullId  = entry.getKey();
            MutableBalance balance = entry.getValue();

            if (balance.balance() > 0) {
                intersect.add(fullId);

                String baseFullId = TokenUtils.normalizeFullId(fullId);
                if (baseFullId != null && !baseFullId.equals(fullId)) // Collects the ids for non-fungible base models
                    intersect.add(baseFullId);
            }
        }

        // Retains only the player's tokens with the permission
        intersect.retainAll(permTokens);

        return intersect;
    }

    public void reloadIdentity() {
        try {
            HttpResponse<GraphQLResponse<List<Identity>>> networkResponse;
            networkResponse = bootstrap.getTrustedPlatformClient()
                                       .getIdentityService()
                                       .getIdentitiesSync(new GetIdentities().identityId(identityId)
                                                                             .withLinkingCode()
                                                                             .withLinkingCodeQr()
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

    public void unlink() {
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

    public void removeQrMap() {
        InventoryView   view      = bukkitPlayer.getOpenInventory();
        PlayerInventory inventory = bukkitPlayer.getInventory();

        Inventory top    = view.getTopInventory();
        Inventory bottom = view.getBottomInventory();
        int size = top.getSize()
                + bottom.getSize()
                - inventory.getExtraContents().length
                - inventory.getArmorContents().length;
        for (int i = 0; i < size; i++) {
            if (QrUtils.hasQrTag(view.getItem(i)))
                view.setItem(i, null);
        }

        if (QrUtils.hasQrTag(view.getCursor()))
            view.setCursor(null);

        if (QrUtils.hasQrTag(inventory.getItemInOffHand()))
            inventory.setItemInOffHand(null);
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

    public boolean hasNonfungibleInstance(@NonNull String id) throws IllegalArgumentException {
        return hasNonfungibleInstance(id, null);
    }

    public boolean hasNonfungibleInstance(@NonNull String id,
                                          Collection<String> ignoredIndices) throws IllegalArgumentException {
        TokenModel baseModel = bootstrap.getTokenManager().getToken(id);
        if (baseModel == null)
            throw new IllegalArgumentException(String.format("Token of id \"%s\" is not registered in token manager", id));
        else if (!baseModel.isNonfungible() || !baseModel.isBaseModel())
            throw new IllegalArgumentException(String.format("Token of id \"%s\" is not a base model of a non-fungible token", id));
        else if (tokenWallet == null)
            return false;

        Set<String> indices = ignoredIndices == null
                ? new HashSet<>()
                : new HashSet<>(ignoredIndices);

        List<MutableBalance> balances = tokenWallet.getBalances();
        for (MutableBalance balance : balances) {
            if (balance.balance() > 0
                    && balance.id().equals(baseModel.getId())
                    && !indices.contains(balance.index()))
                return true;
        }

        return false;
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

    protected void setLinkingCodeQr(Image linkingCodeQr) {
        synchronized (linkingCodeQrLock) {
            this.linkingCodeQr = linkingCodeQr;
        }
    }

    public Image getLinkingCodeQr() {
        synchronized (linkingCodeQrLock) {
            return linkingCodeQr;
        }
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
