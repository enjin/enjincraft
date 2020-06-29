package com.enjin.enjincraft.spigot.wallet;

import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.sdk.models.balance.Balance;

/**
 * Thread safe class that represents the current balance
 * and withdrawn amount of a token. Supports operations
 * to add and subtract from the balance and withdraw and
 * deposit tokens to be used in-game.
 */
public class MutableBalance {

    private final String tokenId;
    private final String tokenIndex;
    private Integer balance = 0;
    private Integer withdrawn = 0;
    private final Object balanceLock = new Object();
    private final Object withdrawnLock = new Object();

    public MutableBalance(Balance balance) {
        this(balance.getId(), balance.getIndex(), balance.getValue());
    }

    public MutableBalance(String tokenId, String tokenIndex, Integer balance) {
        this.tokenId = tokenId;
        this.tokenIndex = tokenIndex == null
                ? TokenUtils.BASE_INDEX
                : tokenIndex;
        this.balance = balance;
    }

    public String id() {
        return this.tokenId;
    }

    public String index() {
        return tokenIndex;
    }

    public Integer balance() {
        synchronized (balanceLock) {
            return balance;
        }
    }

    public Integer withdrawn() {
        synchronized (withdrawnLock) {
            return withdrawn;
        }
    }

    public Integer amountAvailableForWithdrawal() {
        synchronized (balanceLock) {
            synchronized (withdrawnLock) {
                return balance - withdrawn;
            }
        }
    }

    public Integer subtract(Integer amount) {
        synchronized (balanceLock) {
            balance = Math.max(0, balance - amount);
            synchronized (withdrawnLock) {
                if (withdrawn > balance)
                    withdrawn = balance;
            }
            return balance;
        }
    }

    public Integer add(Integer amount) {
        synchronized (balanceLock) {
            balance = Math.max(0, balance + amount);
            synchronized (withdrawnLock) {
                if (withdrawn > balance)
                    withdrawn = balance;
            }
            return balance;
        }
    }

    public void set(Integer amount) throws IllegalArgumentException {
        if (amount < 0)
            throw new IllegalArgumentException("Cannot set balance to a negative amount");

        synchronized (balanceLock) {
            balance = amount;
            synchronized (withdrawnLock) {
                if (withdrawn > balance)
                    withdrawn = balance;
            }
        }
    }

    public boolean withdraw(Integer amount) throws IllegalArgumentException {
        if (amount < 0)
            throw new IllegalArgumentException("Cannot withdraw a negative amount");

        synchronized (withdrawnLock) {
            if (amountAvailableForWithdrawal().compareTo(amount) >= 0) {
                withdrawn += amount;
                return true;
            }
        }

        return false;
    }

    public void deposit(Integer amount) throws IllegalArgumentException {
        if (amount < 0)
            throw new IllegalArgumentException("Cannot deposit a negative amount");

        synchronized (withdrawnLock) {
            withdrawn = Math.max(0, withdrawn - amount);
        }
    }

    public void reset() {
        synchronized (withdrawnLock) {
            withdrawn = 0;
        }
    }
}
