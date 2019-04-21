package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.model.service.users.User;

import java.math.BigInteger;
import java.util.UUID;

public class UserData {

    private BigInteger id;
    private String name;

    public UserData(User user) {
        this.id = user.getId();
        this.name = user.getName();
    }

    public BigInteger getId() {
        return id;
    }

    public String getName() {
        return this.name;
    }

    public UUID getUuid() {
        return UUID.fromString(this.name);
    }
}
