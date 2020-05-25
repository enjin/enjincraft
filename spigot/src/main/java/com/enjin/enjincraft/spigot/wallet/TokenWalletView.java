package com.enjin.enjincraft.spigot.wallet;

import com.enjin.enjincraft.spigot.EnjTokenView;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.minecraft_commons.spigot.ui.AbstractMenu;
import com.enjin.minecraft_commons.spigot.ui.Dimension;
import com.enjin.minecraft_commons.spigot.ui.Position;
import com.enjin.minecraft_commons.spigot.ui.menu.ChestMenu;
import com.enjin.minecraft_commons.spigot.ui.menu.component.SimpleMenuComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Optional;

public class TokenWalletView extends ChestMenu implements EnjTokenView {

    public static final String WALLET_VIEW_NAME = "Enjin Wallet";

    private SpigotBootstrap bootstrap;
    private EnjPlayer owner;
    private SimpleMenuComponent component;

    public TokenWalletView(SpigotBootstrap bootstrap, EnjPlayer owner) {
        super(ChatColor.DARK_PURPLE + WALLET_VIEW_NAME, 6);
        this.bootstrap = bootstrap;
        this.owner = owner;
        this.component = new SimpleMenuComponent(new Dimension(9, 6));
        init();
    }

    @Override
    public void validateInventory() {
        repopulate(owner.getBukkitPlayer());
    }

    private void init() {
        owner.setActiveWalletView(this);
        setCloseConsumer(this::closeMenuAction);
        populate();
    }

    private void populate() {
        List<MutableBalance> balances = owner.getTokenWallet().getBalances();

        component.removeAllActions();

        int index = 0;
        for (MutableBalance balance : balances) {
            if (index == component.size())
                break;
            if (balance.amountAvailableForWithdrawal() == 0)
                continue;

            TokenModel model = bootstrap.getTokenManager().getToken(balance.id());
            if (model == null)
                continue;

            Position position = Position.of(index % getDimension().getWidth(), index / getDimension().getWidth());
            ItemStack is = model.getItemStack();
            is.setAmount(balance.amountAvailableForWithdrawal());
            component.setItem(position, is);

            component.addAction(position, player -> {
                PlayerInventory inventory = player.getInventory();

                if (balance.amountAvailableForWithdrawal() > 0 && slotAvailable(inventory, balance.id())) {
                    balance.withdraw(1);
                    ItemStack clone = is.clone();
                    clone.setAmount(1);
                    inventory.addItem(clone);
                    repopulate(player);
                }
            }, ClickType.LEFT);

            index++;
        }

        addComponent(Position.of(0, 0), component);
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
        for (int y = 0; y < component.getDimension().getHeight(); y++) {
            for (int x = 0; x < component.getDimension().getWidth(); x++)
                component.removeItem(x, y);
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
}
