#include "crypto.h"

/**** UTILITY FUNCTIONS ****/
// Wipes a memory region
void wipe(byte* ptr, size_t ptr_len) {
	memset(ptr, 0, ptr_len);
}

// Frees a pointer to a pointer to a memory region securely
void secureFree(byte** ptrptr, size_t ptr_len) {
	byte* ptr = *ptrptr;
	wipe(ptr, ptr_len);
	free(ptr);
	*ptrptr = NULL;
}

/**** COMPRESSION FUNCTIONS ****/

// Compresses an array in z827. We assume we have a valid compressed length.
void z827_compress(const char* plain, const size_t plain_len, byte* compressed) {
	// Perform first copy outside of loop because we can't have a negative index
	compressed[0] = plain[0];
	// Loop through everything else
	for (size_t i=1; i<plain_len; i++) {
		compressed[z827_compressed_len(i)-1] |= plain[i] << (8-i%8);
		compressed[z827_compressed_len(i)] = plain[i] >> (i%8);
	}
}

// Decompresses an array in z827. We assume we have a valid decompressed length.
void z827_decompress(const byte* compressed, const size_t compressed_len, char* plain) {
	// Perform first copy outside of loop because we can't have a negative index
	plain[0] = compressed[0] & 0x7F;
	// Loop through our indexes
	for (size_t i=1; i<z827_decompressed_len(compressed_len); i++) {
		plain[i] = (compressed[z827_compressed_len(i)-1] >> (8-i%8)) | ((compressed[z827_compressed_len(i)] << (i%8)) & 0x7F);
	}
}

/**** SERIALIZATION FUNCTIONS ****/

// Generates a plaintext from account name and password. Plaintext must be PLAINTEXTLEN, and accname and password must be below their maximum lengths.
// Note that padding is not included in ACCOUNTNAMEMAXLEN or PASSWORDMAXLEN, so we have to add 1 to the maximum to get the padded length.
void generatePlaintext(const byte* accname, const size_t accname_len, const char* password, const size_t password_len, byte* plaintext) {
	// First, compress password
	size_t password_compressed_len = z827_compressed_len(password_len);
	byte* password_compressed = (byte*)malloc(password_compressed_len);
	z827_compress(password, password_len, password_compressed);

	// Get both plaintext outputs and padding sizes
	byte* accname_plaintext = plaintext;
	byte* password_plaintext = plaintext+ACCOUNTNAMEMAXLEN+1;
	uint8_t accname_pad_size = (uint8_t)((ACCOUNTNAMEMAXLEN+1)-accname_len);
	uint8_t password_pad_size = (uint8_t)((PASSWORDCOMPRESSEDMAXLEN+1)-password_compressed_len);

	// Write plaintext and padding
	memcpy(accname_plaintext, accname, accname_len);
	memset(accname_plaintext+accname_len, accname_pad_size, accname_pad_size);
	memcpy(password_plaintext, password_compressed, password_compressed_len);
	memset(password_plaintext+password_compressed_len, password_pad_size, password_pad_size);

	// Wipe compressed password
	secureFree(&password_compressed, password_compressed_len);
}

// Function to deserialize generated plaintext back to account name and password.
struct accountname_and_password deserializePlaintext(const byte* plaintext) {
	// First, get password and account name individually
	const byte* accname_padded = plaintext;
	size_t accname_padded_len = ACCOUNTNAMEMAXLEN+1;
	const byte* password_padded = plaintext+accname_padded_len;
	size_t password_padded_len = PASSWORDCOMPRESSEDMAXLEN+1;

	// Get length of each without padding
	size_t password_compressed_len = password_padded_len-password_padded[password_padded_len-1];
	size_t accname_len = accname_padded_len-accname_padded[accname_padded_len-1];

	// Decompress password
	size_t password_len = z827_decompressed_len(password_compressed_len);
	char* password = (char*)malloc(password_len);
	z827_decompress(password_padded, password_compressed_len, password);

	// Generate null-terminated output struct (null terminators not included in length)
	struct accountname_and_password out;
	out.accname = (byte*)malloc(accname_len+1);
	out.accname_len = accname_len;
	out.password = (char*)malloc(password_len+1);
	out.password_len = password_len;
	memcpy(out.accname, accname_padded, accname_len);
	memcpy(out.password, password, password_len);
	out.accname[accname_len] = '\0';
	out.password[password_len] = '\0';

	// Wipe password
	secureFree((byte**)(&password), password_len);

