package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.client.service.tokens.vo.Token;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.inventory.WalletCheckoutManager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Wallet {

    private Map<String, TokenData> tokenBalances = new ConcurrentHashMap<>();

    private Inventory inventory = null;

    private static WalletCheckoutManager manager;

    public Wallet(UUID uuid) {
        initializeCheckoutManager(uuid);
    }

    public Map<String, TokenData> getTokenBalances() {
        return new HashMap<>(this.tokenBalances);
    }

    public TokenData addToken(Token token) {
        return addToken(token.getTokenId(), new TokenData(token));
    }

    public TokenData addToken(String id, TokenData data) {
        // sync with checkout manager
        if (manager.accessCheckout().containsKey(id)) {
            data.setCheckedOut(manager.accessCheckout().get(id).getAmount());
        }
        return this.tokenBalances.put(id, data);
    }

    public Inventory getInventory() { return this.inventory; }

    public void setInventory(Inventory inventory ) { this.inventory = inventory; }

    public TokenData removeToken(String id) {
        return this.tokenBalances.remove(id);
    }

    public TokenData getToken(String id) {
        return this.tokenBalances.get(id);
    }

    public List<TokenData> getTokens() {
        return new ArrayList<>(tokenBalances.values());
    }

    public boolean isEmpty() {
        return this.tokenBalances.isEmpty();
    }

    protected void populate(List<Token> tokens) {
        this.tokenBalances.clear();
        if (tokens != null) {
            tokens.forEach(this::addToken);
        }
    }

    protected void initializeCheckoutManager(UUID playerId) {
        this.manager = new WalletCheckoutManager(playerId);
    }



    public WalletCheckoutManager accessCheckoutManager() {
        if (manager != null)
            return manager;

        return null;
    }
}