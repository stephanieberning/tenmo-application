package com.techelevator.tenmo.controller;

import com.techelevator.tenmo.dao.AccountDAO;
import com.techelevator.tenmo.dao.UserDAO;
import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.security.jwt.TokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@PreAuthorize("isAuthenticated()")
@RestController
public class AccountController {

    private AccountDAO accountDAO;
    private UserDAO userDAO;

    public AccountController(AccountDAO accountDAO, UserDAO userDAO) {
        this.accountDAO = accountDAO;
        this.userDAO = userDAO;
    }

    // change to not need ID
    @RequestMapping(value = "/accounts/{id}", method = RequestMethod.GET)
    public Account getAccountByUserId(@PathVariable int id, Principal principal) {
        Account result;
        int principalId = userDAO.findIdByUsername(principal.getName());
        if (principalId == id) {
            result = accountDAO.getUserAccountByUserId(id);
        } else {
            result = null;
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot get information for another user's account.");
        }
        return result;
    }

}
