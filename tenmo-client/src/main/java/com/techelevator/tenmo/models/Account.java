package com.techelevator.tenmo.models;

import java.math.BigDecimal;

public class Account {

    private int userId;
    private int accountId;
    private BigDecimal balance;

    public int getUserId() {
        return userId;
    }

    public int getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

}
