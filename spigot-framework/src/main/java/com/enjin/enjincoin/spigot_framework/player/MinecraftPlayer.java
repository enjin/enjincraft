package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.sdk.client.service.users.vo.User;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import org.bukkit.entity.Player;

import java.util.Optional;

public class MinecraftPlayer {

    // Bukkit Objects
    private BasePlugin plugin;
    private Player bukkitPlayer;

    // Enjin Coin User Data
    private Integer userId;

    // Enjin Coin Identity Data
    private Integer identityId;
    private String linkingCode;
    private String ethereumAddress;

    // State Fields
    private boolean userLoaded;
    private boolean identityLoaded;

    public MinecraftPlayer(BasePlugin plugin, Player player) {
        this.plugin = plugin;
        this.bukkitPlayer = player;
    }

    public Player getBukkitPlayer() {
        return this.bukkitPlayer;
    }

    public String getEthereumAddress() {
        return ethereumAddress;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getIdentityId() {
        return identityId;
    }

    public String getLinkingCode() {
        return linkingCode;
    }

    public boolean isUserLoaded() {
        return this.userLoaded;
    }

    public void loadUser(User user) {
        if (user == null) {
            return;
        }

        this.userId = user.getId();
        this.userLoaded = true;

        Integer appId = this.plugin.getBootstrap().getAppId();
        Optional<Identity> optionalIdentity = user.getIdentities().stream()
                .filter(identity -> identity.getAppId() == appId)
                .findFirst();
        optionalIdentity.ifPresent(this::loadIdentity);
    }

    public boolean isIdentityLoaded() {
        return this.identityLoaded;
    }

    public void loadIdentity(Identity identity) {
        if (identity == null) {
            return;
        }

        this.ethereumAddress = identity.getEthereumAddress();
        this.identityId = identity.getId();
        this.linkingCode = identity.getLinkingCode();
        this.identityLoaded = true;
    }

    public boolean isLoaded() {
        return isUserLoaded() && isIdentityLoaded();
    }

    protected void cleanUp() {
        this.bukkitPlayer = null;
    }
}
