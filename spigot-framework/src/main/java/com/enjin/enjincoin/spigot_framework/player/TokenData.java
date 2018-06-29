package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.client.service.tokens.vo.Token;

public class TokenData {

    private String id;
    private Double balance;
    private Integer decimals;
    private String symbol;

    public TokenData(Token token) {
        this.id = token.getTokenId();
        this.balance = token.getBalance();
        this.decimals = token.getDecimals();
        this.symbol = token.getSymbol();
    }

    public String getId() {
        return this.id;
    }

    public Double getBalance() {
        return this.balance;
    }

    public Integer getDecimals() {
        return this.decimals;
    }

    public String getSymbol() {
        return this.symbol;
    }

    public Double addBalance(Double amount) {
        this.balance += amount;
        return this.balance;
    }
}
