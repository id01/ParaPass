package one.id0.ppass.backend;

import java.math.BigInteger;
import java.util.Base64;

import org.web3j.crypto.ECKeyPair;

import cz.adamh.utils.NativeUtils;

// Wrapper class for C++ Cryptography backend
public class Crypto {
	// Constants
	public static final int PPASSHASHLEN = 8;
	public static final int USERHASHLEN = 32;
	public static final int ACCOUNTHASHLEN = 16;
	public static final int BLAKE2HASHLEN = 32;
	public static final int PRNGSTATESIZE = 32;
	public static final int CIPHERTEXTLEN = 160;
	public static final int ETHERPUBKEYLEN = 512/8;
	public static final int ETHERPRIVKEYLEN = 256/8;

	// Native methods
	private native byte[] blake2b(byte[] javaTohash, int digest_len);
	private native void generateRandomBytes(byte[] javaBytes);
	private native byte[] generateKey(String javaPassword, byte[] javaUserhash) throws Exception;
	private native int returnMasterKeyLength();
	private native byte[] encryptAccountPassword(byte[] javaMasterKey, String javaAccount, String javaPassword) throws Exception;
	private native String[] decryptAccountPassword(byte[] javaMasterKey, byte[] javaCiphertext) throws Exception;
	private native String generateRandomPassword(byte[] javaCharlist, int javaLength);
	private native byte[] encryptMiscData(byte[] javaMasterKey, byte[] javaPlaintext);
	private native byte[] decryptMiscData(byte[] javaMasterKey, byte[] javaCiphertextFull) throws Exception;
	private native byte[] oneTimePad(byte[] javaPlaintext, byte[] javaKey) throws Exception;
	// Load the native crypto library from jar or path
	static {
		try {
			NativeUtils.loadLibraryFromJar("/lib/libCrypto.so");
		} catch (Exception e) {
			try {
				System.loadLibrary("Crypto");
			} catch (Exception ee) {
				System.out.println("Error on path library load: " + ee.getMessage());
				System.out.println();
				System.out.println("Error on jar library load: " + e.getMessage());
			}
		}
	}
	
	// Class variables.
	private byte[] userhash, ppassFileHash; // Username hash and hash of decrypted PPassFile content
	private final byte[] masterkey, keystorekey, misckey; // Master key, keystore key, misc key (assigned in constructor)
	
	// Constructor - gets userhash, masterkey, and masterkeyhash from username, password, and PPass file content
	// File content should be 4 bytes (for the version of the file) followed by 128 random bytes.
	// The random bytes are decrypted to become the master key (first 64), keystore key (next 32), and misc key (last 32)
	public Crypto(String username, String password, int version, byte[] contentBytes) throws Exception {
		// Get username hash
		userhash = blake2b(username.toLowerCase().getBytes(), USERHASHLEN);
		// Generate file key
		byte[] fileKey = generateKey(password, userhash);
		// Run one time pad and split output
		byte[] decryptedBytes = oneTimePad(contentBytes, fileKey);
		byte[] masterkey = new byte[64];
		byte[] keystorekey = new byte[32];
		byte[] misckey = new byte[32];
		System.arraycopy(masterkey, 0, decryptedBytes, 0, 64);
		System.arraycopy(keystorekey, 0, decryptedBytes, 64, 32);
		System.arraycopy(misckey, 0, decryptedBytes, 64+32, 32);
		// Copy over output
		this.masterkey = masterkey;
		this.keystorekey = keystorekey;
		this.misckey = misckey;
		// Generate hash of file
		ppassFileHash = blake2b(decryptedBytes, PPASSHASHLEN);
	}
	
	// Constructor - using existing misckey for misc data encryption/decryption only, or a null misckey for utility functions
	public Crypto(byte[] misckey) {
		userhash = blake2b(new byte[1], USERHASHLEN);
		if (misckey != null) {
			this.misckey = misckey;
			ppassFileHash = blake2b(misckey, PPASSHASHLEN);
		} else {
			this.misckey = null;
			ppassFileHash = null;
		}
		masterkey = null;
		keystorekey = null;
	}

	// Function to hash an account name
	public byte[] hashAccountName(String accountName) {
		byte[] accountNameHash = blake2b(accountName.toLowerCase().getBytes(), BLAKE2HASHLEN);
		byte[] concatenated = new byte[PPASSHASHLEN+USERHASHLEN+BLAKE2HASHLEN];
		System.arraycopy(ppassFileHash, 0, concatenated, 0, PPASSHASHLEN);
		System.arraycopy(userhash, 0, concatenated, BLAKE2HASHLEN, USERHASHLEN);
		System.arraycopy(accountNameHash, 0, concatenated, PPASSHASHLEN+USERHASHLEN, BLAKE2HASHLEN);
		return blake2b(concatenated, ACCOUNTHASHLEN);
	}
	
	// Functions to get the master key hash, username hash, and generic hash
	public byte[] getUserHash() {
		return userhash;
	}
	public byte[] getMasterKeyHash() {
		return ppassFileHash;
	}
	public byte[] getGenericHash(byte[] tohash, int digest_len) {
		return blake2b(tohash, digest_len);
	}

