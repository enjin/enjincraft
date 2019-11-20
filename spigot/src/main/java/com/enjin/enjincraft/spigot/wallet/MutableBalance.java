package com.enjin.enjincraft.spigot.wallet;

import com.enjin.sdk.model.service.balances.Balance;
import com.enjin.sdk.model.service.tokens.Token;

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
        this(balance.getTokenId(), balance.getTokenIndex(), balance.getAmount());
    }

    public MutableBalance(Token token) {
        this(token.getTokenId(), token.getIndex(), token.getBalance());
    }

    public MutableBalance(String tokenId, String tokenIndex, Integer balance) {
        this.tokenId = tokenId;
        this.tokenIndex = tokenIndex;
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
            balance -= amount;
            if (balance < 0)
                balance = 0;
            return balance;
        }
    }

    public Integer add(Integer amount) {
        synchronized (balanceLock) {
            balance += amount;
            return balance;
        }
    }

    public void set(Integer amount) {
        synchronized (balanceLock) {
            balance = amount;
            synchronized (withdrawnLock) {
                if (withdrawn > balance)
                    withdrawn = balance;
            }
        }
    }

    public boolean withdraw(Integer amount) {
        synchronized (withdrawnLock) {
            if (amountAvailableForWithdrawal().compareTo(amount) >= 0) {
                withdrawn += amount;
                return true;
            }
        }

        return false;
    }

    public void deposit(Integer amount) {
        synchronized (withdrawnLock) {
            withdrawn -= amount;
            if (withdrawn < 0)
                withdrawn = 0;
        }
    }

    public void reset() {
        synchronized (withdrawnLock) {
            withdrawn = 0;
        }
    }
}
