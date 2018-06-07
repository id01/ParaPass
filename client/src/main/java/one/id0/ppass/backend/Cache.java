package one.id0.ppass.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

import one.id0.ppass.utils.CRC64;
import one.id0.ppass.utils.UserAccount;

public class Cache {
	// SQL table structure.
	// This table has 4 columns - an 8-byte user ID hash, an 8-byte account ID hash, a variable-length description,
	// a 64-bit last-accessed timestamp, and a 0/1 (boolean) integer with whether or not the account is pinned.
	// Note that we can use 8-byte hashes because we expect each user to not have a ridiculous amount of passwords.
	private final String accountsTableStructure =
			"(uid INTEGER NOT NULL, aid INTEGER NOT NULL, desc BLOB, time INTEGER, pin INTEGER, UNIQUE (uid,aid))";
	
	// Database information
	private Connection connection;
	private User user;
	private long uidhash;
	
	// Utility function to hash a UID or AID
	protected static long runCRC(byte[] input) {
		// Hash user ID and account ID with CRC64
		CRC64 hasher = new CRC64(); hasher.update(input);
		return hasher.getValue();
	}
	
	// Constructor
	public Cache(User user) throws SQLException {
		// Initialize connection and create table if necessary
		connection = DriverManager.getConnection("jdbc:sqlite:PPassCache.db");
		Statement createStatement = connection.createStatement();
		createStatement.executeUpdate("CREATE TABLE IF NOT EXISTS accounts " + accountsTableStructure);
		// Create UID hash and copy over user
		uidhash = runCRC(user.getUserHash());
		this.user = user;
	}
	
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
		statement.setBytes(1, user.encryptAccountDescription(description));
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
					String description = user.decryptAccountDescription(desc);
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
}
