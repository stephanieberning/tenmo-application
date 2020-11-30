package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcTransferDAO implements TransferDAO {

    private JdbcTemplate jdbcTemplate;
    private JdbcUserDAO userDAO;
    private JdbcAccountDAO accountDAO;

    public JdbcTransferDAO(JdbcTemplate jdbcTemplate, JdbcUserDAO userDAO, JdbcAccountDAO accountDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.userDAO = userDAO;
        this.accountDAO = accountDAO;
    }

    //constant for 2s or sub-queries
    @Override
    public void addTransfer(Account depositAccount, Account withdrawalAccount, BigDecimal amount) {
        Transfer result;
        String sql = "INSERT INTO transfers (transfer_type_id, transfer_status_id, account_from, account_to, amount) " +
                "VALUES (?, ?, ?, ?, ?);";
        jdbcTemplate.update(sql, 2, 2, withdrawalAccount.getAccountId(), depositAccount.getAccountId(), amount);
        accountDAO.withdrawal(withdrawalAccount, amount);
        accountDAO.deposit(depositAccount, amount);
    }

    @Override
    public List<Transfer> getAllTransfers(int id) {
        List<Transfer> transfers = new ArrayList<>();
        String sql = "SELECT transfer_id, transfer_type_id, transfer_status_id, amount, user_from.username AS user_from, " +
                        "user_to.username AS user_to, account_from.account_id AS account_from_id, account_to.account_id AS account_to_id " +
                        "FROM transfers " +
                        "JOIN accounts AS account_from ON transfers.account_from = account_from.account_id " +
                        "JOIN accounts AS account_to ON transfers.account_to = account_to.account_id " +
                        "JOIN users AS user_from ON account_from.user_id = user_from.user_id " +
                        "JOIN users AS user_to ON account_to.user_id = user_to.user_id " +
                        "WHERE account_from.user_id = ? OR account_to.user_id = ?;";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, id, id);
        while (rowSet.next()) {
            transfers.add(mapRowToTransfer(rowSet));
        }
        return transfers;
    }

    //put sql in constant
    @Override
    public Transfer getTransferFromId(int id) {
        Transfer transfer;
        String sql = "SELECT transfer_id, transfer_type_id, transfer_status_id, amount, user_from.username AS user_from, " +
                "user_to.username AS user_to, account_from.account_id AS account_from_id, account_to.account_id AS account_to_id " +
                "FROM transfers " +
                "JOIN accounts AS account_from ON transfers.account_from = account_from.account_id " +
                "JOIN accounts AS account_to ON transfers.account_to = account_to.account_id " +
                "JOIN users AS user_from ON account_from.user_id = user_from.user_id " +
                "JOIN users AS user_to ON account_to.user_id = user_to.user_id WHERE transfer_id = ?;";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, id);
        if (rowSet.next()) {
            transfer = mapRowToTransfer(rowSet);
        } else {
            transfer = null;
        }
        return transfer;
    }

    @Override
    public boolean checksBeforeTransfer(Principal principal, Account depositAccount, Account withdrawalAccount, BigDecimal amount) {
        boolean result = false;
        if (checkSameAccountTransfer( principal, withdrawalAccount)){
            if (checkCannotTransferToSelf(principal, depositAccount)){
                if (checkNotNegativeAmount(amount)) {
                    if (checkForSufficientFunds(withdrawalAccount, amount)){
                        if (checkIfDepositAccountExists(depositAccount)) {
                            result = true;
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean checkSameAccountTransfer(Principal principal, Account withdrawalAccount){
        boolean result = false;
        int principalId = userDAO.findIdByUsername(principal.getName());
        int withdrawalId = withdrawalAccount.getUserId();
        if (principalId == withdrawalId) {
            result = true;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only transfer from your account.");
        }
        return result;
    }
    private boolean checkCannotTransferToSelf(Principal principal, Account depositAccount) {
        boolean result = false;
        int principalId = userDAO.findIdByUsername(principal.getName());
        int depositId = depositAccount.getUserId();
        if (principalId != depositId) {
            result = true;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can not transfer to yourself.");
        }
        return result;
    }
    private boolean checkNotNegativeAmount(BigDecimal amount) {
        boolean result = false;
        boolean isPositive = amount.compareTo(BigDecimal.ZERO) > 0;
        if (isPositive) {
            result = true;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer negative amounts.");
        }
        return result;
    }
    private boolean checkForSufficientFunds(Account withdrawalAccount, BigDecimal amount) {
        boolean result = false;
        boolean balanceIsEnough = withdrawalAccount.getBalance().compareTo(amount) >= 0;

        if (balanceIsEnough) {
            result = true;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds.");
        }
        return result;
    }

    //check all users takes too much time
    private boolean checkIfDepositAccountExists(Account depositAccount) {
        int depositId = depositAccount.getUserId();
        boolean result = false;
        boolean depositAccountExists = false;
        List<User> users = userDAO.findAll();
        for (User user : users) {
            if (user.getId() == depositId) {
                depositAccountExists = true;
            }
        }
        if (depositAccountExists) {
            result = true;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deposit account does not exist.");
        }
        return result;
    }
    @Override
    public boolean checkBeforeGettingTransfer(Principal principal, int id) {
        boolean result = false;
        int principalUserId = userDAO.findIdByUsername(principal.getName());
        int principalAccountId = accountDAO.getUserAccountByUserId(principalUserId).getAccountId();
        if (getTransferFromId(id) != null) {
            Transfer transfer = getTransferFromId(id);
            if (principalAccountId == transfer.getAccountFrom() || principalAccountId == transfer.getAccountTo()) {
                result = true;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only see transfers from your account.");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No transfer history for that id.");
        }
        return result;
    }

    private Transfer mapRowToTransfer(SqlRowSet rowSet) {
        Transfer transfer = new Transfer();
        transfer.setTransferId(rowSet.getInt("transfer_id"));
        transfer.setTransferTypeId(rowSet.getInt("transfer_type_id"));
        transfer.setTransferStatusId(rowSet.getInt("transfer_status_id"));
        transfer.setAccountFrom(rowSet.getInt("account_from_id"));
        transfer.setAccountTo(rowSet.getInt("account_to_id"));
        transfer.setUserFrom(rowSet.getString("user_from"));
        transfer.setUserTo(rowSet.getString("user_to"));
        transfer.setAmount(rowSet.getBigDecimal("amount"));
        return transfer;
    }

}
