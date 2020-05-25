package com.enjin.enjincraft.spigot.wallet;

import com.enjin.enjincraft.spigot.EnjTokenView;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.util.UiUtils;
import com.enjin.minecraft_commons.spigot.ui.AbstractMenu;
import com.enjin.minecraft_commons.spigot.ui.Component;
import com.enjin.minecraft_commons.spigot.ui.Dimension;
import com.enjin.minecraft_commons.spigot.ui.Position;
import com.enjin.minecraft_commons.spigot.ui.menu.ChestMenu;
import com.enjin.minecraft_commons.spigot.ui.menu.component.SimpleMenuComponent;
import com.enjin.minecraft_commons.spigot.ui.menu.component.pagination.SimplePagedComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

public class TokenWalletView extends ChestMenu implements EnjTokenView {

    public static final String WALLET_VIEW_NAME = "Enjin Wallet";
    public static final int WIDTH = 9;
    public static final Dimension INVENTORY_DIMENSION = new Dimension(WIDTH, 4);

    private SpigotBootstrap bootstrap;
    private EnjPlayer owner;
    private SimpleMenuComponent navigationComponent;
    private SimpleMenuComponent inventoryViewComponent;
    private SimplePagedComponent inventoryPagedComponent;
    int currentPage;

    public TokenWalletView(SpigotBootstrap bootstrap, EnjPlayer owner) {
        super(ChatColor.DARK_PURPLE + WALLET_VIEW_NAME, 6);
        this.bootstrap = bootstrap;
        this.owner = owner;
        this.navigationComponent = new SimpleMenuComponent(new Dimension(WIDTH, 1));
        this.inventoryViewComponent = new SimpleMenuComponent(INVENTORY_DIMENSION);
        this.inventoryPagedComponent = new SimplePagedComponent(INVENTORY_DIMENSION);
        this.currentPage = 0;
        init();
    }

    @Override
    public void validateInventory() {
        repopulate(owner.getBukkitPlayer());
    }

    private void init() {
        owner.setActiveWalletView(this);
        setCloseConsumer(this::closeMenuAction);

        Position pageBackPosition = Position.of(0, 0);
        navigationComponent.setItem(pageBackPosition, createPageBackItemStack());
        navigationComponent.addAction(pageBackPosition, p -> {
            if (currentPage < 0) {
                currentPage = 0;
                return;
            } else if (currentPage == 0) {
                return;
            }

            currentPage--;
            drawInventory();
            refresh(p);
        }, ClickType.LEFT, ClickType.RIGHT);

        Position pageNextPosition = Position.of(WIDTH - 1, 0);
        navigationComponent.setItem(pageNextPosition, createPageNextItemStack());
        navigationComponent.addAction(pageNextPosition, p -> {
            if (currentPage >= inventoryPagedComponent.getPageCount() - 1)
                return;

            currentPage++;
            drawInventory();
            refresh(p);
        }, ClickType.LEFT, ClickType.RIGHT);

        // Creates the navigation separator
        Component separator = UiUtils.createSeparator(new Dimension(WIDTH, 1));

        addComponent(Position.of(0, 0), navigationComponent);
        addComponent(Position.of(0, 1), separator);

        populate();
    }

    private void populate() {
        List<MutableBalance> balances = owner.getTokenWallet().getBalances();

        inventoryPagedComponent.clear();

        int index = 0;
        for (MutableBalance balance : balances) {
            if (balance.amountAvailableForWithdrawal() == 0)
                continue;

            TokenModel model = bootstrap.getTokenManager().getToken(balance.id());
            if (model == null)
                continue;

            int page = index / INVENTORY_DIMENSION.getArea();
            int x = index % INVENTORY_DIMENSION.getWidth();
            int y = index % INVENTORY_DIMENSION.getArea() / INVENTORY_DIMENSION.getWidth();
            Position position = Position.of(x, y);

            ItemStack is = model.getItemStack();
            is.setAmount(balance.amountAvailableForWithdrawal());
            inventoryPagedComponent.setItem(page, position, is);

            index++;
        }

        // Puts the view at the nearest empty page if multiple pages were lost when repopulating
        if (currentPage > inventoryPagedComponent.getPageCount())
            currentPage = inventoryPagedComponent.getPageCount();

        drawInventory();
    }