	// Return it
	return out;
}

/**** SYMMETRIC ENCRYPTION FUNCTIONS ****/

// Wrapper around tweetnacl encryption
void tweetnacl_wrapper_encrypt(const byte* ptraw, const size_t ptrawlen, const byte* nonce, const byte* key, byte* out) {
	// Get lengths
	size_t ptlen = ptrawlen+crypto_secretbox_ZEROBYTES;
	size_t ctrawlen = ptrawlen+HMACLEN;
	// Allocate buffers, copy bytes in, encrypt, copy encrypted bytes out
	byte* pt = (byte*)calloc(1, ptlen);
	byte* ct = (byte*)calloc(1, ptlen);
	memcpy(pt+crypto_secretbox_ZEROBYTES, ptraw, ptrawlen);
	crypto_secretbox(ct, pt, ptlen, nonce, key);
	memcpy(out, ct+crypto_secretbox_BOXZEROBYTES, ctrawlen);
	// Wipe and return
	secureFree(&pt, ptlen);
	secureFree(&ct, ptlen);
}

// Wrapper around tweetnacl decryption
int tweetnacl_wrapper_decrypt(const byte* ctraw, const size_t ctrawlen, const byte* nonce, const byte* key, byte* out) {
	// Get lengths
	size_t ctlen = ctrawlen+crypto_secretbox_BOXZEROBYTES;
	size_t ptrawlen = ctrawlen-HMACLEN;
	// Allocate buffers, copy encrypted bytes in, decrypt, copy bytes out
	byte* pt = (byte*)calloc(1, ctlen);
	byte* ct = (byte*)calloc(1, ctlen);
	memcpy(ct+crypto_secretbox_BOXZEROBYTES, ctraw, ctrawlen);
	int result = crypto_secretbox_open(pt, ct, ctlen, nonce, key);
	memcpy(out, pt+crypto_secretbox_ZEROBYTES, ptrawlen);
	// Wipe and return
	secureFree(&pt, ctlen);
	secureFree(&ct, ctlen);
	return result;
}

// Encrypts a plaintext using a single block cipher function. Output and plaintext must both be CIPHERTEXTWITHOUTSEEDLEN.
void encryptOneBlockCipher(CryptoPP::CipherModeDocumentation::Encryption *encryptor, const byte* plaintextWithNonce, const byte* key, byte* output) {
	// Set up input. Our plaintext is everything after byte 16 (the last 8 bytes of the xsalsa20 nonce, the HMAC, and the encrypted plaintext).
	const byte* nonce = plaintextWithNonce;
	const byte* plaintext = plaintextWithNonce+16;
	// Set up output. Our IV is everything before byte 16 (the first 16 bytes of the xsalsa20 nonce).
	byte* ciphertextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	byte* iv = ciphertextfull;
	byte* ciphertext = iv+IVLEN;
	memcpy(iv, nonce, IVLEN);
	// Run encryption without padding
	encryptor->SetKeyWithIV(key, LOCALKEYLEN, iv, IVLEN);
	CryptoPP::ArraySource(plaintext, BLOCKTEXTLEN, true,
		new CryptoPP::StreamTransformationFilter( *encryptor,
			new CryptoPP::ArraySink(ciphertext, BLOCKTEXTLEN),
			CryptoPP::StreamTransformationFilter::NO_PADDING
		)
	);
	// Copy ciphertextfull over to output and free
	memcpy(output, ciphertextfull, CIPHERTEXTWITHOUTSEEDLEN);
	secureFree(&ciphertextfull, CIPHERTEXTWITHOUTSEEDLEN);
}

