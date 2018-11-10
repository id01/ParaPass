package one.id0.ppass.server;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Base64;
import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD;
import org.json.*;

import one.id0.ppass.backend.Crypto;
import one.id0.ppass.backend.Logger;
import one.id0.ppass.backend.PPassBackend;

// HTTP Server. Runs on (HOST,PORT). All data sent and received are encrypted with XSalsa20-Poly1305 using the key in ServerKey.b64, and encoded in base64.
public class JSONServer extends NanoHTTPD implements PPassServer {
	// Class Variables
	private static final String HOST = "127.0.0.1";
	private static final int PORT = 9455;
	private static final byte STATUS_ERR = 1;
	private static final byte STATUS_SUCCESS = 0;
	private static final String SERVERKEYFILE = "ServerKey.b64";
	private PPassBackend backend;
	private Crypto serverCrypto; // Note: This is the Crypto class used for communications with clients (like the WebExtension), not for interacting with the chain
	
	// Constructor. All this does is call the super constructor because we need to have an interface method for initialization
	public JSONServer() {
		super(HOST, PORT);
	}
	
	// Function to initialize everything. Should be called right after the constructor.
	public void init(PPassBackend backend, boolean forceServerKeyChange, Logger logger) throws FileNotFoundException, IOException {
		// Read server key and initialize Crypto
		try {
			if (forceServerKeyChange) { // If we want to force a server key change, jump to the exception handler
				throw new FileNotFoundException("Force it!");
			}
			// Read key from file and create Crypto class
			logger.log("Attempting to get server key from file...");
			Scanner keyScanner = new Scanner(new File(SERVERKEYFILE));
			serverCrypto = new Crypto(Base64.getDecoder().decode(keyScanner.nextLine()));
			keyScanner.close();
		} catch (FileNotFoundException e) {
			// Create a new key and write it to ServerKey.
			logger.log("Creating new server key...");
			PrintWriter keyWriter = new PrintWriter(new File(SERVERKEYFILE));
			// Initialize a fake Crypto instance and get 64 random bytes as the server key
			Crypto fakeCrypto = new Crypto(null);
			byte[] key = fakeCrypto.generateRandomBytes(64);
			// Now that we have our key, write it to the file and create our real Crypto instance
			serverCrypto = new Crypto(key);
			keyWriter.write(Base64.getEncoder().encodeToString(key));
			keyWriter.close();
		}
		// Copy over backend class
		this.backend = backend;
		// Start HTTP server
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}
	
	// Create response with headers
	private Response createResponseWithHeaders(String data) {
		Response r = newFixedLengthResponse(data);
		r.addHeader("Content-Type", "text/plain");
		r.addHeader("Content-Security-Policy", "");
		return r;
	}
	
	// Create error message
	private Response createErrorMessage(String message) {
		// Create error JSON object response 
		JSONObject errmsgobj = new JSONObject();
		errmsgobj.put("status", STATUS_ERR);
		errmsgobj.put("message", message);
		// Encrypt, encode, and return
		return createResponseWithHeaders(Base64.getEncoder().encodeToString(
				serverCrypto.encryptMiscData(errmsgobj.toString().getBytes())
		));
	}
	
	// Create success response
	private Response createSuccessResponse(String message, String account, String password) {
		// Create success JSON object response with account name and password
		JSONObject responseobj = new JSONObject();
		responseobj.put("status", STATUS_SUCCESS);
		responseobj.put("message", message);
		responseobj.put("acc", account);
		responseobj.put("pass", password);
		// Encrypt, encode, and return
		return createResponseWithHeaders(Base64.getEncoder().encodeToString(
				serverCrypto.encryptMiscData(responseobj.toString().getBytes())
		));
	}
	
	// Server code
	@Override
	public Response serve(IHTTPSession session) {
		// Decrypt arguments
		byte[] argsjson = null;
		try {
			session.parseBody(new HashMap<String, String>());
			argsjson = serverCrypto.decryptMiscData(Base64.getDecoder().decode(session.getParms().get("args").getBytes()));
		} catch (Exception e) { // Decryption error. Return status error and description of error
			return createErrorMessage("Decryption Error. Make sure you have the right server key.");
		}
		try {
			// Get JSON arguments
			JSONObject args = new JSONObject(new String(argsjson));
			// Parse command and execute, respond to client
			String command = args.getString("command");
			if (command.equals("get")) {
				// Get account name argument and run get password
				String accName = args.getString("acc");
				String[] accnpass = backend.getPassword(accName);
				// Create success response with account name and password, then respond
				return createSuccessResponse("Success!", accnpass[0], accnpass[1]);
			} else if (command.equals("put")) {
				// Get account name argument, charlist argument, and length argument and run put password
				String accName = args.getString("acc");
				String charlist = args.getString("chrs");
				int passLength = args.getInt("len");
				String password = backend.putPassword(accName, charlist, passLength);
				// Create success response with account name and password, then respond
				return createSuccessResponse("Success!", accName, password);
			} else {
				return createErrorMessage("Command not recognized.");
			}
		} catch (Exception e) {
			return createErrorMessage(e.getMessage());
		}
	}
}
