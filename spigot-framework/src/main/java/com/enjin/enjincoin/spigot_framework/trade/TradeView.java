package com.enjin.enjincoin.spigot_framework.trade;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.enjin.minecraft_commons.spigot.ui.Component;
import com.enjin.minecraft_commons.spigot.ui.Dimension;
import com.enjin.minecraft_commons.spigot.ui.Position;
import com.enjin.minecraft_commons.spigot.ui.menu.ChestMenu;
import com.enjin.minecraft_commons.spigot.ui.menu.component.SimpleMenuComponent;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradeView extends ChestMenu {

    private BasePlugin plugin;

    private MinecraftPlayer viewer;
    private MinecraftPlayer other;

    private SimpleMenuComponent viewerItemsComponent;
    private SimpleMenuComponent viewerStatusComponent;

    private SimpleMenuComponent otherItemsComponent;
    private SimpleMenuComponent otherStatusComponent;

    private boolean playerReady = false;
    private boolean tradeApproved = false;
    private ItemStack readyPane = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
    private ItemStack unreadyPane = new ItemStack(Material.RED_STAINED_GLASS_PANE);

    public TradeView(BasePlugin plugin, MinecraftPlayer viewer, MinecraftPlayer other) {
        super("Trade", 6);
        this.plugin = plugin;
        this.viewer = viewer;
        this.other = other;
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
                        MessageUtils.sendMessage(viewer.getBukkitPlayer(), TextComponent.builder("")
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

        this.viewerItemsComponent = new SimpleMenuComponent(new Dimension(4, 4));
        this.viewerItemsComponent.setAllowPlace(true);
        this.viewerItemsComponent.setAllowDrag(true);
        this.viewerItemsComponent.setAllowPickup(true);
        this.viewerItemsComponent.setSlotUpdateHandler((player, slot, oldItem, newItem) -> {
            TradeView otherView = this.other.getActiveTradeView();
            Position position = Position.toPosition(this, slot);
            otherView.setItem(this.other.getBukkitPlayer(), otherView.getOtherItemsComponent(), position, newItem);
        });

        this.viewerStatusComponent = new SimpleMenuComponent(new Dimension(4, 1));
        this.viewerStatusComponent.setItem(Position.of(0, 0), getPlayerHead(viewer.getBukkitPlayer(), true));
        ItemStack readyItem = new ItemStack(Material.HOPPER);
        this.viewerStatusComponent.setItem(Position.of(1, 0), readyItem);
        this.viewerStatusComponent.addAction(readyItem, (p) -> {
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

                    UUID viewerUuid = viewer.getBukkitPlayer().getUniqueId();
                    UUID otherUuid = other.getBukkitPlayer().getUniqueId();
                    Trade trade = new Trade(viewerUuid, viewerOffer, otherUuid, otherOffer);

                    // TODO: Complete Trade Request
                     this.plugin.getBootstrap().getTradeManager().submitCreateTrade(trade);

                    // TODO: To be removed
//                    viewer.getBukkitPlayer().getInventory().addItem(trade.getPlayerTwoOffer().toArray(new ItemStack[0]));
//                    other.getBukkitPlayer().getInventory().addItem(trade.getPlayerOneOffer().toArray(new ItemStack[0]));

                    closeMenu(p);
                }
            }
        }, ClickType.LEFT, ClickType.RIGHT);
        ItemStack unreadyItem = new ItemStack(Material.BARRIER);
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

        this.otherItemsComponent = new SimpleMenuComponent(new Dimension(4, 4));

        this.otherStatusComponent = new SimpleMenuComponent(new Dimension(4, 1));
        this.otherStatusComponent.setItem(Position.of(0, 0), getPlayerHead(other.getBukkitPlayer(), false));
        this.otherStatusComponent.setItem(Position.of(3, 0), unreadyPane);

        Component horizontalBarrier = new SimpleMenuComponent(new Dimension(9, 1));
        for (int i = 0; i < horizontalBarrier.getDimension().getWidth(); i++) {
            ((SimpleMenuComponent) horizontalBarrier).setItem(Position.of(i, 0), new ItemStack(Material.IRON_BARS));
        }

        Component verticalBarrierTop = new SimpleMenuComponent(new Dimension(1, 4));
        for (int i = 0; i < verticalBarrierTop.getDimension().getHeight(); i++) {
            ((SimpleMenuComponent) verticalBarrierTop).setItem(Position.of(0, i), new ItemStack(Material.IRON_BARS));
        }

        Component verticalBarrierBottom = new SimpleMenuComponent(new Dimension(1, 1));
        ((SimpleMenuComponent) verticalBarrierBottom).setItem(Position.of(0, 0), new ItemStack(Material.IRON_BARS));

        addComponent(Position.of(0, 0), this.viewerItemsComponent);
        addComponent(Position.of(0, 5), this.viewerStatusComponent);
        addComponent(Position.of(5, 0), this.otherItemsComponent);
        addComponent(Position.of(5, 5), this.otherStatusComponent);
        addComponent(Position.of(4, 0), verticalBarrierTop);
        addComponent(Position.of(4, 5), verticalBarrierBottom);
        addComponent(Position.of(0, 4), horizontalBarrier);

        open(this.viewer.getBukkitPlayer());
    }

    public MinecraftPlayer getViewer() {
        return viewer;
    }

    public MinecraftPlayer getOther() {
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

    private ItemStack getPlayerHead(Player player, boolean self) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(self ? "You" : player.getName());
        stack.setItemMeta(meta);
        return stack;
    }
}
