package one.id0.ppass.backend;

import java.util.Base64;

// Wrapper class for C++ Cryptography backend
public class Crypto {
	// Constants
	public static final int PWHASHLEN = 8;
	public static final int USERHASHLEN = 32;
	public static final int ACCOUNTHASHLEN = 16;
	public static final int BLAKE2HASHLEN = 32;
	public static final int PRNGSTATESIZE = 32;
	public static final int CIPHERTEXTLEN = 160;

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
	static {
		System.loadLibrary("Crypto");
	}
	
	// Class variables.
	private byte[] userhash, masterkeyhash; // Username hash and hash of master key
	private final byte[] masterkey; // Master key (assigned in constructor)
	
	// Constructor - generates userhash, masterkey, and masterkeyhash
	public Crypto(String username, String password) throws Exception {
		userhash = blake2b(username.toLowerCase().getBytes(), USERHASHLEN);
		masterkey = generateKey(password, userhash);
		masterkeyhash = blake2b(masterkey, PWHASHLEN);
	}

	// Function to hash an account name
	public byte[] hashAccountName(String accountName) {
		byte[] accountNameHash = blake2b(accountName.toLowerCase().getBytes(), BLAKE2HASHLEN);
		byte[] concatenated = new byte[PWHASHLEN+USERHASHLEN+BLAKE2HASHLEN];
		System.arraycopy(masterkeyhash, 0, concatenated, 0, PWHASHLEN);
		System.arraycopy(userhash, 0, concatenated, BLAKE2HASHLEN, USERHASHLEN);
		System.arraycopy(accountNameHash, 0, concatenated, PWHASHLEN+USERHASHLEN, BLAKE2HASHLEN);
		return blake2b(concatenated, ACCOUNTHASHLEN);
	}
	
	// Functions to get the master key hash and username hash
	public byte[] getUserHash() {
		return userhash;
	}
	public byte[] getMasterKeyHash() {
		return masterkeyhash;
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
		return encryptMiscData(masterkey, plaintext);
	}
	public byte[] decryptMiscData(byte[] ciphertext) throws Exception {
		return decryptMiscData(masterkey, ciphertext);
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
			System.out.println("Key generation:");
			Crypto self = new Crypto("asdf", "asdfasdf");
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