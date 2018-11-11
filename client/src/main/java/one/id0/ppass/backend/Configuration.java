package one.id0.ppass.backend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Configuration {
	// Default configuration file information
	// Note: Database structures are stored in CachedUser, and many settings are stored in PPassBackend.
	// This is the handler for configuration files, NOT all configuration.
	// Array of URLs to default nodes
	public final static String[] DEFAULT_NODES = {"http://localhost:8545"};
	
	// Paths to configuration files
	public final static String CONFIG_DIR = System.getProperty("user.home") +
			File.separatorChar + ".ParaPass"; // Directory to store configuration files
	public final static String dbPATH = CONFIG_DIR + File.separatorChar + "PPassCache.db"; // Path to the cache database
	public final static String dbURI = "jdbc:sqlite:" + dbPATH; // URI to the cache database
	public final static String NODE_LIST_PATH = CONFIG_DIR +
			File.separatorChar + "PPassNodes.txt"; // Path to the ethereum node list
	
	// Creates the configuration directory and initializes everything
	public static void createConfig() throws IOException {
		// Create the configuration directory if it doesn't exist
		File configDir = new File(CONFIG_DIR);
		configDir.mkdir();
		// Initialize our configuration files if they don't exist
		try {
			// Initialize the database if it doesn't exist
			if (!new File(dbPATH).exists()) {
				Connection conn = DriverManager.getConnection(dbURI);
				CachedUser.initTables(conn);
				conn.close();
			}
		} catch (SQLException e) { // If this failed with an SQLException, I don't know what's wrong.
			throw new RuntimeException(e);
		}
		try {
			// Initialize the node list file if it doesn't exist
			if (!new File(NODE_LIST_PATH).exists()) {
				PrintWriter writer = new PrintWriter(new FileWriter(NODE_LIST_PATH));
				writer.write(DEFAULT_NODES[0] + "\n"); // Add the first default node as the last used one
				for (String node: DEFAULT_NODES) { // Loop through all default nodes, adding them to the file
					writer.write(node + "\n");
				}
				writer.close();
			}
		} catch (IOException e) { // If this failed, delete the node list file and throw our exception
			new File(NODE_LIST_PATH).delete();
			throw e;
		}
	}
}
