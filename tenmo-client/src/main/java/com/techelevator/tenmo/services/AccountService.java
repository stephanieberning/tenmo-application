package com.techelevator.tenmo.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techelevator.tenmo.models.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AccountService {

    private String authToken = "";
    private String baseUrl;
    private RestTemplate restTemplate = new RestTemplate();

    public AccountService(String url) {
        this.baseUrl = url;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public Account getCurrentBalance(int id) {
        Account result;
        try {
            result = restTemplate.exchange(baseUrl + "accounts/" + id, HttpMethod.GET, makeAuthEntity(), Account.class).getBody();
        } catch (ResourceAccessException | RestClientResponseException e) {
            result = null;
            System.out.println("Could not get current balance.");
        }
        return result;
    }

    public User[] getAllUsers() {
        User[] result = new User[]{};
        try {
            HttpEntity<User[]> entity = restTemplate.exchange(baseUrl + "users", HttpMethod.GET, makeAuthEntity(), User[].class);
           result = entity.getBody();
        } catch (ResourceAccessException | RestClientResponseException e){
            System.out.println("Could not find list of Users.");
        }
        return result;
    }

    public Transfer sendTransfer(User user, BigDecimal amount, AuthenticatedUser currentUser) throws JsonProcessingException {
        Transfer result = new Transfer();
        try{
            result = restTemplate.exchange(baseUrl + "transfers", HttpMethod.POST, makeTransferEntity(user, amount, currentUser), Transfer.class).getBody();
        }catch(ResourceAccessException e){
            System.out.println("Could not complete transfer.");
        }catch (RestClientResponseException exception){
            ObjectMapper mapper = new ObjectMapper();
            ErrorDetails details = mapper.readValue(exception.getResponseBodyAsString(), ErrorDetails.class);
            System.out.println(details.getMessage());
//            System.out.println(exception.getResponseBodyAsString());
        }
        return result;
    }

    public Transfer[] getAllTransfers() {
        Transfer[] result = new Transfer[]{};
        try {
            HttpEntity<Transfer[]> entity = restTemplate.exchange(baseUrl + "transfers", HttpMethod.GET, makeAuthEntity(), Transfer[].class);
            result = entity.getBody();
        } catch (ResourceAccessException | RestClientResponseException e){
            System.out.println("Could not find list of Transfers.");
        }
        return result;
    }

    public Transfer getTransferDetails(int id) {
        Transfer result;
        try {
            result = restTemplate.exchange(baseUrl + "transfers/" + id, HttpMethod.GET, makeAuthEntity(), Transfer.class).getBody();
        } catch (ResourceAccessException | RestClientResponseException e) {
            result = null;
            System.out.println("Could not get transfer details.");
        }
        return result;
    }

    private HttpEntity<Account> makeAccountEntity(Account account) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(authToken);
        return new HttpEntity<>(account, headers);
    }

    private HttpEntity<TransferDTO> makeTransferEntity(User user, BigDecimal amount, AuthenticatedUser currentUser) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(authToken);
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setUserIdTo(user.getId());
        transferDTO.setAmount(amount);
        transferDTO.setUserIdFrom(currentUser.getUser().getId());
        return new HttpEntity<>(transferDTO, headers);
    }

    private HttpEntity<?> makeAuthEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        return new HttpEntity<>(headers);
    }

}
