// Standard libs
#include <stdlib.h>
#include <string.h>
#include <pthread.h>

// CryptoPP Algorithms
#include <cryptopp/aes.h>
#include <cryptopp/twofish.h>
#include <cryptopp/serpent.h>
#include <cryptopp/ccm.h>
#include <cryptopp/salsa.h>
#include <cryptopp/poly1305.h>
#include <cryptopp/blake2.h>
#include <cryptopp/sha.h>
#include <cryptopp/osrng.h>
#include <cryptopp/filters.h>
#include <cryptopp/cryptlib.h>

// Custom libs (kdfs)
#include "libs/scrypt-jane/scrypt-jane.h"
#include "libs/argon2/include/argon2.h"

// Let's make "unsigned char" less long
typedef unsigned char byte;

/**** UTILITY FUNCTIONS ****/
void wipe(byte* ptr, size_t ptr_len) {
	for (size_t i=0; i<ptr_len; i++) {
		ptr[i] = 0;
	}
}

void secureFree(byte** ptrptr, size_t ptr_len) {
	byte* ptr = *ptrptr;
	wipe(ptr, ptr_len);
	free(ptr);
	ptrptr = NULL;
}

/**** COMPRESSION FUNCTIONS ****/

// Get compressed/decompressed length with z827. Note that this is equivalent to Math.ceil(plain_len*7/8) and compressed_len*8/7
#define z827_compressed_len(plain_len) ((plain_len+1)*7/8)
#define z827_decompressed_len(compressed_len) (compressed_len*8/7)

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
		plain[i] = (compressed[z827_compressed_len(i)-1] >> (8-i%8)) | (compressed[z827_compressed_len(i)] << (i%8)) & 0x7F;
	}
}

/**** SERIALIZATION FUNCTIONS ****/

#define ACCOUNTNAMEMAXLEN 46
#define PASSWORDCOMPRESSEDMAXLEN 56
#define PASSWORDMAXLEN (PASSWORDCOMPRESSEDMAXLEN*8/7)

struct accountname_and_password {
	byte* accname;
	size_t accname_len;
	char* password;
	size_t password_len;
};

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

	// Generate output struct
	struct accountname_and_password out;
	out.accname = (byte*)malloc(accname_len);
	out.accname_len = accname_len;
	out.password = (char*)malloc(password_len);
	out.password_len = password_len;
	memcpy(out.accname, accname_padded, accname_len);
	memcpy(out.password, password, password_len);

	// Wipe password
	secureFree((byte**)(&password), password_len);

	// Return it
	return out;
}

/**** SYMMETRIC QUADRUPLE ENCRYPTION FUNCTIONS ****/

