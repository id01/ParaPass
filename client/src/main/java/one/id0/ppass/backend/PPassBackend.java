package one.id0.ppass.backend;

import java.util.Scanner;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

import one.id0.ppass.gen.PPassNetwork;
import one.id0.ppass.utils.UserAccount;
import one.id0.ppass.utils.UserPassword;

// Wrapper class for entire backend and CLI application
public class PPassBackend {
	// Constants
	final static int versionNum = 0;
	final static int pollInterval = 2000;
	final static int pollCount = 1000;

	// Class variables
	private Web3j web3;
	private Credentials credentials;
	private PPassNetwork ppass;
	private BigInteger gasPrice, maxGas;
	private CachedUser user;
	private Logger logger;

	// Connect constructor. Connects to or creates a PPassNetwork address, and also prepares other backends
	public PPassBackend(String password, String walletFile, String address, Logger logger) throws Exception {
		// Copy over logger
		this.logger = logger;
		// Set user to null for now
		user = null;
		// Set gas price and max gas
		gasPrice = BigInteger.valueOf(10000);
		maxGas = BigInteger.valueOf(5000000);
		// Initialize web3j
		logger.log("Initializing backends...");
		web3 = Web3j.build(new HttpService());
		// Try decrypting credentials
		try {
			credentials = WalletUtils.loadCredentials(password, walletFile);
		} catch (CipherException e) {
			throw new Exception("Couldn't decrypt keystore!");
		}
		System.out.println(credentials);
		// If address is null, we run the genesis constructor. Else, we run the connection constructor to that address.
		FastRawTransactionManager tx_mgr = new FastRawTransactionManager(web3, credentials,
				new PollingTransactionReceiptProcessor(web3, pollInterval, pollCount));
		if (address == null) {
			logger.log("Creating new ParaPass Network contract...");
			ppass = PPassNetwork.deploy(web3, tx_mgr, gasPrice, maxGas).send();
		} else {
			logger.log("Connecting to ParaPass Network instance at " + address + "...");
			ppass = PPassNetwork.load(address, web3, tx_mgr, gasPrice, maxGas);
		}
		// Check version of the ParaPass network we connected to
		if (ppass.getPPassNetworkVersion().send().intValue() != versionNum) {
			throw new IllegalStateException("Version mismatch!");
		}
		logger.log("Connected");
	}
	
	/**** Web3j wrappers ****/
	// Create a wallet file in the given directory with given password
	public static String createWalletFile(String keystoreDir, String password) throws Exception {
		String path = keystoreDir;
		String fileName = WalletUtils.generateLightNewWalletFile(password, new File(path));
		return path + fileName;
	}
	
	// Check whether wallet file can be decrypted with the given password
	public static boolean checkWalletFile(String pathToFile, String password) throws IOException {
		try {
			WalletUtils.loadCredentials(password, pathToFile);
			return true;
		} catch (CipherException e) {
			return false;
		}
	}

	// Get address of current PPassNetwork
	public String getContractAddress() {
		return ppass.getContractAddress();
	}
	
	// Get address of current wallet
	public String getWalletAddress() {
		return credentials.getAddress();
	}
	
	// Get balance of current wallet
	public BigInteger getBalance() {
		try {
			EthGetBalance bal = web3.ethGetBalance(getWalletAddress(),
					DefaultBlockParameterName.LATEST).send();
			return bal.getBalance();
		} catch (IOException e) {
			logger.logErr("IOException: " + e.getMessage());
			logger.logErr("Returning 0 for account balance");
			return BigInteger.valueOf(0);
		}
	}

	/**** User object wrappers ****/
	// Setup user account
	public void userSetup(String username, String masterpass) throws Exception {
		// Add user
		user = new CachedUser(ppass, username, masterpass, true, logger);
	}

	// Login as user
	public void userLogin(String username, String masterpass) throws Exception {
		// Login as user
		user = new CachedUser(ppass, username, masterpass, false, logger);
	}
	
	// Check if logged in. If not, throw an exception.
	public void checkLogin() throws Exception {
		if (user == null) {
			throw new Exception("Not logged in!");
		}
	}
	
	// Puts a password
	public String putPassword(String accountname, String charlist, int length) throws Exception {
		// Check login, put password
		checkLogin();
		return user.putPassword(accountname, charlist.getBytes(), length);
	}
	
	// Gets a password and account name with the account name
	public String[] getPassword(String accountname) throws Exception {
		// Check login, get password
		checkLogin();
		return user.getPassword(accountname);
	}
	
	// Gets a password and account name with an ID
	public String[] getPassword(byte[] accountID) throws Exception {
		// Check login, get password by ID
		checkLogin();
		return user.getPassword(accountID);
	}
	
	// Sets logger object
	public void setLogger(Logger logger) {
		this.logger = logger;
		this.user.setLogger(logger);
	}
	
	// Gets an account hash from a string
	public byte[] getAccountHash(String accountname) {
		return user.getAccountHash(accountname);
	}
	
	/**** CachedUser object wrappers ****/
	// Gets a UserAccount object from an account name
	public UserAccount getUserAccountObject(String accountname) throws Exception {
		// Create account in cache if it doesn't exist
		logger.log("Creating user account in cache if it doesn't exist...");
		byte[] aid = user.getAccountHash(accountname);
		try {
			user.cacheNewAccount(aid);
			logger.log("User account inserted into cache");
		} catch (Exception e) {
			logger.log("Account already exists!");
		}
		// Get account from cache (may throw exception)
		return user.getUserAccount(aid, accountname);
	}
	
