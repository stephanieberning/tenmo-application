package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.Transfer;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

public interface TransferDAO {

    void addTransfer(Account depositAccount, Account withdrawalAccount, BigDecimal amount);

    List<Transfer> getAllTransfers(int id);

    Transfer getTransferFromId(int id);

    boolean checksBeforeTransfer(Principal principal, Account depositAccount, Account withdrawalAccount, BigDecimal amount);

    boolean checkBeforeGettingTransfer(Principal principal, int id);
}