// Encrypts a plaintext using a master key. Plaintext must be PLAINTEXTLEN, output must be CIPHERTEXTLEN
void encrypt(const byte* plaintext, const byte* masterkey, byte* output) {
	/* Set up keys, seed, and nonce */
	// Generate random seed and nonce
	byte* tohash = (byte*)malloc(SEEDLEN+MASTERKEYLEN+1); // This is [seed || master key || number]
	byte* nonce = (byte*)malloc(NONCELEN);
	CryptoPP::OS_GenerateRandomBlock(false, tohash, SEEDLEN);
	CryptoPP::OS_GenerateRandomBlock(false, nonce, NONCELEN);
	// Copy over master key
	memcpy(tohash+SEEDLEN, masterkey, MASTERKEYLEN);
	// Generate local keys using master key, seed, and encryption algorithm number
	byte* keys[4] = {(byte*)malloc(LOCALKEYLEN), (byte*)malloc(LOCALKEYLEN), (byte*)malloc(LOCALKEYLEN), (byte*)malloc(LOCALKEYLEN)};
	for (uint8_t i=0; i<4; i++) {
		CryptoPP::SHA256 sha256;
		tohash[SEEDLEN+MASTERKEYLEN] = i;
		sha256.CalculateDigest(keys[i], tohash, SEEDLEN+MASTERKEYLEN+1);
	}

	/* Encrypt using XSalsa20-Poly1305 */
	// Set up output. Input is at plaintext. Our output (xsalsa20ciphertextfull) will be [nonce || hmac || ciphertext]
	byte* xsalsa20ciphertextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	byte* xsalsa20nonce = xsalsa20ciphertextfull;
	byte* xsalsa20hmacandct = xsalsa20nonce+NONCELEN;
	// Copy over nonce
	memcpy(xsalsa20nonce, nonce, NONCELEN);
	// Encrypt and generate HMAC
	tweetnacl_wrapper_encrypt(plaintext, PLAINTEXTLEN, xsalsa20nonce, keys[0], xsalsa20hmacandct);

	/* Encrypt using AES-CBC */
	// Run AES-CBC encryption
	byte* aesciphertextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	CryptoPP::CBC_Mode<CryptoPP::AES>::Encryption* aescbc = new CryptoPP::CBC_Mode<CryptoPP::AES>::Encryption();
	encryptOneBlockCipher(aescbc, xsalsa20ciphertextfull, keys[1], aesciphertextfull);
	delete aescbc;

	/* Encrypt using Twofish-CTR */
	// Run Twofish-CTR encryption
	byte* twofishciphertextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	CryptoPP::CTR_Mode<CryptoPP::Twofish>::Encryption* twofishctr = new CryptoPP::CTR_Mode<CryptoPP::Twofish>::Encryption();
	encryptOneBlockCipher(twofishctr, aesciphertextfull, keys[2], twofishciphertextfull);
	delete twofishctr;

	/* Encrypt using Serpent-CBC */
	// Run Serpent-CBC encryption
	byte* serpentciphertextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	CryptoPP::CBC_Mode<CryptoPP::Serpent>::Encryption* serpentcbc = new CryptoPP::CBC_Mode<CryptoPP::Serpent>::Encryption();
	encryptOneBlockCipher(serpentcbc, twofishciphertextfull, keys[3], serpentciphertextfull);
	delete serpentcbc;

	/* Output and Cleanup */
	// Copy over seed and ciphertext to output
	memcpy(output, tohash, SEEDLEN);
	memcpy(output+SEEDLEN, serpentciphertextfull, CIPHERTEXTWITHOUTSEEDLEN);
	// Wipe everything
	secureFree(&tohash, SEEDLEN+MASTERKEYLEN+1);
	secureFree(&nonce, NONCELEN);
	for (uint8_t i=0; i<4; i++) {
		secureFree(&(keys[i]), LOCALKEYLEN);
	}
	secureFree(&xsalsa20ciphertextfull, CIPHERTEXTWITHOUTSEEDLEN);
	secureFree(&aesciphertextfull, CIPHERTEXTWITHOUTSEEDLEN);
	secureFree(&twofishciphertextfull, CIPHERTEXTWITHOUTSEEDLEN);
	secureFree(&serpentciphertextfull, CIPHERTEXTWITHOUTSEEDLEN);
}

// Decrypts a ciphertext using a single block cipher function. Output and ciphertext must both be CIPHERTEXTWITHOUTSEEDLEN.
void decryptOneBlockCipher(CryptoPP::CipherModeDocumentation::Decryption *decryptor, const byte* ciphertextWithNonce, const byte* key, byte* output) {
	// Set up output
	byte* plaintextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	byte* iv = plaintextfull;
	byte* plaintext = iv+IVLEN;
	// Set up input
	memcpy(iv, ciphertextWithNonce, IVLEN);
	const byte* ciphertext = ciphertextWithNonce+IVLEN;
	// Decrypt with Serpent
	decryptor->SetKeyWithIV(key, LOCALKEYLEN, iv, IVLEN);
	CryptoPP::ArraySource(ciphertext, BLOCKTEXTLEN, true,
		new CryptoPP::StreamTransformationFilter( *decryptor,
			new CryptoPP::ArraySink(plaintext, BLOCKTEXTLEN),
			CryptoPP::StreamTransformationFilter::NO_PADDING
		)
	);
	// Copy over output and free
	memcpy(output, plaintextfull, CIPHERTEXTWITHOUTSEEDLEN);
	secureFree(&plaintextfull, CIPHERTEXTWITHOUTSEEDLEN);
}

