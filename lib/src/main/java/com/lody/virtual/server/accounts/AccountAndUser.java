package com.lody.virtual.server.accounts;

import android.accounts.Account;

/**
 * Used to store the Account and the UserId this account is associated with.
 */
public class AccountAndUser {
    public Account account;
    public int userId;

    public AccountAndUser(Account account, int userId) {
        this.account = account;
        this.userId = userId;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountAndUser)) return false;
        final AccountAndUser other = (AccountAndUser) o;
        return this.account.equals(other.account)
                && this.userId == other.userId;
    }

    @Override
    public int hashCode() {
        return account.hashCode() + userId;
    }

    public String toString() {
        return account.toString() + " u" + userId;
    }
}