	// Function to generate a password
	public String generatePassword(byte[] charlist, int length) {
		return generateRandomPassword(charlist, length);
	}
	
	// Functions to encrypt and decrypt account name and password
	public byte[] encrypt(String accountName, String accountPassword) throws Exception {
		return encryptAccountPassword(masterkey, accountName.toLowerCase(), accountPassword);
	}
	public String[] decrypt(byte[] ciphertext) throws Exception {
		return decryptAccountPassword(masterkey, ciphertext);
	}
	
	// Functions to encrypt and decrypt misc data
	public byte[] encryptMiscData(byte[] plaintext) {
		return encryptMiscData(misckey, plaintext);
	}
	public byte[] decryptMiscData(byte[] ciphertext) throws Exception {
		return decryptMiscData(misckey, ciphertext);
	}
	
	// Function to generate a random byteArray of the specified length
	public byte[] generateRandomBytes(int length) {
		byte[] bytes = new byte[length];
		generateRandomBytes(bytes);
		return bytes;
	}
	
	// Function to encrypt an ECKeyPair object
	public byte[] encryptEtherKeys(ECKeyPair keyPair) {
		// Get public and private keys as byte arrays
		byte[] pubkeyBytes = keyPair.getPublicKey().toByteArray();
		byte[] privkeyBytes = keyPair.getPrivateKey().toByteArray();
		// Combine the byte arrays into the form [pubkey | privkey], keeping in mind that there may be leading zeroes due to the sign bit
		// Note: BigInteger.toByteArray() returns a big-endian byte array
		byte[] keypairBytes = new byte[ETHERPUBKEYLEN+ETHERPRIVKEYLEN];
		System.arraycopy(pubkeyBytes, pubkeyBytes.length-ETHERPUBKEYLEN, keypairBytes, 0, ETHERPUBKEYLEN);
		System.arraycopy(privkeyBytes, privkeyBytes.length-ETHERPRIVKEYLEN, keypairBytes, ETHERPUBKEYLEN, ETHERPRIVKEYLEN);
		// Encrypt using keystorekey and return
		return encryptMiscData(keystorekey, keypairBytes);
	}
	
	// Function to decrypt encrypted ethereum keys into an ECKeyPair object
	public ECKeyPair decryptEtherKeys(byte[] encryptedKeyPair) throws Exception {
		// Decrypt encrypted key pair into byte array
		byte[] keypairBytes = decryptMiscData(keystorekey, encryptedKeyPair);
		// Split into byte arrays for public and private keys (adding a leading byte to fill the big-endian sign bit)
		byte[] pubkeyBytes = new byte[ETHERPUBKEYLEN+1];
		byte[] privkeyBytes = new byte[ETHERPRIVKEYLEN+1];
		System.arraycopy(keypairBytes, 0, pubkeyBytes, 1, ETHERPUBKEYLEN);
		System.arraycopy(keypairBytes, ETHERPUBKEYLEN, privkeyBytes, 1, ETHERPRIVKEYLEN);
		// Create ECKeyPair and return
		return new ECKeyPair(new BigInteger(privkeyBytes), new BigInteger(pubkeyBytes));
	}
	
	// Destructor
	protected void finalize() throws Throwable {
		// Nuke master key
		for (int i=0; i<masterkey.length; i++) {
			masterkey[i] = 0;
		}
		// Call regular finalize function
		super.finalize();
	}

	// Main test function
	public static void main(String [] args) {
		try {
			// Keygen and encryptiuon test
			byte[] keyfiledata = new byte[132]; // Initialize to all nulls
			keyfiledata[3] = 1; // Set version to 1
			System.out.println("Key generation:");
			Crypto self = new Crypto("asdf", "asdfasdf", 1, keyfiledata);
			byte[] masterkey = self.generateKey("aasdfsdf", self.getUserHash());
			byte[] acchash = self.hashAccountName("stuff");
			byte[] ct = self.encrypt("aasdfsdf", "qwasdfer");
			String[] pt = self.decrypt(ct);
			Base64.Encoder b64e = Base64.getEncoder();
			System.out.println("Master key: " + b64e.encodeToString(masterkey));
			System.out.println("Decrypted Plaintext: " + pt[0] + ":" + pt[1]);
			System.out.println("Key hash: " + b64e.encodeToString(self.getMasterKeyHash()));
			System.out.println("Account name hash: " + b64e.encodeToString(acchash));
			// Rng text
			String[] passwords = new String[128];
			for (int i=0; i<passwords.length; i++) {
				passwords[i] = self.generatePassword("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ+/".getBytes(), 128);
			}
			System.out.println("RNG test: ");
			for (String password: passwords) {
				System.out.print(password);
			}
			System.out.println();
			// Misc encryption test
			System.out.println("Misc data encryption test:");
			byte[] encryptedData = self.encryptMiscData("This is some misc data that can be of any length.".getBytes());
			byte[] decryptedData = self.decryptMiscData(encryptedData);
			System.out.println(b64e.encodeToString(decryptedData));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}