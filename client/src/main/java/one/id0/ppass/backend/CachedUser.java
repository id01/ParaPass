package one.id0.ppass.backend;

import java.util.ArrayList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

import one.id0.ppass.gen.PPassNetwork;
import one.id0.ppass.utils.CRC64;
import one.id0.ppass.utils.UserAccount;
import one.id0.ppass.utils.UserPassword;

public class CachedUser extends User {
	// SQL table structures.

	// This table stores account-specific information. It has 4 columns - an 8-byte user ID hash, an 8-byte account
	// ID hash, a variable-length description, a 64-bit last-accessed timestamp, and a 0/1 (boolean) integer with
	// whether or not the account is pinned.
	// Note that we can use 8-byte hashes because we expect each user to not have a ridiculous amount of passwords.
	private final String accountsTableStructure =
			"(uid INTEGER NOT NULL, aid INTEGER NOT NULL, desc BLOB, time INTEGER, pin INTEGER, UNIQUE (uid,aid))";
	// This table stores past passwords. It has 4 columns - an 8-byte user ID hash, an 8-byte account hash, a 64=bit
	// timestamp storing when the password was first seen (downloaded), a 64-bit hash of the encrypted password data,
	// and a blob storing encrypted password data. Everything should ideally be unique, but checking uniqueness of the
	// blob imposes extra overhead, so we're using the hash instead.
	private final String passwordsTableStructure =
			"(uid INTEGER NOT NULL, aid INTEGER NOT NULL, time INTEGER NOT NULL, pwhash INTEGER NOT NULL, data BLOB, UNIQUE (uid,aid,pwhash))";
	
	// Database information
	private Connection connection;
	private long uidhash;
	
	// Utility function to hash a UID or AID
	protected static long runCRC(byte[] input) {
		// Hash user ID and account ID with CRC64
		CRC64 hasher = new CRC64(); hasher.update(input);
		return hasher.getValue();
	}
	
	// Constructor
	public CachedUser(PPassNetwork ppass, String username, String masterpass, boolean createmode, Logger logger) throws Exception {
		// Initialize user
		super(ppass, username, masterpass, createmode, logger);
		// Initialize connection and create tables if necessary
		logger.log("Connecting to cache...");
		connection = DriverManager.getConnection("jdbc:sqlite:PPassCache.db");
		Statement createStatement = connection.createStatement();
		createStatement.executeUpdate("CREATE TABLE IF NOT EXISTS accounts " + accountsTableStructure);
		createStatement.executeUpdate("CREATE TABLE IF NOT EXISTS passwords " + passwordsTableStructure);
		// Create UID hash and copy over user
		uidhash = runCRC(super.getUserHash());
	}
	
	/**** OVERLOADED USER FUNCTIONS ****/
	// Puts a password and caches it too
	public String putPassword(String accountname, byte[] charlist, int length) throws Exception {
		byte[] ciphertext = new byte[Crypto.CIPHERTEXTLEN];
		String result = super.putPassword(accountname, charlist, length);
		addPastPassword(super.getAccountHash(accountname), ciphertext);
		return result;
	}
	
	// Gets a password and caches it too
	public String[] getPassword(byte[] accounthash) throws Exception {
		byte[] ciphertext = new byte[Crypto.CIPHERTEXTLEN];
		String[] result = super.getPassword(accounthash, ciphertext);
		addPastPassword(accounthash, ciphertext);
		return result;
	}
	
	/**** ACCOUNT FUNCTIONS ****/
	// Function to add new account. Throws and Exception if it already exists (duplicate key)
	public void cacheNewAccount(byte[] aid) throws SQLException {
		// Get account hash
		long aidhash = runCRC(aid);
		// Construct query statement and update
		PreparedStatement statement = connection.prepareStatement("INSERT INTO accounts (uid,aid,pin) VALUES (?,?,0)");
		statement.setLong(1, uidhash);
		statement.setLong(2, aidhash);
		statement.executeUpdate();
	}
	
	// Function to update access timestamp of an account. Thows exception if it doesn't exist
	public void updateAccessTimestamp(byte[] aid) throws SQLException {
		// Get account hash
		long aidhash = runCRC(aid);
		// Construct update statement and update
		PreparedStatement statement = connection.prepareStatement("UPDATE accounts SET time=? WHERE uid=? AND aid=?");
		statement.setLong(1, System.currentTimeMillis());
		statement.setLong(2, uidhash);
		statement.setLong(3, aidhash);
		statement.executeUpdate();
	}
	
