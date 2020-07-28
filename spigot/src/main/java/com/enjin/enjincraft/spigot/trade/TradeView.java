package com.enjin.enjincraft.spigot.trade;

import com.enjin.enjincraft.spigot.EnjTokenView;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.TargetPlayer;
import com.enjin.enjincraft.spigot.enums.Trader;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.util.MessageUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.util.UiUtils;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import com.enjin.enjincraft.spigot.wallet.TokenWallet;
import com.enjin.enjincraft.spigot.wallet.TokenWalletViewState;
import com.enjin.minecraft_commons.spigot.ui.*;
import com.enjin.minecraft_commons.spigot.ui.menu.ChestMenu;
import com.enjin.minecraft_commons.spigot.ui.menu.component.SimpleMenuComponent;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.Getter;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class TradeView extends ChestMenu implements EnjTokenView {

    public static final int INV_WIDTH = 9;

    private final SpigotBootstrap bootstrap;

    @Getter
    private final EnjPlayer viewer;
    @Getter
    private final EnjPlayer other;
    private final Trader traderType;

    @Getter
    private SimpleMenuComponent viewerItemsComponent;
    @Getter
    private SimpleMenuComponent viewerStatusComponent;
    @Getter
    private SimpleMenuComponent otherItemsComponent;
    @Getter
    private SimpleMenuComponent otherStatusComponent;

    private boolean playerReady = false;
    private boolean tradeApproved = false;
    private final ItemStack readyPane = createReadyPaneItemStack();
    private final ItemStack unreadyPane = createUnreadyPaneItemStack();
    private final ItemStack readyItem = createReadyItemStack();
    private final ItemStack unreadyItem = createUnreadyItemStack();

    public TradeView(SpigotBootstrap bootstrap, EnjPlayer viewer, EnjPlayer other, Trader traderType) {
        super("Trade", 6);
        this.bootstrap = bootstrap;
        this.viewer = viewer;
        this.other = other;
        this.traderType = traderType;
        init();
    }

    private void init() {
        allowPlayerInventoryInteractions(true);
        setCloseConsumer(this::closeMenuAction);

        //  Create the offering region for the viewing player
        viewerItemsComponent = new SimpleMenuComponent(new Dimension(4, 4));
        viewerItemsComponent.setAllowPlace(true);
        viewerItemsComponent.setAllowDrag(true);
        viewerItemsComponent.setAllowPickup(true);
        viewerItemsComponent.setSlotUpdateHandler((player, slot, oldItem, newItem) -> {
            TradeView otherView = other.getActiveTradeView();
            Position position = Position.toPosition(this, slot);
            otherView.setItem(other.getBukkitPlayer(), otherView.getOtherItemsComponent(), position, newItem);

            // Un-readies the trade for both parties
            unreadyAction();
            otherView.unreadyAction();
        });

        //  Create the status region for the viewing player
        viewerStatusComponent = new SimpleMenuComponent(new Dimension(4, 1));
        viewerStatusComponent.setItem(Position.of(0, 0), getPlayerHead(viewer.getBukkitPlayer(), TargetPlayer.SELF));

        Position readyPosition = Position.of(1, 0);
        viewerStatusComponent.setItem(readyPosition, readyItem);
        viewerStatusComponent.addAction(readyPosition, p -> {
            try {
                playerReady = true;
                setItem(p, viewerStatusComponent, Position.of(3, 0), readyPane);
                p.updateInventory();

                TradeView otherView = other.getActiveTradeView();
                otherView.setItem(other.getBukkitPlayer(), otherView.otherStatusComponent, Position.of(3, 0), readyPane);
                other.getBukkitPlayer().updateInventory();

                if (otherView.playerReady) {
                    List<ItemStack> viewerOffer = getOfferedItems();
                    List<ItemStack> otherOffer = otherView.getOfferedItems();
                    if (!viewerOffer.isEmpty() || !otherOffer.isEmpty()) {
                        tradeApproved = true;
                        otherView.tradeApproved = true;

                        if (traderType == Trader.INVITER)
                            bootstrap.getTradeManager().createTrade(viewer, other, viewerOffer, otherOffer);
                        else
                            bootstrap.getTradeManager().createTrade(other, viewer, otherOffer, viewerOffer);

                        closeMenu(p);
                    }
                }
            } catch (Exception ex) {
                bootstrap.log(ex);
            }
        }, ClickType.LEFT, ClickType.RIGHT);

        Position unreadyPosition = Position.of(2, 0);
        viewerStatusComponent.setItem(unreadyPosition, unreadyItem);
        viewerStatusComponent.addAction(unreadyPosition, p -> {
            playerReady = false;
            setItem(p, viewerStatusComponent, Position.of(3, 0), unreadyPane);
            p.updateInventory();

            TradeView otherView = other.getActiveTradeView();
            otherView.setItem(other.getBukkitPlayer(), otherView.otherStatusComponent, Position.of(3, 0), unreadyPane);
            other.getBukkitPlayer().updateInventory();
        }, ClickType.LEFT, ClickType.RIGHT);
        viewerStatusComponent.setItem(Position.of(3, 0), unreadyPane);

        // Create the offering region for the other player
        otherItemsComponent = new SimpleMenuComponent(new Dimension(4, 4));

        // Create the status region for the other player
        otherStatusComponent = new SimpleMenuComponent(new Dimension(4, 1));
        otherStatusComponent.setItem(Position.of(0, 0), getPlayerHead(other.getBukkitPlayer(), TargetPlayer.OTHER));
        otherStatusComponent.setItem(Position.of(3, 0), unreadyPane);

        // Creates the horizontal separator
        Component horizontalBarrier = UiUtils.createSeparator(new Dimension(9, 1));
        // Creates the upper vertical separator
        Component verticalBarrierTop = UiUtils.createSeparator(new Dimension(1, 4));
        // Creates the lower vertical separator
        Component verticalBarrierBottom = UiUtils.createSeparator(new Dimension(1, 1));

        addComponent(Position.of(0, 0), viewerItemsComponent);
        addComponent(Position.of(0, 5), viewerStatusComponent);
        addComponent(Position.of(5, 0), otherItemsComponent);
        addComponent(Position.of(5, 5), otherStatusComponent);
        addComponent(Position.of(4, 0), verticalBarrierTop);
        addComponent(Position.of(4, 5), verticalBarrierBottom);
        addComponent(Position.of(0, 4), horizontalBarrier);
    }

    protected void unreadyAction() {
        playerReady = false;
        setItem(viewer.getBukkitPlayer(), viewerStatusComponent, Position.of(3, 0), unreadyPane);

        TradeView otherView = other.getActiveTradeView();
        otherView.setItem(other.getBukkitPlayer(), otherView.otherStatusComponent, Position.of(3, 0), unreadyPane);
    }

    public List<ItemStack> getOfferedItems() {
        InventoryView view = viewer.getBukkitPlayer().getOpenInventory();
        List<ItemStack> items = new ArrayList<>();

        Dimension dimension = viewerItemsComponent.getDimension();
        int rows = dimension.getHeight();
        int cols = dimension.getWidth();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                ItemStack item = view.getItem(x + (y * INV_WIDTH));
                if (TokenUtils.isValidTokenItem(item))
                    items.add(item);
            }
        }

        return items;
    }

    public void open() {
        open(this.viewer.getBukkitPlayer());
    }

    @Override
    public void validateInventory() {
        TokenManager tokenManager = bootstrap.getTokenManager();
        InventoryView view = viewer.getBukkitPlayer().getOpenInventory();

        Dimension dimension = viewerItemsComponent.getDimension();
        int rows = dimension.getHeight();
        int cols = dimension.getWidth();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int       slot = x + (y * INV_WIDTH);
                ItemStack is   = view.getItem(slot);
                if (!TokenUtils.hasTokenData(is)) {
                    continue;
                } else if (!TokenUtils.isValidTokenItem(is)) {
                    view.setItem(slot, null);
                    updateSlotWithHandler(slot, is, null);
                    bootstrap.debug(String.format("Removed corrupted token from %s's trade window", viewer.getBukkitPlayer().getDisplayName()));
                    continue;
                }

                String         fullId     = TokenUtils.createFullId(TokenUtils.getTokenID(is),
                                                                    TokenUtils.getTokenIndex(is));
                TokenModel     tokenModel = tokenManager.getToken(fullId);
                MutableBalance balance    = viewer.getTokenWallet().getBalance(fullId);
                if (tokenModel == null
                        || balance == null
                        || balance.amountAvailableForWithdrawal() == 0) {
                    view.setItem(slot, null);
                    updateSlotWithHandler(slot, is, null);
                } else if (tokenModel.getWalletViewState() != TokenWalletViewState.WITHDRAWABLE) {
                    balance.deposit(is.getAmount());
                    view.setItem(slot, null);
                    updateSlotWithHandler(slot, is, null);
                } else {
                    if (balance.amountAvailableForWithdrawal() < is.getAmount()) {
                        is.setAmount(balance.amountAvailableForWithdrawal());
                        updateSlotWithHandler(slot, is, is);
                    }

                    balance.withdraw(is.getAmount());

                    updateTokenInInventory(tokenModel, balance, is, slot);
                }
            }
        }
    }

    private void updateTokenInInventory(TokenModel tokenModel,
                                        MutableBalance balance,
                                        ItemStack is,
                                        int slot) {
        ItemStack newStack = tokenModel.getItemStack(is.getAmount());
        if (newStack == null) {
            balance.deposit(is.getAmount());
            updateSlotWithHandler(slot, is, null);
            return;
        }

        String newNBT  = NBTItem.convertItemtoNBT(newStack).toString();
        String itemNBT = NBTItem.convertItemtoNBT(is).toString();
        if (itemNBT.equals(newNBT)) {
            return;
        } else if (is.getAmount() > newStack.getMaxStackSize()) {
            balance.deposit(is.getAmount() - newStack.getMaxStackSize());
            newStack.setAmount(newStack.getMaxStackSize());
        }

        updateSlotWithHandler(slot, is, newStack);
    }

    @Override
    public void updateInventory() {
        validateInventory();
    }

    private void updateSlotWithHandler(int slot, ItemStack oldItem, ItemStack newItem) {
        viewerItemsComponent.getSlotUpdateHandler()
                            .ifPresent(handler -> handler.handle(viewer.getBukkitPlayer(), slot, oldItem, newItem));
    }

    private ItemStack getPlayerHead(Player player, TargetPlayer target) {
        ItemStack is   = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) is.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(target == TargetPlayer.SELF ? "You" : player.getName());
            is.setItemMeta(meta);
        }

        return is;
    }

    private ItemStack createReadyItemStack() {
        ItemStack is   = new ItemStack(Material.HOPPER);
        ItemMeta  meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Ready Up");
            is.setItemMeta(meta);
        }

        return is;
    }

    private ItemStack createUnreadyItemStack() {
        ItemStack is   = new ItemStack(Material.BARRIER);
        ItemMeta  meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Unready");
            is.setItemMeta(meta);
        }

        return is;
    }

    private ItemStack createReadyPaneItemStack() {
        ItemStack is   = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta  meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Ready");
            is.setItemMeta(meta);
        }

        return is;
    }

    private ItemStack createUnreadyPaneItemStack() {
        ItemStack is   = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta  meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Not Ready");
            is.setItemMeta(meta);
        }

        return is;
    }

    @Override
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (viewer.getBukkitPlayer() != event.getWhoClicked())
            return;

        if (event.getClickedInventory() instanceof PlayerInventory) {
            ItemStack is = event.getCurrentItem();
            if (is == null || is.getType() == Material.AIR) {
                return;
            } else if (!TokenUtils.isValidTokenItem(is)) {
                event.setResult(Event.Result.DENY);
                return;
            }

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)
                moveToTradeInventory(event);
        } else {
            super.onInventoryClick(event);

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)
                moveToPlayerInventory(event);
        }
    }

    private void moveToTradeInventory(InventoryClickEvent event) {
        event.setCancelled(true);

        InventoryView view = viewer.getBukkitPlayer().getOpenInventory();
        TokenManager tokenManager = bootstrap.getTokenManager();
        ItemStack  currItem  = event.getCurrentItem();
        TokenModel currModel = tokenManager.getToken(currItem);
        if (currItem == null || currModel == null)
            return;

        Dimension dimension = viewerItemsComponent.getDimension();
        int rows = dimension.getHeight();
        int cols = dimension.getWidth();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int       slot      = x + (y * INV_WIDTH);
                ItemStack otherItem = view.getItem(slot);
                if (otherItem == null || otherItem.getType() == Material.AIR) {
                    // Transfers the whole stack
                    view.setItem(slot, event.getCurrentItem());
                    updateSlotWithHandler(slot, otherItem, event.getCurrentItem());
                    Objects.requireNonNull(event.getClickedInventory()).setItem(event.getSlot(), null);
                    return;
                }

                TokenModel otherModel = tokenManager.getToken(otherItem);
                if (otherModel == currModel) {
                    // Combines what is possible with the other stack
                    int amount = Math.min(otherItem.getMaxStackSize(), otherItem.getAmount() + currItem.getAmount());
                    currItem.setAmount(currItem.getAmount() - (amount - otherItem.getAmount()));
                    otherItem.setAmount(amount);

                    updateSlotWithHandler(slot, otherItem, otherItem);

                    if (currItem.getAmount() <= 0) {
                        Objects.requireNonNull(event.getClickedInventory()).setItem(event.getSlot(), null);
                        return;
                    }
                }
            }
        }
    }

    private void moveToPlayerInventory(InventoryClickEvent event) {
        event.setCancelled(true);

        TokenManager tokenManager = bootstrap.getTokenManager();
        PlayerInventory playerInventory = viewer.getBukkitPlayer().getInventory();
        ItemStack  currItem  = event.getCurrentItem();
        TokenModel currModel = tokenManager.getToken(currItem);
        if (currItem == null || currModel == null)
            return;

        for (int i = 0; i < playerInventory.getStorageContents().length; i++) {
            ItemStack otherItem = playerInventory.getItem(i);
            if (otherItem == null || otherItem.getType() == Material.AIR) {
                // Transfers the whole stack
                Objects.requireNonNull(event.getClickedInventory()).setItem(event.getSlot(), null);
                updateSlotWithHandler(event.getSlot(), currItem, null);
                playerInventory.setItem(i, currItem);
                return;
            }

            TokenModel otherModel = tokenManager.getToken(otherItem);
            if (otherModel == currModel) {
                // Combines what is possible with the other stack
                int amount = Math.min(otherItem.getMaxStackSize(), otherItem.getAmount() + currItem.getAmount());
                currItem.setAmount(currItem.getAmount() - (amount - otherItem.getAmount()));
                otherItem.setAmount(amount);

                if (currItem.getAmount() > 0) {
                    updateSlotWithHandler(event.getSlot(), currItem, currItem);
                } else {
                    Objects.requireNonNull(event.getClickedInventory()).setItem(event.getSlot(), null);
                    updateSlotWithHandler(event.getSlot(), currItem, null);
                    return;
                }
            }
        }
    }

    @Override
    protected void onClose(Player player) {
        HandlerList.unregisterAll(this);
        super.onClose(player);
    }

    private void closeMenuAction(Player player, AbstractMenu menu) {
        if (player != viewer.getBukkitPlayer())
            return;

        viewer.setActiveTradeView(null);

        TradeView otherTradeView = other.getActiveTradeView();
        if (otherTradeView != null) {
            otherTradeView.removePlayer(other.getBukkitPlayer());
            otherTradeView.destroy();
        }

        if (!tradeApproved && otherTradeView == null)
            informViewerOfCancellation();

        returnItems(player);
        destroy();
    }

    private void returnItems(Player player) {
        InventoryView view = viewer.getBukkitPlayer().getOpenInventory();
        Inventory playerInventory = player.getInventory();
        List<ItemStack> items = new ArrayList<>();

        Dimension dimension = viewerItemsComponent.getDimension();
        int rows = dimension.getHeight();
        int cols = dimension.getWidth();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                ItemStack is = view.getItem(x + (y * INV_WIDTH));
                if (TokenUtils.hasTokenData(is))
                    items.add(is);
            }
        }

        Map<Integer, ItemStack> leftOver = playerInventory.addItem(items.toArray(new ItemStack[0]));
        if (leftOver.size() > 0) {
            TokenWallet tokenWallet = viewer.getTokenWallet();
            for (ItemStack is : leftOver.values()) {
                MutableBalance balance = tokenWallet.getBalance(TokenUtils.getTokenID(is),
                                                                TokenUtils.getTokenIndex(is));
                if (balance != null)
                    balance.deposit(is.getAmount());
            }
        }
    }

    private void informViewerOfCancellation() {
        MessageUtils.sendComponent(viewer.getBukkitPlayer(), TextComponent.builder("")
                .color(TextColor.GRAY)
                .append(TextComponent.builder(other.getBukkitPlayer().getName())
                        .color(TextColor.GOLD)
                        .build())
                .append(TextComponent.builder(" has cancelled the trade.")
                        .build())
                .build());
    }
}
