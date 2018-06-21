package com.enjin.enjincoin.spigot_framework.entity;

import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnjinCoinPlayer {

    private UUID uuid;

    private Identity identity;

    private Map<Integer, Double> tokenBalances;

    protected EnjinCoinPlayer(UUID uuid, Identity identity) {
        this.identity = identity;
        this.identity.getTokens().forEach(token -> tokenBalances.put(token.getTokenId(), token.getBalance()));
    }

    public EnjinCoinPlayer(String uuid, Identity identity) {
        this.identity = identity;

        UUID uid = UUID.fromString(uuid);
        this.uuid = uid;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

    public Identity getIdentity() {
        return this.identity;
    }

    public Map<Integer, Double> getTokenBalances() {
        return new HashMap<>(this.tokenBalances);
    }

    public EnjinCoinPlayer create(UUID uuid, Identity identity) {
        EnjinCoinPlayer player = null;
        if (uuid != null && identity != null) {
            player = new EnjinCoinPlayer(uuid, identity);
        }
        return player;
    }

}