#define SEEDLEN 16
#define NONCELEN 24
#define IVLEN 16
#define HMACLEN 16
#define CIPHERTEXTLEN 160
#define CIPHERTEXTWITHOUTSEEDLEN (CIPHERTEXTLEN-SEEDLEN)
#define BLOCKTEXTLEN (CIPHERTEXTWITHOUTSEEDLEN-IVLEN)
#define MASTERKEYLEN 64
#define LOCALKEYLEN 32
#define PLAINTEXTLEN (CIPHERTEXTLEN-SEEDLEN-NONCELEN-HMACLEN) // Note: This must be less than 256 for padding to work
// Plaintext is [47-byte account name, after padding] [57-byte compressed password, after padding]
// Ciphertext is [16 byte seed] [24 byte nonce] [plaintext encrypted] [16 byte hmac]

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
	byte* xsalsa20hmac = xsalsa20nonce+NONCELEN;
	byte* xsalsa20ciphertext = xsalsa20hmac+HMACLEN;
	// Copy over nonce
	memcpy(xsalsa20nonce, nonce, NONCELEN);
	// Generate HMAC for plaintext
	CryptoPP::Poly1305<CryptoPP::AES> poly1305(keys[0], LOCALKEYLEN, xsalsa20nonce, NONCELEN);
	poly1305.Update(plaintext, PLAINTEXTLEN);
	poly1305.Final(xsalsa20hmac);
	// Encrypt with XSalsa20
	CryptoPP::XSalsa20::Encryption xsalsa20;
	xsalsa20.SetKeyWithIV(keys[0], LOCALKEYLEN, xsalsa20nonce, NONCELEN);
	xsalsa20.ProcessData(xsalsa20ciphertext, plaintext, PLAINTEXTLEN);

	/* Encrypt using AES-CBC */
	// Set up input. Our plaintext is the last 8 bytes of the xsalsa20 nonce, the xsalsa20 ciphertext, and the poly1305 hmac.
	byte* aesplaintext = xsalsa20ciphertextfull+16;
	// Set up output. Our IV is the first 16 bytes of the xsalsa20 nonce.
	byte* aesciphertextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	byte* aesiv = aesciphertextfull;
	byte* aesciphertext = aesiv+IVLEN;
	memcpy(aesiv, xsalsa20nonce, IVLEN);
	// Run AES-CBC encryption without padding
	CryptoPP::CBC_Mode<CryptoPP::AES>::Encryption aescbc;
	aescbc.SetKeyWithIV(keys[1], LOCALKEYLEN, aesiv, IVLEN);
	CryptoPP::ArraySource(aesplaintext, BLOCKTEXTLEN, true,
		new CryptoPP::StreamTransformationFilter( aescbc,
			new CryptoPP::ArraySink(aesciphertext, BLOCKTEXTLEN),
			CryptoPP::StreamTransformationFilter::NO_PADDING
		)
	);

	/* Encrypt using Twofish-CFB */
	// Set up input. Our plaintext is the last 8 bytes of the xsalsa20 nonce, the xsalsa20 ciphertext, and the poly1305 hmac.
	byte* twofishplaintext = aesciphertext;
	// Set up output. Our IV is the first 16 bytes of the xsalsa20 nonce.
	byte* twofishciphertextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	byte* twofishiv = twofishciphertextfull;
	byte* twofishciphertext = twofishiv+IVLEN;
	memcpy(twofishiv, aesiv, IVLEN);
	// Run Twofish-CFB encryption without padding
	CryptoPP::CFB_Mode<CryptoPP::Twofish>::Encryption twofishcfb;
	twofishcfb.SetKeyWithIV(keys[2], LOCALKEYLEN, twofishiv, IVLEN);
	CryptoPP::ArraySource(twofishplaintext, BLOCKTEXTLEN, true,
		new CryptoPP::StreamTransformationFilter( twofishcfb,
			new CryptoPP::ArraySink(twofishciphertext, BLOCKTEXTLEN),
			CryptoPP::StreamTransformationFilter::NO_PADDING
		)
	);

	/* Encrypt using Serpent-CTR */
	// Set up input. Our plaintext is the last 8 bytes of the xsalsa20 nonce, the xsalsa20 ciphertext, and the poly1305 hmac.
	byte* serpentplaintext = twofishciphertext;
	// Set up output. Our IV is the first 16 bytes of the xsalsa20 nonce.
	byte* serpentciphertextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	byte* serpentiv = serpentciphertextfull;
	byte* serpentciphertext = serpentiv+IVLEN;
	memcpy(serpentiv, twofishiv, IVLEN);
	// Run Serpent-CTR encryption without padding
	CryptoPP::CTR_Mode<CryptoPP::Serpent>::Encryption serpentctr;
	serpentctr.SetKeyWithIV(keys[3], LOCALKEYLEN, serpentiv, IVLEN);
	CryptoPP::ArraySource(serpentplaintext, BLOCKTEXTLEN, true,
		new CryptoPP::StreamTransformationFilter( serpentctr,
			new CryptoPP::ArraySink(serpentciphertext, BLOCKTEXTLEN),
			CryptoPP::StreamTransformationFilter::NO_PADDING
		)
	);

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

