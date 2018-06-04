package one.id0.ppass.backend;

import java.util.Scanner;
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

// Wrapper class for entire backend and CLI application
public class PPassBackend {
	// Constants
	final static int versionNum = 0;
	final static int pollInterval = 5000;
	final static int pollCount = 1000;

	// Class variables
	private Web3j web3;
	private Credentials credentials;
	private PPassNetwork ppass;
	private BigInteger gasPrice, maxGas;
	private User user;
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

	// Setup user account
	public void userSetup(String username, String masterpass) throws Exception {
		user = new User(ppass, username, masterpass, true, logger);
	}

	// Login as user
	public void userLogin(String username, String masterpass) throws Exception {
		user = new User(ppass, username, masterpass, false, logger);
	}
	
	// Puts a password
	public String putPassword(String accountname, String charlist, int length) throws Exception {
		// Check if we are logged in. If not, throw an exception
		if (user == null) {
			throw new Exception("Not logged in!");
		}
		// Put password
		return user.putPassword(accountname, charlist.getBytes(), length);
	}
	
	// Gets a password and account name
	public String[] getPassword(String accountname) throws Exception {
		// Check if we are logged in. If not, throw an exception
		if (user == null) {
			throw new Exception("Not logged in!");
		}
		// Get password
		return user.getPassword(accountname);
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
