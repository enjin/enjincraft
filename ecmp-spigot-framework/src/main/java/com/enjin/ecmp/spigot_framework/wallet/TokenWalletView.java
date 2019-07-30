package com.enjin.ecmp.spigot_framework.wallet;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.ecmp.spigot_framework.TokenDefinition;
import com.enjin.ecmp.spigot_framework.player.EnjinCoinPlayer;
import com.enjin.minecraft_commons.spigot.ui.Dimension;
import com.enjin.minecraft_commons.spigot.ui.Position;
import com.enjin.minecraft_commons.spigot.ui.menu.ChestMenu;
import com.enjin.minecraft_commons.spigot.ui.menu.component.SimpleMenuComponent;

import java.util.List;

public class TokenWalletView extends ChestMenu {

    private BasePlugin plugin;
    private EnjinCoinPlayer owner;

    public TokenWalletView(BasePlugin plugin, EnjinCoinPlayer owner) {
        super("Wallet", 6);
        this.plugin = plugin;
        this.owner = owner;
        init();
    }

    private void init() {
        SimpleMenuComponent container = new SimpleMenuComponent(new Dimension(9, 6));

        List<MutableBalance> balances = owner.getTokenWallet().getBalances();

        int index = 0;
        for (MutableBalance balance : balances) {
            if (index == container.size()) break;
            if (balance.amountAvailableForWithdrawal() == 0) continue;

            TokenDefinition def = plugin.getBootstrap().getConfig().getTokens().get(balance.id());
            if (def == null) continue;
            Position position = Position.toPosition(container, index);
            container.setItem(position, def.getItemStackInstance());

            addComponent(Position.of(0, 0), container);

            index++;
        }
    }

}
