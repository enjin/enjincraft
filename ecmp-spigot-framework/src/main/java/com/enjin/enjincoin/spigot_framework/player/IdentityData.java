package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.service.identities.vo.Identity;

public class IdentityData {

    private Integer id;
    private String ethereumAddress;
    private String linkingCode;
    private Double ethBalance;
    private Double enjBalance;

    public IdentityData(Identity identity) {
        this.id = identity.getId();
        this.ethereumAddress = identity.getEthereumAddress();
        this.linkingCode = identity.getLinkingCode();
        this.ethBalance = identity.getEthBalance();
        this.enjBalance = identity.getEnjBalance();
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

    public Double getEthBalance() { return this.ethBalance; }

    public Double getEnjBalance() { return this.enjBalance; }
}
