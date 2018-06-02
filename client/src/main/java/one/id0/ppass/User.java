package one.id0.ppass;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import one.id0.ppass.gen.PPassNetwork;

public class User {
	// Global variables
	private Crypto crypto;
	private PPassNetwork ppass;
	
	// Constructor. Takes in the PPassNetwork connection the user is on
	// the username, the password, and whether we want to create a user or log into one 
	public User(PPassNetwork ppass, String username, String masterpass, boolean createmode) throws Exception {
		// Generate keys in new Crypto class and copy over ppass to global.
		Logger.log("Generating keys...");
		crypto = new Crypto(username, masterpass);
		this.ppass = ppass;
		// Get userhash and masterkeyhash
		byte[] userhash = crypto.getUserHash();
		byte[] masterkeyhash = crypto.getMasterKeyHash();
		// If we are creating a user account, attempt to create it.
		if (createmode) {
			// Check whether user exists. If not, attempt to create user account.
			Logger.log("Attempting to create user account...");
			if (!ppass.checkUserFree(userhash).send()) {
				throw new Exception("User already exists!");
			}
			TransactionReceipt tx_receipt = ppass.addUser(userhash, masterkeyhash).send();
			Logger.log("Success! Tranaction hash: " + tx_receipt.getTransactionHash());
		}
		// Check if we are the owner of this account and our masterkeyhash is right
		Logger.log("Checking login status...");
		if (!ppass.checkLogin(userhash, masterkeyhash).send()) {
			Logger.logErr("Failed to log in.");
			throw new Exception("Failed to log in.");
		}
		// We are now logged in
		Logger.log("Logged in");
	}
	
	// Puts a password
	public String putPassword(String accountname, byte[] charlist, int length) throws Exception {
		// Generate password and ciphertext
		Logger.log("Generating password...");
		String pass = crypto.generatePassword(charlist, length);
		byte[] ciphertext = crypto.encrypt(accountname, pass);
		// Attempt to create user account and then check login status
		Logger.log("Attempting to put password into blockchain...");
		TransactionReceipt tx_receipt = ppass.putPassword(crypto.getUserHash(), crypto.hashAccountName(accountname), ciphertext).send();
		Logger.log("Success! Tranaction hash: " + tx_receipt.getTransactionHash());
		// Return plaintext password
		return pass;
	}
	
	// Gets a password and account name in the form {accountname, password}
	public String[] getPassword(String accountname) throws Exception {
		// Get ciphertext and decrypt
		Logger.log("Getting password...");
		byte[] ciphertext = ppass.getPassword(crypto.getUserHash(), crypto.hashAccountName(accountname)).send();
		return crypto.decrypt(ciphertext);
	}
}
