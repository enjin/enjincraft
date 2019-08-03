package com.enjin.ecmp.spigot_framework.wallet;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.ecmp.spigot_framework.TokenDefinition;
import com.enjin.ecmp.spigot_framework.player.EnjinCoinPlayer;
import com.enjin.minecraft_commons.spigot.ui.Dimension;
import com.enjin.minecraft_commons.spigot.ui.Position;
import com.enjin.minecraft_commons.spigot.ui.menu.ChestMenu;
import com.enjin.minecraft_commons.spigot.ui.menu.component.SimpleMenuComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TokenWalletView extends ChestMenu {

    public static final String WALLET_VIEW_NAME = "Enjin Wallet";

    private BasePlugin plugin;
    private EnjinCoinPlayer owner;
    private SimpleMenuComponent component;

    public TokenWalletView(BasePlugin plugin, EnjinCoinPlayer owner) {
        super(ChatColor.DARK_PURPLE + WALLET_VIEW_NAME, 6);
        this.plugin = plugin;
        this.owner = owner;
        this.component = new SimpleMenuComponent(new Dimension(9, 6));
        init();
    }

    private void init() {
        List<MutableBalance> balances = owner.getTokenWallet().getBalances();

        int index = 0;
        for (MutableBalance balance : balances) {
            if (index == component.size()) break;
            if (balance.amountAvailableForWithdrawal() == 0) continue;

            TokenDefinition def = plugin.getBootstrap().getConfig().getTokens().get(balance.id());
            if (def == null) continue;
            ItemStack is = def.getItemStackInstance();
            component.setItem(index % getDimension().getWidth(), index / getDimension().getWidth(), is);

            addComponent(Position.of(0, 0), component);
            component.addAction(is, player -> {
                if (balance.amountAvailableForWithdrawal() > 0) {
                    balance.withdraw(1);
                    player.getInventory().addItem(is.clone());
                    reinit(player);
                }
            }, ClickType.LEFT);

            index++;
        }
    }

    public void reinit(Player player) {
        for (int y = 0; y < component.getDimension().getHeight(); y++) {
            for (int x = 0; x < component.getDimension().getWidth(); x++) {
                component.removeItem(x, y);
            }
        }

        init();

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

}