	// Gets all accounts as UserAccount objects and cache them all (lol)
	public ArrayList<UserAccount> getAllAccounts() throws Exception {
		// Get all accounts for user
		ArrayList<byte[]> allAccountIDs = user.getAllAccounts();
		// Loop through them, getting UserAccount objects for all (with the side effect of caching them)
		ArrayList<UserAccount> allAccounts = new ArrayList<UserAccount>();
		for (byte[] accountID : allAccountIDs) {
			String accountName = user.getPassword(accountID)[0];
			allAccounts.add(getUserAccountObject(accountName));
		}
		return allAccounts;
	}
	
	// Updates access timestamp if updateAccessTimestamp is true, description if description
	// is not null, and pinned status if pinnedStatus is 0 (false) or more (true).
	public void updateAccountCache(byte[] aid, boolean updateAccessTimestamp, String description, int pinnedStatus) throws Exception {
		if (updateAccessTimestamp) {
			user.updateAccessTimestamp(aid);
		}
		if (description != null) {
			user.updateDescription(aid, description);
		}
		if (pinnedStatus >= 0) {
			if (pinnedStatus == 0) {
				user.updatePinnedStatus(aid, false);
			} else {
				user.updatePinnedStatus(aid, true);
			}
		}
	}
	
	// Gets an ArrayList of all the user's past passwords for an account cached in database
	public ArrayList<UserPassword> getPastPasswords(byte[] aid) throws Exception {
		return user.getPastPasswords(aid);
	}
	public ArrayList<UserPassword> getPastPasswords(String accountName) throws Exception {
		return getPastPasswords(user.getAccountHash(accountName));
	}

	// Main function
	public static void main(String[] args) {
		// Initialize logger
		Logger logger = new Logger();
		// Initialize PPassBackend object
		PPassBackend self = null;
		try {
			if (args.length == 3) {
				self = new PPassBackend(args[2], args[1], null, logger);
			} else if (args.length > 3) {
				self = new PPassBackend(args[2], args[1], args[3], logger);
			} else {
				System.out.println("Usage: ./run [walletFile] [walletPass] [contract address]");
			}
		} catch (Exception e) {
				throw new RuntimeException(e);
		}
		// Open shell
		Scanner sc = new Scanner(System.in);
		String[] line;
		while (true) {
			try {
				System.out.print("ParaPass > ");
				line = sc.nextLine().split(" ");
				if (line[0].equals("getinfo")) { // Gets information about stuff.
					System.out.println("Contract Address: " + self.getContractAddress());
					System.out.println("Wallet Address: " + self.getWalletAddress());
					System.out.println("Wallet Balance: " + self.getBalance());
				} else if (line[0].equals("setupuser")) { // Creates a user account. Usage: setupuser [username] [masterpass]
					if (line.length > 2) {
						self.userSetup(line[1], line[2]);
					} else {
						System.out.println("Usage: setupuser [username] [masterpass]");
					}
				} else if (line[0].equals("loginuser")) { // Logs a user in. Usage: loginuser [username] [masterpass]
					if (line.length > 2) {
						self.userLogin(line[1], line[2]);
					} else {
						System.out.println("Usage: loginuser [username] [masterpass]");
					}
				} else if (line[0].equals("putpassword")) { // Puts a password. Usage: putpassword [account] [charlist] [length]
					if (line.length > 3) {
						String passwordPut = self.putPassword(line[1], line[2], Integer.parseInt(line[3]));
						System.out.println("Password: " + passwordPut);
					} else {
						System.out.println("Usage: putpassword [account] [charlist] [length]");
					}
				} else if (line[0].equals("getpassword")) { // Gets a password. Usage: getpassword [account]
					if (line.length > 1) {
						String[] accountnamepassword = self.getPassword(line[1]);
						System.out.println("Account name: " + accountnamepassword[0]);
						System.out.println("Password: " + accountnamepassword[1]);
					} else {
						System.out.println("Usage: getpassword [account]");
					}
				} else if (line[0].equals("getallaccounts")) { // Gets all accounts. Usage: getallaccounts
					ArrayList<UserAccount> accounts = self.getAllAccounts();
					for (UserAccount account : accounts) {
						System.out.println(account.accountID + ": " + account.accountName);
					}
				} else if (line[0].equals("updatecache")) { // Updates the account data cache. Usage: updatecache 
															// [account] [description] [pinnedStatus] 
					if (line.length > 3) {
						self.updateAccountCache(self.getAccountHash(line[1]), true, line[2], Integer.parseInt(line[3]));
					} else {
						System.out.println("Usage: updatecache [account] [description] [pinnedStatus]");
					}
				} else if (line[0].equals("readcache")) { // Reads an account in the cache. Usage: readcache [account]
					if (line.length > 1) {
						UserAccount acc = self.getUserAccountObject(line[1]);
						System.out.println("Description: " + acc.description.getValue());
						System.out.println("Timestamp: " + acc.timestamp.getValue());
						System.out.println("Pinned: " + acc.pinned.getValue());
					} else {
						System.out.println("Usage: readcache [account]");
					}
				} else if (line[0].equals("getcachedpasswords")) { // Gets past passwords for an account that were cached
																   // in the database. Usage: getcachedpasswords [account]
					if (line.length > 1) {
						ArrayList<UserPassword> pastPasswords = self.getPastPasswords(line[1]);
						for (UserPassword pass : pastPasswords) {
							System.out.println("Account name: " + pass.accountName + ", password: " + pass.pass +
									", timestamp: " + pass.getTimestamp());
						}
					} else {
						System.out.println("Usage: getcachedpasswords [account]");
					}
				} else if (line[0].equals("exit")) {
					sc.close();
					System.exit(0);
				}
			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
			}
		}
	}

}
