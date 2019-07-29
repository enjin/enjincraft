package com.enjin.ecmp.spigot_framework.wallet;

import com.enjin.enjincoin.sdk.model.service.tokens.Token;

import java.math.BigInteger;

/**
 * Thread safe class that represents the current balance
 * and withdrawn amount of a token. Supports operations
 * to add and subtract from the balance and withdraw and
 * deposit tokens to be used in-game.
 */
public class Balance {

    // ID of the token
    private final String tokenId;

    // Balance contained in wallet
    private Integer balance = 0;

    // Amount withdrawn from balance
    private Integer withdrawn = 0;

    public Balance(Token token) {
        this.tokenId = token.getTokenId();
        this.balance = token.getBalance();
    }

    public String getTokenId() {
        return this.tokenId;
    }

    public Integer balance() {
        return this.balance;
    }

    public Integer withdrawn() {
        synchronized (this.withdrawn) {
            return this.withdrawn;
        }
    }

    public Integer amountAvailableForWithdrawal() {
        synchronized (this.balance) {
            synchronized (this.withdrawn) {
                return this.balance - this.withdrawn;
            }
        }
    }

    public Integer subtract(Integer amount) {
        synchronized (this.balance) {
            this.balance -= amount;
            if (this.balance < 0) this.balance = 0;
            return this.balance;
        }
    }

    public Integer add(Integer amount) {
        synchronized (this.balance) {
            this.balance += amount;
            return this.balance;
        }
    }

    public boolean withdraw(Integer amount) {
        synchronized (this.withdrawn) {
            if (amountAvailableForWithdrawal().compareTo(amount) != -1) {
                this.withdrawn += amount;
                return true;
            }
        }

        return false;
    }

    public void deposit(Integer amount) {
        synchronized (this.withdrawn) {
            this.withdrawn -= amount;
            if (this.withdrawn < 0) this.withdrawn = 0;
        }
    }
}