// Decrypts a plaintext using a master key. Input must CIPHERTEXTLEN, output must be PLAINTEXTLEN. Returns false on failure, true on success.
bool decrypt(const byte* ciphertext, const byte* masterkey, byte* output) {
	/* Set up keys, seed, and nonce */
	// Get seed and copy over to tohash 
	const byte* seed = ciphertext;
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

	/* Decrypt using Serpent-CTR */
	// Set up output
	byte* serpentplaintextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	byte* serpentiv = serpentplaintextfull;
	byte* serpentplaintext = serpentiv+IVLEN;
	memcpy(serpentiv, seed+SEEDLEN, IVLEN);
	// Set up input
	const byte* serpentciphertext = seed+SEEDLEN+IVLEN;
	// Decrypt with Serpent
	CryptoPP::CTR_Mode<CryptoPP::Serpent>::Decryption serpentctr;
	serpentctr.SetKeyWithIV(keys[3], LOCALKEYLEN, serpentiv, IVLEN);
	CryptoPP::ArraySource(serpentciphertext, BLOCKTEXTLEN, true,
		new CryptoPP::StreamTransformationFilter( serpentctr,
			new CryptoPP::ArraySink(serpentplaintext, BLOCKTEXTLEN),
			CryptoPP::StreamTransformationFilter::NO_PADDING
		)
	);

	/* Decrypt using Twofish-CFB */
	// Set up output
	byte* twofishplaintextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	byte* twofishiv = twofishplaintextfull;
	byte* twofishplaintext = twofishiv+IVLEN;
	memcpy(twofishiv, serpentiv, IVLEN);
	// Set up input
	byte* twofishciphertext = serpentplaintext;
	// Decrypt with Serpent
	CryptoPP::CFB_Mode<CryptoPP::Twofish>::Decryption twofishcfb;
	twofishcfb.SetKeyWithIV(keys[2], LOCALKEYLEN, twofishiv, IVLEN);
	CryptoPP::ArraySource(twofishciphertext, BLOCKTEXTLEN, true,
		new CryptoPP::StreamTransformationFilter( twofishcfb,
			new CryptoPP::ArraySink(twofishplaintext, BLOCKTEXTLEN),
			CryptoPP::StreamTransformationFilter::NO_PADDING
		)
	);

	/* Decrypt using AES-CBC */
	// Set up output
	byte* aesplaintextfull = (byte*)malloc(CIPHERTEXTWITHOUTSEEDLEN);
	byte* aesiv = aesplaintextfull;
	byte* aesplaintext = aesiv+IVLEN;
	memcpy(aesiv, twofishiv, IVLEN);
	// Set up input
	byte* aesciphertext = twofishplaintext;
	// Decrypt with AES
	CryptoPP::CBC_Mode<CryptoPP::AES>::Decryption aescbc;
	aescbc.SetKeyWithIV(keys[1], LOCALKEYLEN, aesiv, IVLEN);
	CryptoPP::ArraySource(aesciphertext, BLOCKTEXTLEN, true,
		new CryptoPP::StreamTransformationFilter( aescbc,
			new CryptoPP::ArraySink(aesplaintext, BLOCKTEXTLEN),
			CryptoPP::StreamTransformationFilter::NO_PADDING
		)
	);

	/* Decrypt using XSalsa20-Poly1305 */
	// Set up output
	byte* plaintext = (byte*)malloc(PLAINTEXTLEN);
	byte hmac[HMACLEN];
	// Set up input
	byte* xsalsa20nonce = aesplaintextfull;
	byte* xsalsa20hmac = xsalsa20nonce+NONCELEN;
	byte* xsalsa20ciphertext = xsalsa20hmac+HMACLEN;
	// Decrypt with XSalsa20
	CryptoPP::XSalsa20::Decryption xsalsa20;
	xsalsa20.SetKeyWithIV(keys[0], LOCALKEYLEN, xsalsa20nonce, NONCELEN);
	xsalsa20.ProcessData(plaintext, xsalsa20ciphertext, PLAINTEXTLEN);
	// Generate HMAC for plaintext
	CryptoPP::Poly1305<CryptoPP::AES> poly1305(keys[0], LOCALKEYLEN, xsalsa20nonce, NONCELEN);
	poly1305.Update(plaintext, PLAINTEXTLEN);
	poly1305.Final(hmac);

	/* Output, Cleanup, and Return */
	// Copy output
	memcpy(output, plaintext, PLAINTEXTLEN);
	// Compare HMACs and return if they are equal. Store result in RAM.
	bool hmacs_equal = (memcmp(xsalsa20hmac, hmac, HMACLEN) == 0);
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

/**** HASHING FUNCTIONS ****/

#define HASH_PART_LEN 64
#define NUM_HASHES 2
#define HASH_FULL_LEN (HASH_PART_LEN*NUM_HASHES)
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

	// Hash wtih BLAKE2 (of length MASTERKEYLEN) 16384 times
	byte* blake2_input = (byte*)malloc(HASH_FULL_LEN);
	byte* blake2_output = (byte*)malloc(HASH_FULL_LEN);
	memcpy(blake2_input, hashes, HASH_FULL_LEN);
	for (int i=0; i<16384; i++) {
		CryptoPP::BLAKE2b(false, HASH_FULL_LEN).CalculateDigest(blake2_output, blake2_input, HASH_FULL_LEN);
		memcpy(blake2_input, blake2_output, HASH_FULL_LEN);
	}

	// Hash one final time to decrease length
	CryptoPP::BLAKE2b(false, MASTERKEYLEN).CalculateDigest(out, blake2_output, HASH_FULL_LEN);
	// Free everything and return
	secureFree(&hashes, HASH_FULL_LEN);
	secureFree(&blake2_input, HASH_FULL_LEN);
	secureFree(&blake2_output, HASH_FULL_LEN);
	return true;
}

