package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;

public class IdentityData {

    private Integer id;
    private String ethereumAddress;
    private String linkingCode;

    public IdentityData(Identity identity) {
        this.id = identity.getId();
        this.ethereumAddress = identity.getEthereumAddress();
        this.linkingCode = identity.getLinkingCode();
    }

    public Integer getId() {
        return this.id;
    }

    public String getEthereumAddress() {
        return this.ethereumAddress;
    }

    public String getLinkingCode() {
        return this.linkingCode;
    }
}
