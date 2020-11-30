package com.techelevator.tenmo.controller;

import com.techelevator.tenmo.dao.AccountDAO;
import com.techelevator.tenmo.dao.TransferDAO;
import com.techelevator.tenmo.dao.UserDAO;
import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.TransferDTO;
import com.techelevator.tenmo.security.jwt.TokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@PreAuthorize("isAuthenticated()")
@RestController
public class TransferController {

    private final TokenProvider tokenProvider;
    private TransferDAO transferDAO;
    private UserDAO userDAO;
    private AccountDAO accountDAO;

    public TransferController(TokenProvider tokenProvider, TransferDAO transferDAO, UserDAO userDAO, AccountDAO accountDAO) {
        this.tokenProvider = tokenProvider;
        this.transferDAO = transferDAO;
        this.userDAO = userDAO;
        this.accountDAO = accountDAO;
    }

    @RequestMapping(value = "/transfers", method = RequestMethod.GET)
    public List<Transfer> getAllTransfers(Principal principal) {
        List<Transfer> result;
        int id = userDAO.findIdByUsername(principal.getName());
        result = transferDAO.getAllTransfers(id);
        return result;
    }

    //pass username, not principal
    @RequestMapping(value = "/transfers/{id}", method = RequestMethod.GET)
    public Transfer getTransferFromTransferId(Principal principal, @PathVariable int id) {
        Transfer result;
        if (transferDAO.checkBeforeGettingTransfer(principal, id)) {
            result = transferDAO.getTransferFromId(id);
        } else {
            result = null;
        }
        return result;
    }

    //pass username, not principal
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RequestMapping(value = "/transfers", method = RequestMethod.POST)
    public void postTransfer(Principal principal, @RequestBody TransferDTO transferDTO) {
        Account depositAccount = accountDAO.getUserAccountByUserId(transferDTO.getUserIdTo());
        Account withdrawalAccount = accountDAO.getUserAccountByUserId(transferDTO.getUserIdFrom());
        if (depositAccount == null || withdrawalAccount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more of the accounts does not exist.");
        } else {
            boolean checksPass = transferDAO.checksBeforeTransfer(principal, depositAccount, withdrawalAccount, transferDTO.getAmount());
            if (checksPass) {
                transferDAO.addTransfer(depositAccount, withdrawalAccount, transferDTO.getAmount());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not post transfer.");
            }
        }
    }

}
