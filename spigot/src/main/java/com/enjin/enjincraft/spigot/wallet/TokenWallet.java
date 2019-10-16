package com.enjin.enjincraft.spigot.wallet;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.sdk.model.service.balances.Balance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenWallet {

    private Map<String, MutableBalance> balances;

    public TokenWallet(SpigotBootstrap bootstrap, List<Balance> balances) {
        this.balances = new ConcurrentHashMap<>();
        balances.forEach(balance -> {
            if (bootstrap.getTokenConf().getTokens().containsKey(balance.getTokenId()))
                this.balances.put(balance.getTokenId(), new MutableBalance(balance));
        });
    }

    public MutableBalance removeBalance(String id) {
        return balances.remove(id);
    }

    public MutableBalance getBalance(String id) {
        return balances.get(id);
    }

    public List<MutableBalance> getBalances() {
        return new ArrayList<>(balances.values());
    }

    public void setBalance(MutableBalance balance) {
        if (balance == null)
            return;
        balances.put(balance.id(), balance);
    }

    public Map<String, MutableBalance> getBalancesMap() {
        return new HashMap<>(balances);
    }

    public boolean isEmpty() {
        return balances.isEmpty();
    }
}
