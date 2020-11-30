package com.techelevator.tenmo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.techelevator.tenmo.models.*;
import com.techelevator.tenmo.services.AccountService;
import com.techelevator.tenmo.services.AuthenticationService;
import com.techelevator.tenmo.services.AuthenticationServiceException;
import com.techelevator.tenmo.services.ServerConnectionException;
import com.techelevator.view.ConsoleService;

import java.math.BigDecimal;
import java.util.List;

public class App {

private static final String API_BASE_URL = "http://localhost:8080/";
    
    private static final String MENU_OPTION_EXIT = "Exit";
    private static final String LOGIN_MENU_OPTION_REGISTER = "Register";
	private static final String LOGIN_MENU_OPTION_LOGIN = "Login";
	private static final String[] LOGIN_MENU_OPTIONS = { LOGIN_MENU_OPTION_REGISTER, LOGIN_MENU_OPTION_LOGIN, MENU_OPTION_EXIT };
	private static final String MAIN_MENU_OPTION_VIEW_BALANCE = "View your current balance";
	private static final String MAIN_MENU_OPTION_SEND_BUCKS = "Send TE bucks";
	private static final String MAIN_MENU_OPTION_VIEW_PAST_TRANSFERS = "View your past transfers";
	private static final String MAIN_MENU_OPTION_REQUEST_BUCKS = "Request TE bucks";
	private static final String MAIN_MENU_OPTION_VIEW_PENDING_REQUESTS = "View your pending requests";
	private static final String MAIN_MENU_OPTION_LOGIN = "Login as different user";
	private static final String[] MAIN_MENU_OPTIONS = { MAIN_MENU_OPTION_VIEW_BALANCE, MAIN_MENU_OPTION_SEND_BUCKS, MAIN_MENU_OPTION_VIEW_PAST_TRANSFERS, MAIN_MENU_OPTION_REQUEST_BUCKS, MAIN_MENU_OPTION_VIEW_PENDING_REQUESTS, MAIN_MENU_OPTION_LOGIN, MENU_OPTION_EXIT };
	private static final String LINE = "-------------------------------";

    private AuthenticatedUser currentUser;
    private ConsoleService console;
    private AuthenticationService authenticationService;
    private AccountService accountService;

    public static void main(String[] args) throws JsonProcessingException {
    	App app = new App(new ConsoleService(System.in, System.out), new AuthenticationService(API_BASE_URL), new AccountService(API_BASE_URL));
    	app.run();
    }

    public App(ConsoleService console, AuthenticationService authenticationService, AccountService accountService) {
		this.console = console;
		this.authenticationService = authenticationService;
		this.accountService = accountService;
	}

	public void run() throws JsonProcessingException {
		System.out.println("*********************");
		System.out.println("* Welcome to TEnmo! *");
		System.out.println("*********************");
		
		registerAndLogin();
		if (isAuthenticated()) {
			mainMenu();
		}
	}

	private void mainMenu() throws JsonProcessingException {
    	boolean running = true;
		while(running) {
			String choice = (String)console.getChoiceFromOptionsDisplaysMenu(MAIN_MENU_OPTIONS);
			if(MAIN_MENU_OPTION_VIEW_BALANCE.equals(choice)) {
				viewCurrentBalance();
			} else if(MAIN_MENU_OPTION_VIEW_PAST_TRANSFERS.equals(choice)) {
				viewTransferHistory();
			} else if(MAIN_MENU_OPTION_VIEW_PENDING_REQUESTS.equals(choice)) {
				viewPendingRequests();
			} else if(MAIN_MENU_OPTION_SEND_BUCKS.equals(choice)) {
				sendBucks();
			} else if(MAIN_MENU_OPTION_REQUEST_BUCKS.equals(choice)) {
				requestBucks();
			} else if(MAIN_MENU_OPTION_LOGIN.equals(choice)) {
				login();
			} else {
				running = false;
			}
		}
	}

	private void viewCurrentBalance() {
    	int id = currentUser.getUser().getId();
    	Account account = accountService.getCurrentBalance(id);
    	System.out.println("Your current balance is: $" + account.getBalance());
	}

	private void viewTransferHistory() {
		Transfer[] allTransfers = printAllTransfers();
		Object choice = console.getChoiceFromOptions(allTransfers, "Enter Transfer ID to view details (0 to cancel): ");
		if (choice != null && !choice.equals(-1)) {
			Transfer transfers = (Transfer)choice;
			Transfer transferDetails = accountService.getTransferDetails(transfers.getTransferId());
			printTransferDetails(transferDetails);
		}
	}

