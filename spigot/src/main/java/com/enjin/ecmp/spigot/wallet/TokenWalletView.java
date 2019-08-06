package com.enjin.ecmp.spigot.wallet;

import com.enjin.ecmp.spigot.EcmpPlugin;
import com.enjin.ecmp.spigot.EcmpSpigot;
import com.enjin.ecmp.spigot.configuration.TokenDefinition;
import com.enjin.ecmp.spigot.player.EnjinCoinPlayer;
import com.enjin.ecmp.spigot.util.TokenUtils;
import com.enjin.java_commons.StringUtils;
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

public class TokenWalletView extends ChestMenu {

    public static final String WALLET_VIEW_NAME = "Enjin Wallet";

    private EcmpPlugin plugin;
    private EnjinCoinPlayer owner;
    private SimpleMenuComponent component;

    public TokenWalletView(EcmpPlugin plugin, EnjinCoinPlayer owner) {
        super(ChatColor.DARK_PURPLE + WALLET_VIEW_NAME, 6);
        this.plugin = plugin;
        this.owner = owner;
        this.component = new SimpleMenuComponent(new Dimension(9, 6));
        init();
    }

    private void init() {
        populate();
    }



    private void populate() {
        List<MutableBalance> balances = owner.getTokenWallet().getBalances();

        int index = 0;
        for (MutableBalance balance : balances) {
            if (index == component.size()) break;
            if (balance.amountAvailableForWithdrawal() == 0) continue;

            TokenDefinition def = EcmpSpigot.bootstrap().getConfig().getTokens().get(balance.id());
            if (def == null) continue;
            ItemStack is = def.getItemStackInstance();
            is.setAmount(balance.amountAvailableForWithdrawal());
            component.setItem(index % getDimension().getWidth(), index / getDimension().getWidth(), is);

            addComponent(Position.of(0, 0), component);
            component.addAction(is, player -> {
                if (balance.amountAvailableForWithdrawal() > 0) {
                    balance.withdraw(1);
                    ItemStack clone = is.clone();
                    clone.setAmount(1);
                    player.getInventory().addItem(clone);
                    repopulate(player);
                }
            }, ClickType.LEFT);

            index++;
        }
    }

    public void repopulate(Player player) {
        for (int y = 0; y < component.getDimension().getHeight(); y++) {
            for (int x = 0; x < component.getDimension().getWidth(); x++) {
                component.removeItem(x, y);
            }
        }

        populate();

        refresh(player);
    }

    public static boolean isViewingWallet(Player player) {
        if (player != null) {
            InventoryView view = player.getOpenInventory();

            if (view != null) {
                return ChatColor.stripColor(view.getTitle()).equalsIgnoreCase(TokenWalletView.WALLET_VIEW_NAME);
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTokenCheckout(InventoryClickEvent event) {
        if (event.getClickedInventory() instanceof PlayerInventory) {
            ItemStack current = event.getCurrentItem();
            if (current != null) {
                String id = TokenUtils.getTokenID(current);
                if (!StringUtils.isEmpty(id)) {
                    EnjinCoinPlayer player = EcmpSpigot.bootstrap().getPlayerManager()
                            .getPlayer(event.getWhoClicked().getUniqueId());
                    MutableBalance balance = player.getTokenWallet().getBalance(id);
                    balance.deposit(current.getAmount());
                    current.setAmount(0);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> repopulate((Player) event.getWhoClicked()));
                }
            }
        }
    }

    @Override
    protected void onClose(Player player) {
        HandlerList.unregisterAll(this);
        super.onClose(player);
    }
}