// Decrypts a plaintext using a master key. Input must CIPHERTEXTLEN, output must be PLAINTEXTLEN. Returns false on failure, true on success.
bool decrypt(const byte* ciphertext, const byte* masterkey, byte* output) {
	/* Set up keys, seed, and nonce */
	// Get seed and copy over to tohash 
	const byte* seed = ciphertext;
	const byte* ciphertextWithoutSeed = ciphertext+SEEDLEN;
	byte* tohash = (byte*)malloc(SEEDLEN+MASTERKEYLEN+1); // This is [seed || master key || number]
	memcpy(tohash, seed, SEEDLEN);
	// Copy over master key
	memcpy(tohash+SEEDLEN, masterkey, MASTERKEYLEN);
	// Generate local keys using master key, seed, and encryption algorithm number
	byte* keys[4] = {(byte*)malloc(LOCALKEYLEN), (byte*)malloc(LOCALKEYLEN), (byte*)malloc(LOCALKEYLEN), (byte*)malloc(LOCALKEYLEN)};
	for (uint8_t i=0; i<4; i++) {
		CryptoPP::SHA256 sha256;
		tohash[SEEDLEN+MASTERKEYLEN] = i;
		sha256.CalculateDigest(keys[i], tohash, SEEDLEN+MASTERKEYLEN+1);
	}

	/* Decrypt using Serpent-CBC */
	// Run Serpent-CBC decryption
	byte* serpentplaintextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	CryptoPP::CBC_Mode<CryptoPP::Serpent>::Decryption* serpentcbc = new CryptoPP::CBC_Mode<CryptoPP::Serpent>::Decryption();
	decryptOneBlockCipher(serpentcbc, ciphertextWithoutSeed, keys[3], serpentplaintextfull);
	delete serpentcbc;

	/* Decrypt using Twofish-CTR */
	// Run Twofish-CTR decryption
	byte* twofishplaintextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	CryptoPP::CTR_Mode<CryptoPP::Twofish>::Decryption* twofishctr = new CryptoPP::CTR_Mode<CryptoPP::Twofish>::Decryption();
	decryptOneBlockCipher(twofishctr, serpentplaintextfull, keys[2], twofishplaintextfull);
	delete twofishctr;

	/* Decrypt using AES-CBC */
	// Run AES-CBC decryption
	byte* aesplaintextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	CryptoPP::CBC_Mode<CryptoPP::AES>::Decryption* aescbc = new CryptoPP::CBC_Mode<CryptoPP::AES>::Decryption();
	decryptOneBlockCipher(aescbc, twofishplaintextfull, keys[1], aesplaintextfull);
	delete aescbc;

	/* Decrypt using XSalsa20-Poly1305 */
	// Set up output
	byte* plaintext = (byte*)malloc(PLAINTEXTLEN);
	// Set up input
	byte* xsalsa20nonce = aesplaintextfull;
	byte* xsalsa20hmacandct = xsalsa20nonce+NONCELEN;
	// Decrypt with XSalsa20
	int result = tweetnacl_wrapper_decrypt(xsalsa20hmacandct, PLAINTEXTLEN+HMACLEN, xsalsa20nonce, keys[0], plaintext); 

	/* Output, Cleanup, and Return */
	// Copy output
	memcpy(output, plaintext, PLAINTEXTLEN);
	// Check if result was success and store it in hmacs_equal
	bool hmacs_equal = (result == 0);
	// Wipe everything
	secureFree(&tohash, SEEDLEN+MASTERKEYLEN+1);
	for (uint8_t i=0; i<4; i++) {
		secureFree(&(keys[i]), LOCALKEYLEN);
	}
	secureFree(&serpentplaintextfull, CIPHERTEXTWITHOUTSEEDLEN);
	secureFree(&twofishplaintextfull, CIPHERTEXTWITHOUTSEEDLEN);
	secureFree(&aesplaintextfull, CIPHERTEXTWITHOUTSEEDLEN);
	secureFree(&plaintext, PLAINTEXTLEN);
	// Return
	return hmacs_equal;
}

