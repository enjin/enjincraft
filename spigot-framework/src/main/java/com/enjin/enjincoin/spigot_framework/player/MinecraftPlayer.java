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

    // Trusted Platform Data
    private UserData userData;
    private Identity identity;
    private IdentityData identityData;
    private Wallet wallet;

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

    public UserData getUserData() {
        return this.userData;
    }

    public IdentityData getIdentityData() {
        return this.identityData;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Identity getIdentity() { return this.identity; }

    public boolean isUserLoaded() {
        return this.userLoaded;
    }

    public void loadUser(User user) {
        if (user == null) {
            return;
        }

        this.userData = new UserData(user);
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

        this.identity = identity;

        this.identityData = new IdentityData(identity);
        this.wallet = new Wallet();
        this.identityLoaded = true;

        this.wallet.populate(identity.getTokens());
    }

    public boolean isLoaded() {
        return isUserLoaded() && isIdentityLoaded();
    }

    protected void cleanUp() {
        this.bukkitPlayer = null;
    }
}