    protected void drawInventory() {
        inventoryViewComponent.removeAllActions();

        for (int y = 0; y < INVENTORY_DIMENSION.getHeight(); y++) {
            for (int x = 0; x < INVENTORY_DIMENSION.getWidth(); x++) {
                ItemStack is = inventoryPagedComponent.getItem(currentPage, x, y);
                String    id = TokenUtils.getTokenID(is);
                if (StringUtils.isEmpty(id)) {
                    inventoryViewComponent.removeItem(x, y);
                    continue;
                }

                MutableBalance balance = owner.getTokenWallet().getBalance(id);
                inventoryViewComponent.setItem(x, y, is);
                addWithdrawAction(Position.of(x, y), balance, is);
            }
        }

        addComponent(Position.of(0, 2), inventoryViewComponent);
    }

    protected void addWithdrawAction(Position position, MutableBalance balance, ItemStack is) {
        inventoryViewComponent.addAction(position, player -> {
            PlayerInventory inventory = player.getInventory();

            if (balance.amountAvailableForWithdrawal() > 0 && slotAvailable(inventory, balance.id())) {
                balance.withdraw(1);
                ItemStack clone = is.clone();
                clone.setAmount(1);
                inventory.addItem(clone);
                repopulate(player);
            }
        }, ClickType.LEFT);
    }

    private boolean slotAvailable(PlayerInventory inventory, String tokenId) {
        boolean slotAvailable = false;
        int capacity = inventory.getSize() - (inventory.getArmorContents().length + inventory.getExtraContents().length);

        for (int i = 0; i < capacity && !slotAvailable; i++) {
            ItemStack content   = inventory.getItem(i);
            String    contentId = TokenUtils.getTokenID(content);

            if (contentId == null) {
                slotAvailable = true;
                continue;
            } else if (StringUtils.isEmpty(contentId)) {
                continue;
            }

            if (contentId.equals(tokenId) && content.getAmount() < content.getMaxStackSize())
                slotAvailable = true;
        }

        return slotAvailable;
    }

    public void repopulate(Player player) {
        for (int y = 0; y < INVENTORY_DIMENSION.getHeight(); y++) {
            for (int x = 0; x < INVENTORY_DIMENSION.getWidth(); x++) {
                inventoryViewComponent.removeItem(x, y);
            }
        }

        populate();

        refresh(player);
    }

    public static boolean isViewingWallet(Player player) {
        if (player != null) {
            InventoryView view = player.getOpenInventory();
            return ChatColor.stripColor(view.getTitle()).equalsIgnoreCase(TokenWalletView.WALLET_VIEW_NAME);
        }

        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTokenDeposit(InventoryClickEvent event) {
        if (event.getClickedInventory() instanceof PlayerInventory) {
            ItemStack current = event.getCurrentItem();
            String    id      = TokenUtils.getTokenID(current);
            if (!StringUtils.isEmpty(id)) {
                Optional<EnjPlayer> optionalPlayer = bootstrap.getPlayerManager().getPlayer((Player) event.getWhoClicked());
                if (!optionalPlayer.isPresent())
                    return;
                EnjPlayer player = optionalPlayer.get();
                MutableBalance balance = player.getTokenWallet().getBalance(id);
                balance.deposit(current.getAmount());
                current.setAmount(0);
                Bukkit.getScheduler().scheduleSyncDelayedTask(bootstrap.plugin(), () -> repopulate((Player) event.getWhoClicked()));
            }
        }
    }

    @Override
    protected void onClose(Player player) {
        HandlerList.unregisterAll(this);
        super.onClose(player);
    }

    private void closeMenuAction(Player player, AbstractMenu menu) {
        if (player != owner.getBukkitPlayer() || this != owner.getActiveWalletView())
            return;

        owner.setActiveWalletView(null);
    }

    protected ItemStack createPageBackItemStack() {
        ItemStack stack = new ItemStack(Material.HOPPER);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "<--");
        stack.setItemMeta(meta);
        return stack;
    }

    protected ItemStack createPageNextItemStack() {
        ItemStack stack = new ItemStack(Material.HOPPER);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "-->");
        stack.setItemMeta(meta);
        return stack;
    }

}
