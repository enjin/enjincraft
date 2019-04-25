package com.enjin.enjincoin.spigot_framework.wallet;

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
    private BigInteger balance = BigInteger.ZERO;

    // Amount withdrawn from balance
    private BigInteger withdrawn = BigInteger.ZERO;

    public Balance(Token token) {
        this.tokenId = token.getTokenId();
        this.balance = token.getBalance();
    }

    public String getTokenId() {
        return this.tokenId;
    }

    public BigInteger balance() {
        return this.balance;
    }

    public BigInteger withdrawn() {
        synchronized (this.withdrawn) {
            return this.withdrawn;
        }
    }

    public BigInteger amountAvailableForWithdrawal() {
        synchronized (this.balance) {
            return this.balance.min(this.withdrawn);
        }
    }

    public BigInteger subtract(BigInteger amount) {
        synchronized (this.balance) {
            this.balance = this.balance.subtract(amount);
            if (this.balance.compareTo(BigInteger.ZERO) == -1) this.balance = BigInteger.ZERO;
            return this.balance;
        }
    }

    public BigInteger add(BigInteger amount) {
        synchronized (this.balance) {
            this.balance = this.balance.add(amount);
            return this.balance;
        }
    }

    public boolean withdraw(BigInteger amount) {
        synchronized (this.withdrawn) {
            if (amountAvailableForWithdrawal().compareTo(amount) != -1) {
                this.withdrawn.add(amount);
                return true;
            }
        }

        return false;
    }

    public void deposit(BigInteger amount) {
        synchronized (this.withdrawn) {
            this.withdrawn.subtract(amount);
            if (this.withdrawn.compareTo(BigInteger.ZERO) == -1) this.withdrawn = BigInteger.ZERO;
        }
    }
}
