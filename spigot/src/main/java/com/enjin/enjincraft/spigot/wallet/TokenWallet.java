package com.enjin.enjincraft.spigot.wallet;

import com.enjin.enjincraft.spigot.EnjinCraft;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.sdk.models.balance.Balance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TokenWallet {

    private final Map<String, MutableBalance> balances;

    public TokenWallet(List<Balance> balances) {
        this.balances = new ConcurrentHashMap<>();
        balances.forEach(balance -> setBalance(new MutableBalance(balance)));
    }

    public MutableBalance removeBalance(String id) {
        try {
            return balances.remove(TokenUtils.toFullId(id));
        } catch (IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            log(e);
            return null;
        }
    }

    public MutableBalance removeBalance(String tokenId, String tokenIndex) {
        try {
            return removeBalance(TokenUtils.createFullId(tokenId, tokenIndex));
        } catch (IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            log(e);
            return null;
        }
    }

    public MutableBalance getBalance(String id) {
        try {
            return balances.get(TokenUtils.toFullId(id));
        } catch (IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            log(e);
            return null;
        }
    }

    public MutableBalance getBalance(String tokenId, String tokenIndex) {
        try {
            return getBalance(TokenUtils.createFullId(tokenId, tokenIndex));
        } catch (IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            log(e);
            return null;
        }
    }

    public List<MutableBalance> getBalances() {
        return new ArrayList<>(balances.values());
    }

    public void setBalance(MutableBalance balance) {
        if (balance == null)
            return;

        try {
            balances.put(TokenUtils.createFullId(balance.id(), balance.index()), balance);
        } catch (IllegalArgumentException ignored) {
        } catch (Exception e) {
            log(e);
        }
    }

    public Map<String, MutableBalance> getBalancesMap() {
        return new HashMap<>(balances);
    }

    public boolean isEmpty() {
        return balances.isEmpty();
    }

    private static void log(Exception e) {
        SpigotBootstrap bootstrap = EnjinCraft.bootstrap();
        bootstrap.log(e);
    }

}
