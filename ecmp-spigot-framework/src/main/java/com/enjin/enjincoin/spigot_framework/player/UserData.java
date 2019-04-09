package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.service.users.vo.User;

import java.util.UUID;

public class UserData {

    private Integer id;
    private String name;

    public UserData(User user) {
        this.id = user.getId();
        this.name = user.getName();
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public UUID getUuid() {
        return UUID.fromString(this.name);
    }
}
