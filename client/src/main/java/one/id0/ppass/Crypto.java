package one.id0.ppass;

import java.util.Base64;

public class Crypto {
	// Constants
	private final int PWHASHLEN = 8;
	private final int USERHASHLEN = 32;
	private final int ACCOUNTHASHLEN = 16;
	private final int BLAKE2HASHLEN = 32;
	private final int PRNGSTATESIZE = 32;

	// Native methods
	private native byte[] blake2b(byte[] javaTohash, int digest_len);
	private native void generateRandomBytes(byte[] javaBytes);
	private native byte[] generateKey(String javaPassword, byte[] javaUserhash) throws Exception;
	private native int returnMasterKeyLength();
	private native byte[] encryptAccountPassword(byte[] javaMasterKey, String javaAccount, String javaPassword) throws Exception;
	private native byte[] decryptAccountPassword(byte[] javaMasterKey, byte[] javaCiphertext) throws Exception;
	private native byte[] generateRandomPasswordRaw(byte[] javaCharlist, int length);
	static {
		System.loadLibrary("Crypto");
	}
	
	// Class variables.
	private byte[] userhash, masterkeyhash; // Username hash and hash of master key
	private final byte[] masterkey; // Master key (assigned in constructor)
	
	// Constructor - generates userhash, masterkey, and masterkeyhash
	public Crypto(String username, String password) throws Exception {
		userhash = blake2b(username.getBytes(), USERHASHLEN);
		masterkey = generateKey(password, userhash);
		masterkeyhash = blake2b(masterkey, PWHASHLEN);
	}

	// Function to hash an account name
	public byte[] hashAccountName(String accountName) {
		byte[] accountNameHash = blake2b(accountName.getBytes(), BLAKE2HASHLEN);
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
		return new String(generateRandomPasswordRaw(charlist, length));
	}
	
	// Functions to encrypt and decrypt account name and password
	public byte[] encrypt(String accountName, String accountPassword) throws Exception {
		return encryptAccountPassword(masterkey, accountName, accountPassword);
	}
	public String[] decrypt(byte[] ciphertext) throws Exception {
		// Get plaintext.
		byte[] plaintext = decryptAccountPassword(masterkey, ciphertext);
		// Loop through plaintext looking for null character separator
		byte[] accountnamebytes = null, passwordbytes = null;
		for (int i=0; i<plaintext.length; i++) {
			if (plaintext[i] == 0) { // Null byte. This is our separator.
				// Copy everything before the null to accountnamebytes, and everything after to passwordbytes, then break
				accountnamebytes = new byte[i];
				System.arraycopy(plaintext, 0, accountnamebytes, 0, i);
				passwordbytes = new byte[plaintext.length-i-1];
				System.arraycopy(plaintext, i+1, passwordbytes, 0, plaintext.length-i-1);
				break;
			}
		}
		// If we couldn't get either of the values, it means the plaintext didn't have a null separator (malformed)
		if (accountnamebytes == null || passwordbytes == null) {
			throw new IllegalArgumentException("Plaintext must have null byte as separator!");
		}
		// Return our String array.
		String[] toReturn = { new String(accountnamebytes), new String(passwordbytes) };
		return toReturn; 
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
			Crypto self = new Crypto("asdf", "asdfasdf");
			byte[] masterkey = self.generateKey("aasdfsdf", self.getUserHash());
			byte[] acchash = self.hashAccountName("stuff");
			byte[] ct = self.encryptAccountPassword(masterkey, "aasdfsdf", "qwasdfer");
			byte[] pt = self.decryptAccountPassword(masterkey, ct);
			String[] passwords = new String[128];
			for (int i=0; i<passwords.length; i++) {
				passwords[i] = self.generatePassword("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ+/".getBytes(), 128);
			}
			Base64.Encoder b64e = Base64.getEncoder();
			System.out.println("Decrypted Plaintext: " + b64e.encodeToString(pt));
			System.out.println("Key hash: " + b64e.encodeToString(self.getMasterKeyHash()));
			System.out.println("Account name hash: " + b64e.encodeToString(acchash));
			System.out.println("RNG test: ");
			for (String password: passwords) {
				System.out.print(password);
			}
			System.out.println();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}