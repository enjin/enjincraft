package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.client.service.tokens.vo.Token;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Wallet {

    private Map<String, TokenData> tokenBalances = new ConcurrentHashMap<>();

    private Inventory inventory = null;

    public Map<String, TokenData> getTokenBalances() {
        return new HashMap<>(this.tokenBalances);
    }

    public TokenData addToken(Token token) {
        return addToken(token.getTokenId(), new TokenData(token));
    }

    public TokenData addToken(String id, TokenData data) {
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
}
