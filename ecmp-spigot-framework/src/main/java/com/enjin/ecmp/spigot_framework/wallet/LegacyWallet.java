package com.enjin.ecmp.spigot_framework.wallet;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.enjincoin.sdk.model.service.tokens.Token;
import org.bukkit.inventory.Inventory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LegacyWallet {

    private BasePlugin plugin;

    private Map<String, MutableBalance> tokenBalances = new ConcurrentHashMap<>();
    private Inventory inventory = null;
    private static WalletCheckoutManager manager;

    public LegacyWallet(BasePlugin plugin, UUID uuid) {
        this.plugin = plugin;
        initializeCheckoutManager(uuid);
    }

    public Map<String, MutableBalance> getTokenBalances() {
        return new HashMap<>(this.tokenBalances);
    }

    public MutableBalance addToken(Token token) {
//        return addToken(token.getTokenId(), new MutableBalance(token));
        return null;
    }

    public MutableBalance addToken(String id, MutableBalance data) {
        // sync with checkout manager
        if (manager.accessCheckout().containsKey(id)) {
//                data.setCheckedOut(manager.accessCheckout().get(id).getAmount());
        }
        return this.tokenBalances.put(id, data);
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public MutableBalance removeToken(String id) {
        return this.tokenBalances.remove(id);
    }

    public MutableBalance getToken(String id) {
        return this.tokenBalances.get(id);
    }

    public List<MutableBalance> getTokens() {
        return new ArrayList<>(tokenBalances.values());
    }

    public boolean isEmpty() {
        return this.tokenBalances.isEmpty();
    }

    public void populate(List<Token> tokens) {
        this.tokenBalances.clear();
        if (tokens != null) {
            tokens.forEach(this::addToken);
        }
    }

    protected void initializeCheckoutManager(UUID playerId) {
        this.manager = new WalletCheckoutManager(playerId);
    }


    public WalletCheckoutManager getCheckoutManager() {
        if (manager != null)
            return manager;

        return null;
    }
}
