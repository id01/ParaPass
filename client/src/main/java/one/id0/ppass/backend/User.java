package one.id0.ppass.backend;

import java.util.ArrayList;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import one.id0.ppass.gen.PPassNetwork;

// User class for a specific user in a PPassNetwork.
public class User {
	// Global variables
	protected Crypto crypto;
	protected PPassNetwork ppass;
	protected Logger logger;
	protected byte[] userhash;
	
	// Constructor. Takes in the PPassNetwork connection the user is on
	// the username, the password, and whether we want to create a user or log into one 
	public User(PPassNetwork ppass, String username, String masterpass, byte[] ppassFileContent, boolean createmode, Logger logger) throws Exception {
		// Copy over logger, initialize cache to null
		this.logger = logger;
		// Generate keys in new Crypto class and copy over ppass to global.
		logger.log("Generating keys...");
		crypto = new Crypto(username, masterpass, PPassBackend.ppassFileVersion, ppassFileContent);
		this.ppass = ppass;
		// Get userhash and masterkeyhash
		this.userhash = crypto.getUserHash();
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
		} else {
			if (ppass.checkUserFree(userhash).send()) {
				throw new Exception("User doesn't exist!");
			}
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
	
	// Puts a password. Returns plaintext password, and writes ciphertext to ctout if ctout isn't null.
	public String putPassword(String accountname, byte[] charlist, int length, byte[] ctout) throws Exception {
		// Generate password, ciphertext, and account ID
		logger.log("Generating password...");
		String pass = crypto.generatePassword(charlist, length);
		byte[] ciphertext = crypto.encrypt(accountname, pass);
		byte[] accounthash = crypto.hashAccountName(accountname);
		// Attempt to create user account and then check login status
		logger.log("Attempting to put password into blockchain...");
		TransactionReceipt tx_receipt = ppass.putPassword(userhash, accounthash, ciphertext).send();
		logger.log("Success! Tranaction hash: " + tx_receipt.getTransactionHash());
		// Copy ciphertext to ctout if it isn't null
		if (ctout != null) {
			System.arraycopy(ciphertext, 0, ctout, 0, ciphertext.length);
		}
		// Return plaintext password
		return pass;
	}
	
	// Puts a password without ctout (null as default)
	public String putPassword(String accountname, byte[] charlist, int length) throws Exception {
		return putPassword(accountname, charlist, length, null);
	}
	
	// Gets a password and account name in the form {accountname, password} by ID,
	// and writes ciphertext to ctout if ctout isn't null.
	public String[] getPassword(byte[] accounthash, byte[] ctout) throws Exception {
		// Get ciphertext
		logger.log("Getting password...");
		byte[] ciphertext = ppass.getPassword(userhash, accounthash).send();
		// Copy ciphertext to ctout if it isn't null
		if (ctout != null) {
			System.arraycopy(ciphertext, 0, ctout, 0, ciphertext.length);
		}
		// Make sure ciphertext exists (if it's all 0's that means it doesn't.)
		boolean ciphertextExists = false;
		for (byte b : ciphertext) {
			ciphertextExists = (ciphertextExists || b!=0);
		}
		if (!ciphertextExists) {
			throw new Exception("Account doesn't exist!");
		}
		// Decrypt and return
		logger.log("Done!");
		return crypto.decrypt(ciphertext);
	}
	
	// Gets a password without ctout (null as default)
	public String[] getPassword(byte[] accounthash) throws Exception {
		return getPassword(accounthash);
	}
	
	// Gets a password and account name in the form {accountname, password}
	public String[] getPassword(String accountname) throws Exception {
		return getPassword(crypto.hashAccountName(accountname));
	}
	
	// Gets all accounts
	public ArrayList<byte[]> getAllAccounts() throws Exception {
		ArrayList<byte[]> allAccounts = new ArrayList<byte[]>();
		for (Object account : ppass.getAllAccounts(userhash).send()) {
			allAccounts.add((byte[])account);
		}
		return allAccounts;
	}
	
	// Gets this user's user hash
	public byte[] getUserHash() {
		return userhash;
	}
	
	// Gets an account hash
	public byte[] getAccountHash(String accountname) {
		return crypto.hashAccountName(accountname);
	}
	
	// Encrypts/Decrypts an account description
	public byte[] encryptAccountDescription(String description) {
		return crypto.encryptMiscData(description.getBytes());
	}
	public String decryptAccountDescription(byte[] encrypted) throws Exception {
		return new String(crypto.decryptMiscData(encrypted));
	}
	
	// Sets the logger
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
}
