package one.id0.ppass.backend;

import java.util.ArrayList;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import one.id0.ppass.gen.PPassNetwork;

// User class for a specific user in a PPassNetwork.
public class User {
	// Global variables
	private Crypto crypto;
	private PPassNetwork ppass;
	private Logger logger;
	private byte[] userhash;
	
	// Constructor. Takes in the PPassNetwork connection the user is on
	// the username, the password, and whether we want to create a user or log into one 
	public User(PPassNetwork ppass, String username, String masterpass, boolean createmode, Logger logger) throws Exception {
		// Copy over logger
		this.logger = logger;
		// Generate keys in new Crypto class and copy over ppass to global.
		logger.log("Generating keys...");
		crypto = new Crypto(username, masterpass);
		this.ppass = ppass;
		// Get userhash and masterkeyhash
		userhash = crypto.getUserHash();
		byte[] masterkeyhash = crypto.getMasterKeyHash();
		// If we are creating a user account, attempt to create it.
		if (createmode) {
			// Check whether user exists. If not, attempt to create user account.
			logger.log("Attempting to create user account...");
			if (!ppass.checkUserFree(userhash).send()) {
				throw new Exception("User already exists!");
			}
			TransactionReceipt tx_receipt = ppass.addUser(userhash, masterkeyhash).send();
			logger.log("Success! Tranaction hash: " + tx_receipt.getTransactionHash());
		}
		// Check if we are the owner of this account and our masterkeyhash is right
		logger.log("Checking login status...");
		if (!ppass.checkLogin(userhash, masterkeyhash).send()) {
			logger.logErr("Failed to log in.");
			throw new Exception("Failed to log in.");
		}
		// We are now logged in
		logger.log("Logged in");
	}
	
	// Puts a password
	public String putPassword(String accountname, byte[] charlist, int length) throws Exception {
		// Generate password and ciphertext
		logger.log("Generating password...");
		String pass = crypto.generatePassword(charlist, length);
		byte[] ciphertext = crypto.encrypt(accountname, pass);
		// Attempt to create user account and then check login status
		logger.log("Attempting to put password into blockchain...");
		TransactionReceipt tx_receipt = ppass.putPassword(crypto.getUserHash(), crypto.hashAccountName(accountname), ciphertext).send();
		logger.log("Success! Tranaction hash: " + tx_receipt.getTransactionHash());
		// Return plaintext password
		return pass;
	}
	
	// Gets a password and account name in the form {accountname, password} by ID
	public String[] getPasswordById(byte[] accounthash) throws Exception {
		// Get ciphertext and decrypt
		logger.log("Getting password...");
		byte[] ciphertext = ppass.getPassword(crypto.getUserHash(), accounthash).send();
		logger.log("Done!");
		return crypto.decrypt(ciphertext);
	}
	
	// Gets a password and account name in the form {accountname, password}
	public String[] getPassword(String accountname) throws Exception {
		return getPasswordById(crypto.hashAccountName(accountname));
	}
	
	// Gets all accounts
	public ArrayList<byte[]> getAllAccounts() throws Exception {
		ArrayList<byte[]> allAccounts = new ArrayList<byte[]>();
		for (Object account : ppass.getAllAccounts(userhash).send()) {
			allAccounts.add((byte[])account);
		}
		return allAccounts;
	}
	
	// Sets the logger
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
}
