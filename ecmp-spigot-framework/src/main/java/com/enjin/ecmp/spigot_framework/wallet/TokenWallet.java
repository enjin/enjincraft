package com.enjin.ecmp.spigot_framework.wallet;

import com.enjin.ecmp.spigot_framework.SpigotBootstrap;
import com.enjin.ecmp.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.sdk.model.service.balances.Balance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenWallet {

    private SpigotBootstrap bootstrap;
    private MinecraftPlayer player;
    private Map<String, MutableBalance> balances;

    public TokenWallet(List<Balance> balances) {
        this.balances = new HashMap<>();
        balances.forEach(balance -> this.balances.put(balance.getTokenId(), new MutableBalance(balance)));
    }

}