#ifdef TEST_CPP
// Test function for C++
int main() {
	// Test params
	const byte accname[] = "asdfasdg";
	const char password[] = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
	const char masterpass[] = "somepassword";
	const byte username[] = "someuser1123";
	// KDF
	puts("Running KDF...");
	byte* key = (byte*)malloc(MASTERKEYLEN);
	if (!double_hash(password, strlen(password), username, strlen((const char*)username), key)) {
		puts("Error spawning pthread");
	}
	for (int i=0; i<MASTERKEYLEN; i++) {
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
		printf("%.*s:%.*s\n", stuff.accname_len, stuff.accname, stuff.password_len, stuff.password);
	} else {
		puts("Invalid HMAC");
	}
}
#else
// Python module
// Python headers
#include <python3.6m/Python.h>

// Exceptions
static PyObject *HMACError;

// Generates blake2b hash (username hash is 32 byte blake2, account hash is 16 byte blake2)
static PyObject* crypto_blake2b(PyObject *self, PyObject *args) {
	// Get args
	const byte* tohash; uint32_t tohash_len, digest_len;
	if (!PyArg_ParseTuple(args, "s#i", &tohash, &tohash_len, &digest_len)) return NULL;

	// Hash using blake2
	byte* output = (byte*)PyMem_Malloc(digest_len);
	CryptoPP::BLAKE2b(false, digest_len).CalculateDigest(output, tohash, tohash_len);

	// Return
	return PyBytes_FromStringAndSize((const char*)output, digest_len);
}

// Wipes a master key capsule
void wipe_master_key(PyObject* capsule) {
	wipe((byte*)PyCapsule_GetPointer(capsule, NULL), MASTERKEYLEN);
}

// Generates key from password and username hash
static PyObject* crypto_generate_key(PyObject *self, PyObject *args) {
	// Get args
	const char *password, *userhash;
	uint32_t password_len, userhash_len;
	if (!PyArg_ParseTuple(args, "s#s#", &password, &password_len, &userhash, &userhash_len)) return NULL;

	// Run double hash function
	byte* generated_key = (byte*)PyMem_Malloc(MASTERKEYLEN);
	if (!double_hash(password, password_len, (const byte*)userhash, userhash_len, generated_key)) {
		PyErr_SetString(PyExc_ValueError, "Failed to open pthread");
		return NULL;
	}

	// Return capsule containing generated master key
	return PyCapsule_New(generated_key, NULL, wipe_master_key);
}

// Generates master key hash from master key
#define PWHASHLEN 8
static PyObject* crypto_hash_master_key(PyObject *self, PyObject *args) {
	// Get arguments
	PyObject* capsule;
	if (!PyArg_ParseTuple(args, "O", &capsule)) return NULL;
	const byte* masterkey = (byte*)PyCapsule_GetPointer(capsule, NULL);

	// Check if masterkey exists
	if (masterkey == NULL) return NULL;

	// Hash master key
	// Hash master key with BLAKE2 (of length MASTERKEYLEN) 4096 times (this is to make complexity for the attacker to verify a password greater)
	byte* blake2_input = (byte*)malloc(MASTERKEYLEN);
	byte* blake2_output = (byte*)malloc(MASTERKEYLEN);
	memcpy(blake2_input, masterkey, MASTERKEYLEN);
	for (int i=0; i<4096; i++) {
		CryptoPP::BLAKE2b(false, MASTERKEYLEN).CalculateDigest(blake2_output, blake2_input, MASTERKEYLEN);
		memcpy(blake2_input, blake2_output, MASTERKEYLEN);
	}
	// Hash one last time to decrease length
	byte* output = (byte*)PyMem_Malloc(PWHASHLEN);
	CryptoPP::BLAKE2b(false, PWHASHLEN).CalculateDigest(output, blake2_output, MASTERKEYLEN);

	// Free everything and return
	secureFree(&blake2_input, MASTERKEYLEN);
	secureFree(&blake2_output, MASTERKEYLEN);
	return PyBytes_FromStringAndSize((const char*)output, PWHASHLEN);
}