	private void viewPendingRequests() {
		// TODO Auto-generated method stub
		
	}

	private void sendBucks() throws JsonProcessingException {
		User[] ourUsers = printAllUsers();
		Object choice = console.getChoiceFromOptions(ourUsers, "Enter ID of user you are sending to (0 to cancel): ");
		if (choice != null && !choice.equals(-1)) {
			User users = (User)choice;
			BigDecimal amountToTransfer = console.getUserInputBigDecimal("Enter amount");
			accountService.sendTransfer(users,amountToTransfer,currentUser);
		}
	}

	private void requestBucks() {
		// TODO Auto-generated method stub
		
	}

	private User[] printAllUsers(){
		System.out.println(LINE + "\nUsers \nID			Name\n" + LINE);
		User[] allUsers = accountService.getAllUsers();
		for (int i = 0; i < allUsers.length; i++) {
			if (!currentUser.getUser().getId().equals(allUsers[i].getId())) {
				int id = allUsers[i].getId();
				String username = allUsers[i].getUsername();
				System.out.println(id + "			" + username);
			}
		}
		System.out.println(LINE + "\n");
		return allUsers;
	}

	private Transfer[] printAllTransfers(){
		System.out.println(LINE + "\nTransfers \nID			From/To			Amount\n" + LINE);
		Transfer[] allTransfers = accountService.getAllTransfers();
		for (int i = 0; i < allTransfers.length; i++) {
			if (currentUser.getUser().getUsername().equals(allTransfers[i].getUserFrom())) {
				int id = allTransfers[i].getTransferId();
				String username = allTransfers[i].getUserTo();
				BigDecimal amount = allTransfers[i].getAmount();
				System.out.println(id + "		To: " + username + "		$ " + amount);
			}else{
				int id = allTransfers[i].getTransferId();
				String username = allTransfers[i].getUserFrom();
				BigDecimal amount = allTransfers[i].getAmount();
				System.out.println(id + "		From: " + username + "		$ " + amount);
			}
		}
		System.out.println(LINE + "\n");
		return allTransfers;
	}

	private void printTransferDetails(Transfer transfer){
		System.out.println(LINE + "\nTransfer Details\n" + LINE);
		System.out.println("Id: " + transfer.getTransferId());
		System.out.println("From: " + transfer.getUserFrom());
		System.out.println("To: " + transfer.getUserTo());
		if(transfer.getTransferTypeId() == 2) {
			System.out.println("Type: Send");
		}if(transfer.getTransferStatusId() == 2){
			System.out.println("Status: Approved");
		}
		System.out.println("Amount: $" + transfer.getAmount());
	}


	private void registerAndLogin() {
		while(!isAuthenticated()) {
			String choice = (String)console.getChoiceFromOptionsDisplaysMenu(LOGIN_MENU_OPTIONS);
			if (LOGIN_MENU_OPTION_LOGIN.equals(choice)) {
				login();
			} else if (LOGIN_MENU_OPTION_REGISTER.equals(choice)) {
				register();
			} else {
				break;
			}
		}
	}

	private boolean isAuthenticated() {
		return currentUser != null;
	}

	private void register() {
		System.out.println("Please register a new user account");
		boolean isRegistered = false;
        while (!isRegistered) //will keep looping until user is registered
        {
            UserCredentials credentials = collectUserCredentials();
            try {
            	authenticationService.register(credentials);
            	isRegistered = true;
            	System.out.println("Registration successful. You can now login.");
            } catch(AuthenticationServiceException | ServerConnectionException e) {
            	System.out.println("REGISTRATION ERROR: "+e.getMessage());
				System.out.println("Please attempt to register again.");
            }
        }
	}

	private void login() {
		System.out.println("Please log in");
		currentUser = null;
		while (currentUser == null) //will keep looping until user is logged in
		{
			UserCredentials credentials = collectUserCredentials();
		    try {
				currentUser = authenticationService.login(credentials);
				accountService.setAuthToken(currentUser.getToken());
			} catch (AuthenticationServiceException | ServerConnectionException e) {
				System.out.println("LOGIN ERROR: "+e.getMessage());
				System.out.println("Please attempt to login again.");
			}
		}
	}
	
	private UserCredentials collectUserCredentials() {
		String username = console.getUserInput("Username");
		String password = console.getUserInput("Password");
		return new UserCredentials(username, password);
	}
}
