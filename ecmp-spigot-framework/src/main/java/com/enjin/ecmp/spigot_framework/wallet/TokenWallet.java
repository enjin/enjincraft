package com.enjin.ecmp.spigot_framework.wallet;

import com.enjin.ecmp.spigot_framework.SpigotBootstrap;
import com.enjin.enjincoin.sdk.model.service.balances.Balance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenWallet {

    private Map<String, MutableBalance> balances;

    public TokenWallet(SpigotBootstrap bootstrap, List<Balance> balances) {
        this.balances = new ConcurrentHashMap<>();
        balances.forEach(balance -> {
            if (bootstrap.getConfig().getTokens().containsKey(balance.getTokenId()))
                this.balances.put(balance.getTokenId(), new MutableBalance(balance));
        });
    }

    public Map<String, MutableBalance> getBalances() {
        return balances;
    }
}
