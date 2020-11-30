package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.Account;

import java.math.BigDecimal;

public interface AccountDAO {

    Account getUserAccountByUserId(int id);

    Account deposit(Account account, BigDecimal amount);

    Account withdrawal(Account account, BigDecimal amount);

}