// Encrypts/Decrypts a byte array with the one-time pad. Note that the one-time pad is a symmetric operation, so we don't need
// an additional function for decryption. This should ONLY be used when either plaintext or key (or both) are random.
void one_time_pad(byte* ptorct, byte* key, byte* out, size_t len) {
	for (size_t i=0; i<len; i++) {
		out[i] = ptorct[i] ^ key[i];
	}
}

/**** HASHING FUNCTIONS ****/

// Hash is blake(scrypt(k) || argon2(k))
// Credential struct for hashing functions (we need this for pthread)
struct credentials {
	const char* pass;
	size_t pass_len;
	const byte* salt;
	size_t salt_len;
	byte* out;
};

// Runs argon2. Takes in pointer to credential struct.
void argon2_hash_part(void* creds_ptr) {
	struct credentials* creds = (struct credentials*)creds_ptr;
	int e = argon2id_hash_raw(4, 18, 1, creds->pass, creds->pass_len, creds->salt, creds->salt_len, creds->out, HASH_PART_LEN);
	if (e) {
		printf("%s\n", argon2_error_message(e));
	};
}

// Runs scrypt. Takes in pointer to credential struct.
void* scrypt_hash_part(void* creds_ptr) {
	struct credentials* creds = (struct credentials*)creds_ptr;
	scrypt((const byte*)creds->pass, creds->pass_len, creds->salt, creds->salt_len, 18, 4, 0, creds->out, HASH_PART_LEN);
	return NULL; // Nothing here... We don't need to return
}

// Runs double hash on a password. Returns false on threading error.
bool double_hash(const char* pass, const size_t pass_len, const byte* salt, const size_t salt_len, byte* out) {
	// Output hashes (to hash into out)
	byte* hashes = (byte*)malloc(HASH_FULL_LEN);
	// Credentials (and outputs) for hash functions
	struct credentials scrypt_creds, argon2_creds;
	scrypt_creds.pass = argon2_creds.pass = pass;
	scrypt_creds.pass_len = argon2_creds.pass_len = pass_len;
	scrypt_creds.salt = argon2_creds.salt = salt;
	scrypt_creds.salt_len = argon2_creds.salt_len = salt_len;
	scrypt_creds.out = hashes;
	argon2_creds.out = hashes+HASH_PART_LEN;

	// Spawn scrypt thread in background
	pthread_t scrypt_thread;
	if (pthread_create(&scrypt_thread, NULL, scrypt_hash_part, &scrypt_creds)) {
		puts("Error spawning threads");
		return false;
	}
	// Run argon2 in foreground thread
	argon2_hash_part(&argon2_creds);
	// Join with scrypt thread
	if (pthread_join(scrypt_thread, NULL)) {
		puts("Error joining threads");
		return false;
	}

	// Run Scrypt one last time to decrease length
	scrypt(hashes, HASH_FULL_LEN, NULL, 0, 1, 0, 0, out, HASH_DONE_LEN);
	// Free everything and return
	secureFree(&hashes, HASH_FULL_LEN);
	return true;
}

#ifdef TEST_CPP
// Test function for C++
int main() {
	// Test params
	const byte accname[] = "asdfasdg";
	const char password[] = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
	const byte username[] = "someuser1123";
	// KDF
	puts("Running KDF...");
	byte* key = (byte*)malloc(HASH_DONE_LEN); // This is larger now!
	if (!double_hash(password, strlen(password), username, strlen((const char*)username), key)) {
		puts("Error spawning pthread");
	}
	for (uint16_t i=0; i<MASTERKEYLEN; i++) {
		printf("%.02x", key[i]);
	}
	puts("");
	// Serialization and encrypting
	puts("Encrypting");
	byte* plaintext = (byte*)malloc(PLAINTEXTLEN);
	generatePlaintext(accname, strlen((const char*)accname), password, strlen(password), plaintext);
	byte* ct = (byte*)malloc(CIPHERTEXTLEN);
	encrypt(plaintext, key, ct);
	// Deserialization and decrypting
	puts("Decrypting");
	byte* recovered = (byte*)malloc(PLAINTEXTLEN);
	if (decrypt(ct, key, recovered)) {
		struct accountname_and_password stuff = deserializePlaintext(recovered);
		printf("%.*s:%.*s\n", (int)stuff.accname_len, stuff.accname, (int)stuff.password_len, stuff.password);
	} else {
		puts("Invalid HMAC");
	}
}
#endif