	// Function to update an account's description
	public void updateDescription(byte[] aid, String description) throws SQLException {
		// Get account hash
		long aidhash = runCRC(aid);
		// Construct update statement and update
		PreparedStatement statement = connection.prepareStatement("UPDATE accounts SET desc=? WHERE uid=? AND aid=?");
		statement.setBytes(1, super.encryptAccountDescription(description));
		statement.setLong(2, uidhash);
		statement.setLong(3, aidhash);
		statement.executeUpdate();
	}
	
	// Function to update an account's pinned status
	public void updatePinnedStatus(byte[] aid, boolean pinnedStatus) throws SQLException {
		// Get account hash
		long aidhash = runCRC(aid);
		// Construct update statement and update
		PreparedStatement statement = connection.prepareStatement("UPDATE accounts SET pin=? WHERE uid=? AND aid=?");
		statement.setBoolean(1, pinnedStatus);
		statement.setLong(2, uidhash);
		statement.setLong(3, aidhash);
		statement.executeUpdate();
	}
	
	// Function to get a UserAccount object for an account ID and name. Throws an exception on SQL exception or 
	// row not found. If HMAC is invalid, description is set to "- description corrupted -"
	public UserAccount getUserAccount(byte[] aid, String accountName) throws Exception {
		// Get account hash
		long aidhash = runCRC(aid);
		// Construct query statement and query
		PreparedStatement statement = connection.prepareStatement("SELECT desc,time,pin FROM accounts WHERE uid=? AND aid=?");
		statement.setLong(1, uidhash);
		statement.setLong(2, aidhash);
		ResultSet rs = statement.executeQuery();
		// If a result exists, construct a UserAccount object of it
		if (rs.next()) {
			byte[] desc = rs.getBytes(1);
			long timestamp = rs.getLong(2);
			boolean pinned = (rs.getInt(3) > 0);
			if (desc == null) {
				// Description doesn't exist. Just set it to null
				return new UserAccount(aid, accountName, null, timestamp, pinned);
			} else {
				// Attempt to decrypt description. If if fails, set it to "- description corrupted -"
				try {
					String description = super.decryptAccountDescription(desc);
					return new UserAccount(aid, accountName, description, timestamp, pinned);
				} catch (Exception e) {
					if (e.getMessage().equals("Invalid HMAC!")) {
						return new UserAccount(aid, accountName, "- description corrupted -", timestamp, pinned);
					} else {
						throw e;
					}
				}
			}
		} else {
			throw new SQLException("Row not found!");
		}
	}
	
	/**** PAST PASSWORD FUNCTIONS ****/
	// Add a past password if it hasn't already been added. Takes in account ID and password ciphertext as inputs. 
	public boolean addPastPassword(byte[] aid, byte[] ciphertext) {
		// Get account hash and ciphertext hash
		long aidhash = runCRC(aid), cthash = runCRC(ciphertext);
		// Attempt to put password into database and return true. If it fails, it's probably because it's not unique. Return false.
		try {
			PreparedStatement stmt = connection.prepareStatement("INSERT INTO passwords (uid,aid,time,pwhash,data) VALUES (?,?,?,?,?)");
			stmt.setLong(1, uidhash);
			stmt.setLong(2, aidhash);
			stmt.setLong(3, System.currentTimeMillis());
			stmt.setLong(4, cthash);
			stmt.setBytes(5, ciphertext);
			stmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}
	
	// Get all past passwords from an account
	public ArrayList<UserPassword> getPastPasswords(byte[] aid) throws Exception {
		// Get account hash
		long aidhash = runCRC(aid);
		// Get timestamps and passwords from database sorted by timestamp and return them.
		PreparedStatement stmt = connection.prepareStatement("SELECT time,data FROM passwords WHERE uid=? AND aid=? ORDER BY TIME DESC");
		stmt.setLong(1, uidhash);
		stmt.setLong(2, aidhash);
		ResultSet rs = stmt.executeQuery();
		// Loop through results, decrypting them and adding them to ArrayList
		ArrayList<UserPassword> results = new ArrayList<UserPassword>();
		while (rs.next()) {
			String[] accnpass = super.crypto.decrypt(rs.getBytes(2));
			results.add(new UserPassword(rs.getLong(1), accnpass[0], accnpass[1]));
		}
		// Return results
		return results;
	}
}