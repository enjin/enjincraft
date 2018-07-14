package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.sdk.client.service.tokens.vo.Token;

public class TokenData {

    // ID of the token
    private String id;

    // Wallet Balance from the TP (max balance)
    private Double balance;

    // Amount available for in-game checkout
    private int checkedout = 0;

    // not sure what this one is for yet.
    private Integer decimals;

    // CryptoItem Symbol from TP entry for token
    private String symbol;

    public TokenData(Token token) {
        this.id = token.getTokenId();
        this.balance = token.getBalance();
        this.decimals = token.getDecimals();
        this.symbol = token.getSymbol();
//        this.checkedout = 0;
    }

    public String getId() {
        return this.id;
    }

    public int getCheckedOut() { return this.checkedout; }

    public void setCheckedOut(int CheckedOut) {
        this.checkedout = checkedout;
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

    public Double addBalance(Double add) {
        this.balance += add;
        return this.balance;
    }
}
