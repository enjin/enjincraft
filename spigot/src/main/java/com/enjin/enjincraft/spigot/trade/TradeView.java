package com.enjin.enjincraft.spigot.trade;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.TargetPlayer;
import com.enjin.enjincraft.spigot.enums.Trader;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.MessageUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.java_commons.StringUtils;
import com.enjin.minecraft_commons.spigot.ui.Component;
import com.enjin.minecraft_commons.spigot.ui.Dimension;
import com.enjin.minecraft_commons.spigot.ui.Position;
import com.enjin.minecraft_commons.spigot.ui.menu.ChestMenu;
import com.enjin.minecraft_commons.spigot.ui.menu.component.SimpleMenuComponent;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class TradeView extends ChestMenu {

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
        setCloseConsumer((player, menu) -> {
            if (player == this.viewer.getBukkitPlayer()) {
                this.viewer.setActiveTradeView(null);

                TradeView otherTradeView = this.other.getActiveTradeView();
                if (otherTradeView != null) {
                    otherTradeView.removePlayer(this.other.getBukkitPlayer());
                    otherTradeView.destroy();
                }

                if (!tradeApproved) {
                    Inventory playerInventory = player.getInventory();
                    Inventory inventory = getInventory(player, false);
                    if (inventory != null) {
                        for (int y = 0; y < this.viewerItemsComponent.getDimension().getHeight(); y++) {
                            for (int x = 0; x < this.viewerItemsComponent.getDimension().getWidth(); x++) {
                                ItemStack item = inventory.getItem(x + (y * getDimension().getWidth()));
                                if (item != null && item.getType() != Material.AIR) {
                                    playerInventory.addItem(item);
                                }
                            }
                        }
                    }

                    if (otherTradeView == null) {
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

                destroy();
            }
        });

        //  Create the offering region for the viewing player
        this.viewerItemsComponent = new SimpleMenuComponent(new Dimension(4, 4));
        this.viewerItemsComponent.setAllowPlace(true);
        this.viewerItemsComponent.setAllowDrag(true);
        this.viewerItemsComponent.setAllowPickup(true);
        this.viewerItemsComponent.setSlotUpdateHandler((player, slot, oldItem, newItem) -> {
            TradeView otherView = this.other.getActiveTradeView();
            Position position = Position.toPosition(this, slot);
            otherView.setItem(this.other.getBukkitPlayer(), otherView.getOtherItemsComponent(), position, newItem);
        });

        //  Create the status region for the viewing player
        this.viewerStatusComponent = new SimpleMenuComponent(new Dimension(4, 1));
        this.viewerStatusComponent.setItem(Position.of(0, 0), getPlayerHead(viewer.getBukkitPlayer(), TargetPlayer.SELF));

        this.viewerStatusComponent.setItem(Position.of(1, 0), readyItem);
        this.viewerStatusComponent.addAction(readyItem, (p) -> {
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
                    if (viewerOffer.size() > 0 || otherOffer.size() > 0) {
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
        this.viewerStatusComponent.setItem(Position.of(2, 0), unreadyItem);
        this.viewerStatusComponent.addAction(unreadyItem, (p) -> {
            this.playerReady = false;
            setItem(p, this.viewerStatusComponent, Position.of(3, 0), unreadyPane);
            p.updateInventory();

            TradeView otherView = this.other.getActiveTradeView();
            otherView.setItem(other.getBukkitPlayer(), otherView.otherStatusComponent, Position.of(3, 0), unreadyPane);
            other.getBukkitPlayer().updateInventory();
        }, ClickType.LEFT, ClickType.RIGHT);
        this.viewerStatusComponent.setItem(Position.of(3, 0), unreadyPane);

        // Create the offering region for the other player
        this.otherItemsComponent = new SimpleMenuComponent(new Dimension(4, 4));

        // Create the status region for the other player
        this.otherStatusComponent = new SimpleMenuComponent(new Dimension(4, 1));
        this.otherStatusComponent.setItem(Position.of(0, 0), getPlayerHead(other.getBukkitPlayer(), TargetPlayer.OTHER));
        this.otherStatusComponent.setItem(Position.of(3, 0), unreadyPane);

        // Place the horizontal separator
        Component horizontalBarrier = new SimpleMenuComponent(new Dimension(9, 1));
        for (int i = 0; i < horizontalBarrier.getDimension().getWidth(); i++) {
            ((SimpleMenuComponent) horizontalBarrier).setItem(Position.of(i, 0), createSeparatorItemStack());
        }

        // Place the upper vertical separator
        Component verticalBarrierTop = new SimpleMenuComponent(new Dimension(1, 4));
        for (int i = 0; i < verticalBarrierTop.getDimension().getHeight(); i++) {
            ((SimpleMenuComponent) verticalBarrierTop).setItem(Position.of(0, i), createSeparatorItemStack());
        }

        // Place the lower vertical separator
        Component verticalBarrierBottom = new SimpleMenuComponent(new Dimension(1, 1));
        ((SimpleMenuComponent) verticalBarrierBottom).setItem(Position.of(0, 0), createSeparatorItemStack());

        addComponent(Position.of(0, 0), this.viewerItemsComponent);
        addComponent(Position.of(0, 5), this.viewerStatusComponent);
        addComponent(Position.of(5, 0), this.otherItemsComponent);
        addComponent(Position.of(5, 5), this.otherStatusComponent);
        addComponent(Position.of(4, 0), verticalBarrierTop);
        addComponent(Position.of(4, 5), verticalBarrierBottom);
        addComponent(Position.of(0, 4), horizontalBarrier);
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
                ItemStack item = view.getItem(x + (y * 9));
                if (item != null && item.getType() != Material.AIR) {
                    items.add(item);
                }
            }
        }

        return items;
    }

    public void open() {
        open(this.viewer.getBukkitPlayer());
    }

    private ItemStack getPlayerHead(Player player, TargetPlayer target) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(target == TargetPlayer.SELF ? "You" : player.getName());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createSeparatorItemStack() {
        ItemStack stack = new ItemStack(Material.IRON_BARS);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "|");
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() instanceof PlayerInventory) {
            int slot = event.getSlot();

            ItemStack is = event.getCurrentItem();

            if (is == null)
                return;

            String id = TokenUtils.getTokenID(is);
            if (StringUtils.isEmpty(id)) {
                event.setResult(Event.Result.DENY);
            }
        } else {
            bootstrap.debug("Menu Clicked");
            super.onInventoryClick(event);
        }
    }

    @Override
    protected void onClose(Player player) {
        HandlerList.unregisterAll(this);
        super.onClose(player);
    }
}