// Encrypts account name and password using master key
static PyObject* crypto_encrypt(PyObject *self, PyObject *args) {
	// Get arguments
	PyObject* capsule;
	const char *accountname, *password;
	uint32_t accountname_len, password_len;
	if (!PyArg_ParseTuple(args, "Os#s#", &capsule, &accountname, &accountname_len, &password, &password_len)) return NULL;
	const byte* masterkey = (byte*)PyCapsule_GetPointer(capsule, NULL);

	// Check if masterkey exists
	if (masterkey == NULL) return NULL;
	// Check if accountname_len and password_len are valid. Note that because these are unsigned, negative numbers will wrap to huge positives.
	if (accountname_len > ACCOUNTNAMEMAXLEN || password_len > PASSWORDMAXLEN || accountname_len == 0 || password_len == 0) {
		PyErr_SetString(PyExc_ValueError, "Account name or password out of size range");
		return NULL;
	}

	// Run padding and encryption
	byte* plaintext = (byte*)malloc(PLAINTEXTLEN);
	generatePlaintext((const byte*)accountname, accountname_len, password, password_len, plaintext);
	byte* ct = (byte*)PyMem_Malloc(CIPHERTEXTLEN);
	encrypt(plaintext, masterkey, ct);

	// Free plaintext and return ciphertext
	secureFree(&plaintext, PLAINTEXTLEN);
	return PyBytes_FromStringAndSize((const char*)ct, CIPHERTEXTLEN);
}

// Decrypts account name and password using master key
static PyObject* crypto_decrypt(PyObject *self, PyObject *args) {
	// Get arguments
	PyObject* capsule;
	const byte *ciphertext;
	uint32_t ciphertext_len;
	if (!PyArg_ParseTuple(args, "Oy#", &capsule, &ciphertext, &ciphertext_len)) return NULL;
	const byte* masterkey = (byte*)PyCapsule_GetPointer(capsule, NULL);

	// Check if masterkey exists
	if (masterkey == NULL) return NULL;
	// Check if ciphertext length is valid
	if (ciphertext_len != CIPHERTEXTLEN) {
		PyErr_SetString(PyExc_ValueError, "Ciphertext length invalid");
	}

	// Run decryption
	byte* plaintext = (byte*)malloc(PLAINTEXTLEN);
	if (!decrypt(ciphertext, masterkey, plaintext)) {
		PyErr_SetString(HMACError, "HMAC Invalid");
		return NULL; // HMAC invalid
	}

	// Run deserialization and copy over to PyMem buffers
	struct accountname_and_password accnpass = deserializePlaintext(plaintext);
	byte* accname = (byte*)PyMem_Malloc(accnpass.accname_len);
	byte* password = (byte*)PyMem_Malloc(accnpass.password_len);
	memcpy(accname, accnpass.accname, accnpass.accname_len);
	memcpy(password, accnpass.password, accnpass.password_len);

	// Free plaintext and accnpass pointers and return PyMem account name and password
	secureFree(&plaintext, PLAINTEXTLEN);
	secureFree(&(accnpass.accname), accnpass.accname_len);
	secureFree((byte**)&(accnpass.password), accnpass.password_len);
	return Py_BuildValue("s#s#", accname, accnpass.accname_len, password, accnpass.password_len);
}

// Python module and method metadata
static PyMethodDef crypto_methods[] = {
	{"blake2b", crypto_blake2b, METH_VARARGS, "Run Blake2b hash function. Usage: blake2b(something, digestlength), returns digest bytes"},
	{"generateKey", crypto_generate_key, METH_VARARGS, "Generate a master key. Usage: generateKey(password, userhash), returns master key object"},
	{"encrypt", crypto_encrypt, METH_VARARGS, "Encrypt an account name and password. Usage: encrypt(masterkey, accname, pass), returns ciphertext bytes"},
	{"decrypt", crypto_decrypt, METH_VARARGS, "Decrypt a ciphertext. Usage: decrypt(masterkey, ciphertext), returns (accname, pass)"},
	{"hashMasterKey", crypto_hash_master_key, METH_VARARGS, "Hash a master key. Usage: hashMasterKey(masterkey), returns masterkeyhash bytes"},
	{NULL, NULL, 0, NULL}
};
static struct PyModuleDef crypto_module = {
	PyModuleDef_HEAD_INIT,
	"crypto",
	NULL,
	-1,
	crypto_methods
};

// Init functions
PyMODINIT_FUNC PyInit_crypto() {
	// Create module
	PyObject *m;
	m = PyModule_Create(&crypto_module);
	if (m == NULL) return NULL;

	// Create HMAC exception
	HMACError = PyErr_NewException("crypto.HMACError", NULL, NULL);
	Py_INCREF(HMACError);
	PyModule_AddObject(m, "error", HMACError);

	// Return
	return m;
}
int main(int argc, char* argv[]) {
	wchar_t *program = Py_DecodeLocale(argv[0], NULL);
	if (program == NULL) {
		fprintf(stderr, "Cannot decode argv[0]\n");
		exit(1);
	}
	PyImport_AppendInittab("crypto", PyInit_crypto);
	Py_SetProgramName(program);
	Py_Initialize();
	PyImport_ImportModule("crypto");
	PyMem_RawFree(program);
	return 0;
}
#endif