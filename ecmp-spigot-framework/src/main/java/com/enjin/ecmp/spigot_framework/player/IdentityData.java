package com.enjin.ecmp.spigot_framework.player;

import com.enjin.enjincoin.sdk.model.service.identities.Identity;

import java.math.BigDecimal;
import java.math.BigInteger;

public class IdentityData {

    private int id;
    private String ethereumAddress;
    private String linkingCode;
    private BigDecimal ethBalance;
    private BigDecimal enjBalance;

    public IdentityData(Identity identity) {
        this.id = identity.getId();
        this.ethereumAddress = identity.getEthereumAddress();
        this.linkingCode = identity.getLinkingCode();
        this.ethBalance = identity.getEthBalance();
        this.enjBalance = identity.getEnjBalance();
    }

    public int getId() {
        return id;
    }

    public String getEthereumAddress() {
        return this.ethereumAddress;
    }

    public String getLinkingCode() {
        return this.linkingCode;
    }

    public BigDecimal getEthBalance() {
        return ethBalance;
    }

    public BigDecimal getEnjBalance() {
        return enjBalance;
    }
}
