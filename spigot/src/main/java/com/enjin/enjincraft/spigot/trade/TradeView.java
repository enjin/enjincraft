package com.enjin.enjincraft.spigot.trade;

import com.enjin.enjincraft.spigot.EnjTokenView;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.TargetPlayer;
import com.enjin.enjincraft.spigot.enums.Trader;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.MessageUtils;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.util.UiUtils;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import com.enjin.enjincraft.spigot.wallet.TokenWallet;
import com.enjin.minecraft_commons.spigot.ui.*;
import com.enjin.minecraft_commons.spigot.ui.menu.ChestMenu;
import com.enjin.minecraft_commons.spigot.ui.menu.component.SimpleMenuComponent;
import de.tr7zw.changeme.nbtapi.NBTItem;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TradeView extends ChestMenu implements EnjTokenView {

    public static final int INV_WIDTH = 9;

    private SpigotBootstrap bootstrap;

    private EnjPlayer viewer;
    private EnjPlayer other;
    private Trader traderType;

    private SimpleMenuComponent viewerItemsComponent;
    private SimpleMenuComponent viewerStatusComponent;

    private SimpleMenuComponent otherItemsComponent;
    private SimpleMenuComponent otherStatusComponent;

    private boolean playerReady = false;
    private boolean tradeApproved = false;
    private ItemStack readyPane = createReadyPaneItemStack();
    private ItemStack unreadyPane = createUnreadyPaneItemStack();
    private ItemStack readyItem = createReadyItemStack();
    private ItemStack unreadyItem = createUnreadyItemStack();

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
        this.viewerItemsComponent = new SimpleMenuComponent(new Dimension(4, 4));
        this.viewerItemsComponent.setAllowPlace(true);
        this.viewerItemsComponent.setAllowDrag(true);
        this.viewerItemsComponent.setAllowPickup(true);
        this.viewerItemsComponent.setSlotUpdateHandler((player, slot, oldItem, newItem) -> {
            TradeView otherView = this.other.getActiveTradeView();
            Position position = Position.toPosition(this, slot);
            otherView.setItem(this.other.getBukkitPlayer(), otherView.getOtherItemsComponent(), position, newItem);

            // Un-readies the trade for both parties
            unreadyAction();
            otherView.unreadyAction();
        });

        //  Create the status region for the viewing player
        this.viewerStatusComponent = new SimpleMenuComponent(new Dimension(4, 1));
        this.viewerStatusComponent.setItem(Position.of(0, 0), getPlayerHead(viewer.getBukkitPlayer(), TargetPlayer.SELF));

        Position readyPosition = Position.of(1, 0);
        this.viewerStatusComponent.setItem(readyPosition, readyItem);
        this.viewerStatusComponent.addAction(readyPosition, p -> {
            try {
                this.playerReady = true;
                setItem(p, this.viewerStatusComponent, Position.of(3, 0), readyPane);
                p.updateInventory();

                TradeView otherView = this.other.getActiveTradeView();
                otherView.setItem(other.getBukkitPlayer(), otherView.otherStatusComponent, Position.of(3, 0), readyPane);
                other.getBukkitPlayer().updateInventory();

                if (otherView.playerReady) {
                    List<ItemStack> viewerOffer = getOfferedItems();
                    List<ItemStack> otherOffer = otherView.getOfferedItems();
                    if (!viewerOffer.isEmpty() || !otherOffer.isEmpty()) {
                        tradeApproved = true;
                        otherView.tradeApproved = true;

                        if (traderType == Trader.INVITER) {
                            bootstrap.getTradeManager().createTrade(viewer, other, viewerOffer, otherOffer);
                        } else {
                            bootstrap.getTradeManager().createTrade(other, viewer, otherOffer, viewerOffer);
                        }

                        closeMenu(p);
                    }
                }
            } catch (Exception ex) {
                bootstrap.log(ex);
            }
        }, ClickType.LEFT, ClickType.RIGHT);

        Position unreadyPosition = Position.of(2, 0);
        this.viewerStatusComponent.setItem(unreadyPosition, unreadyItem);
        this.viewerStatusComponent.addAction(unreadyPosition, p -> {
            unreadyAction();
            p.updateInventory();
            other.getBukkitPlayer().updateInventory();
        }, ClickType.LEFT, ClickType.RIGHT);
        this.viewerStatusComponent.setItem(Position.of(3, 0), unreadyPane);

        // Create the offering region for the other player
        this.otherItemsComponent = new SimpleMenuComponent(new Dimension(4, 4));

        // Create the status region for the other player
        this.otherStatusComponent = new SimpleMenuComponent(new Dimension(4, 1));
        this.otherStatusComponent.setItem(Position.of(0, 0), getPlayerHead(other.getBukkitPlayer(), TargetPlayer.OTHER));
        this.otherStatusComponent.setItem(Position.of(3, 0), unreadyPane);

        // Creates the horizontal separator
        Component horizontalBarrier = UiUtils.createSeparator(new Dimension(9, 1));
        // Creates the upper vertical separator
        Component verticalBarrierTop = UiUtils.createSeparator(new Dimension(1, 4));
        // Creates the lower vertical separator
        Component verticalBarrierBottom = UiUtils.createSeparator(new Dimension(1, 1));

        addComponent(Position.of(0, 0), this.viewerItemsComponent);
        addComponent(Position.of(0, 5), this.viewerStatusComponent);
        addComponent(Position.of(5, 0), this.otherItemsComponent);
        addComponent(Position.of(5, 5), this.otherStatusComponent);
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

    public EnjPlayer getViewer() {
        return viewer;
    }

    public EnjPlayer getOther() {
        return other;
    }

    public Component getViewerItemsComponent() {
        return viewerItemsComponent;
    }

    public Component getOtherItemsComponent() {
        return otherItemsComponent;
    }

    public SimpleMenuComponent getViewerStatusComponent() {
        return viewerStatusComponent;
    }

    public SimpleMenuComponent getOtherStatusComponent() {
        return otherStatusComponent;
    }

    public List<ItemStack> getOfferedItems() {
        List<ItemStack> items = new ArrayList<>();

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                InventoryView view = this.viewer.getBukkitPlayer().getOpenInventory();
                ItemStack item = view.getItem(x + (y * INV_WIDTH));
                String    id   = TokenUtils.getTokenID(item);
                if (!StringUtils.isEmpty(id))
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
        Dimension dimension = viewerItemsComponent.getDimension();
        int rows = dimension.getHeight();
        int cols = dimension.getWidth();

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int           slot = x + (y * INV_WIDTH);
                InventoryView view = this.viewer.getBukkitPlayer().getOpenInventory();
                ItemStack     is   = view.getItem(slot);
                String        id   = TokenUtils.getTokenID(is);

                if (StringUtils.isEmpty(id))
                    continue;

                MutableBalance balance = viewer.getTokenWallet().getBalance(id);
                if (balance == null || balance.amountAvailableForWithdrawal() == 0) {
                    view.setItem(slot, null);
                    updateSlotWithHandler(slot, is, null);
                } else {
                    boolean changed = false;

                    if (balance.amountAvailableForWithdrawal() < is.getAmount()) {
                        is.setAmount(balance.amountAvailableForWithdrawal());
                        changed = true;
                    }

                    balance.withdraw(is.getAmount());

                    ItemStack newStack = bootstrap.getTokenManager().getToken(id).getItemStack();
                    newStack.setAmount(is.getAmount());

                    String newNBT  = NBTItem.convertItemtoNBT(newStack).toString();
                    String itemNBT = NBTItem.convertItemtoNBT(is).toString();
                    if (!itemNBT.equals(newNBT)) {
                        changed = true;

                        int amount = is.getAmount();
                        if (amount > newStack.getMaxStackSize()) {
                            balance.deposit(amount - newStack.getMaxStackSize());
                            amount = newStack.getMaxStackSize();
                        }

                        newStack.setAmount(amount);
                    }

                    if (changed) {
                        view.setItem(slot, newStack);
                        updateSlotWithHandler(slot, is, newStack);
                    }
                }
            }
        }
    }

    private void updateSlotWithHandler(int slot, ItemStack oldItem, ItemStack newItem) {
        Optional<SlotUpdateHandler> slotUpdateHandler = viewerItemsComponent.getSlotUpdateHandler();
        slotUpdateHandler.ifPresent(handler -> handler.handle(viewer.getBukkitPlayer(), slot, oldItem, newItem));
    }

    private ItemStack getPlayerHead(Player player, TargetPlayer target) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(target == TargetPlayer.SELF ? "You" : player.getName());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createReadyItemStack() {
        ItemStack stack = new ItemStack(Material.HOPPER);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Ready Up");
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createUnreadyItemStack() {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Unready");
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createReadyPaneItemStack() {
        ItemStack stack = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Ready");
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createUnreadyPaneItemStack() {
        ItemStack stack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Not Ready");
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (viewer.getBukkitPlayer() != event.getWhoClicked())
            return;

        if (event.getClickedInventory() instanceof PlayerInventory) {
            ItemStack is = event.getCurrentItem();
            String    id = TokenUtils.getTokenID(is);

            if (id == null) {
                return;
            } else if (StringUtils.isEmpty(id)) {
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

        InventoryView view = this.viewer.getBukkitPlayer().getOpenInventory();
        ItemStack currItem = event.getCurrentItem();
        String    currId   = TokenUtils.getTokenID(currItem);
        Dimension dimension = viewerItemsComponent.getDimension();
        int rows = dimension.getHeight();
        int cols = dimension.getWidth();

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int       slot = x + (y * INV_WIDTH);
                ItemStack is   = view.getItem(slot);
                String    id   = TokenUtils.getTokenID(is);

                if (id == null) {
                    // Transfers the whole stack
                    view.setItem(slot, event.getCurrentItem());
                    updateSlotWithHandler(slot, is, event.getCurrentItem());
                    event.getClickedInventory().setItem(event.getSlot(), null);
                    return;
                } else if (id.equals(currId)) {
                    // Combines what is possible with the other stack
                    int amount = Math.min(is.getMaxStackSize(), is.getAmount() + currItem.getAmount());
                    currItem.setAmount(currItem.getAmount() - (amount - is.getAmount()));
                    is.setAmount(amount);

                    updateSlotWithHandler(slot, is, is);

                    if (currItem.getAmount() <= 0) {
                        event.getClickedInventory().setItem(event.getSlot(), null);
                        return;
                    }
                }
            }
        }
    }

    private void moveToPlayerInventory(InventoryClickEvent event) {
        event.setCancelled(true);

        PlayerInventory playerInventory = viewer.getBukkitPlayer().getInventory();
        ItemStack currItem = event.getCurrentItem();
        String    currId   = TokenUtils.getTokenID(currItem);

        if (StringUtils.isEmpty(currId))
            return;

        for (int i = 0; i < playerInventory.getStorageContents().length; i++) {
            ItemStack is = playerInventory.getItem(i);
            String    id = TokenUtils.getTokenID(is);

            if (id == null) {
                // Transfers the whole stack
                event.getClickedInventory().setItem(event.getSlot(), null);
                updateSlotWithHandler(event.getSlot(), currItem, null);
                playerInventory.setItem(i, currItem);
                return;
            } else if (id.equals(currId)) {
                // Combines what is possible with the other stack
                int amount = Math.min(is.getMaxStackSize(), is.getAmount() + currItem.getAmount());
                currItem.setAmount(currItem.getAmount() - (amount - is.getAmount()));
                is.setAmount(amount);

                if (currItem.getAmount() > 0) {
                    updateSlotWithHandler(event.getSlot(), currItem, currItem);
                } else {
                    event.getClickedInventory().setItem(event.getSlot(), null);
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
        if (player != this.viewer.getBukkitPlayer())
            return;

        this.viewer.setActiveTradeView(null);

        TradeView otherTradeView = this.other.getActiveTradeView();
        if (otherTradeView != null) {
            otherTradeView.removePlayer(this.other.getBukkitPlayer());
            otherTradeView.destroy();
        }

        if (!tradeApproved && otherTradeView == null)
            informViewerOfCancellation();

        returnItems(player);
        destroy();
    }

    private void returnItems(Player player) {
        Inventory playerInventory = player.getInventory();
        Inventory inventory       = getInventory(player, false);
        if (inventory != null) {
            List<ItemStack> items = new ArrayList<>();

            for (int y = 0; y < this.viewerItemsComponent.getDimension().getHeight(); y++) {
                for (int x = 0; x < this.viewerItemsComponent.getDimension().getWidth(); x++) {
                    ItemStack item = inventory.getItem(x + (y * getDimension().getWidth()));
                    String    id   = TokenUtils.getTokenID(item);
                    if (!StringUtils.isEmpty(id))
                        items.add(item);
                }
            }

            Map<Integer, ItemStack> leftOver = playerInventory.addItem(items.toArray(new ItemStack[] {}));
            if (leftOver.size() > 0) {
                TokenWallet tokenWallet = viewer.getTokenWallet();
                for (Map.Entry<Integer, ItemStack> entry : leftOver.entrySet()) {
                    ItemStack is = entry.getValue();
                    MutableBalance balance = tokenWallet.getBalance(TokenUtils.getTokenID(is));
                    balance.deposit(is.getAmount());
                }
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
