// Java header
#include "one_id0_ppass_backend_Crypto.h"

// Libs and crypto header file
#include <string>
#include "crypto.h"

// Throws an exception
jint throwException(JNIEnv *env, const char *message)
{
	jclass Exception = env->FindClass("java/lang/Exception");
	return env->ThrowNew(Exception, message);
}

// Generates blake2b hash (username hash is 32 byte blake2, account hash is 16 byte blake2). Only works from lengths 1 to 64
JNIEXPORT jbyteArray JNICALL Java_one_id0_ppass_backend_Crypto_blake2b(JNIEnv *env, jobject obj, jbyteArray javaTohash, jint digest_len) {
	// Get args
	size_t tohash_len = env->GetArrayLength(javaTohash);
	byte* tohash = (byte*)malloc(tohash_len);
	env->GetByteArrayRegion(javaTohash, 0, tohash_len, reinterpret_cast<jbyte*>(tohash));

	// Check length
	if (digest_len <= 0 || digest_len > 64) {
		throwException(env, "Blake2b output must be between 1 and 64 bytes");
		return NULL;
	}

	// Hash using blake2
	byte* output = (byte*)malloc((size_t)digest_len);
	CryptoPP::BLAKE2b(false, digest_len).CalculateDigest(output, tohash, tohash_len);

	// Copy over to Java and return
	jbyteArray javaOutput = env->NewByteArray(digest_len);
	env->SetByteArrayRegion(javaOutput, 0, digest_len, reinterpret_cast<jbyte*>(output));
	secureFree(&output, digest_len);
	return javaOutput;
}

// Generates random bytes using the native entropy source
JNIEXPORT void JNICALL Java_one_id0_ppass_backend_Crypto_generateRandomBytes(JNIEnv *env, jobject obj, jbyteArray javaBytes) {
	// Get length of byte array passed, and malloc a buffer of the same length
	size_t bytes_len = env->GetArrayLength(javaBytes);
	byte* bytes = (byte*)malloc(bytes_len);

	// Generate random numbers, copy over to Java buffer, then free
	CryptoPP::OS_GenerateRandomBlock(false, bytes, bytes_len);
	env->SetByteArrayRegion(javaBytes, 0, bytes_len, reinterpret_cast<jbyte*>(bytes));
	free(bytes);
}

// Generates PPass file encryption key from password and username hash
JNIEXPORT jbyteArray JNICALL Java_one_id0_ppass_backend_Crypto_generateKey(JNIEnv *env, jobject obj, jstring javaPassword, jbyteArray javaUserhash) {
	// Get args
	jboolean javaFalse = false;
	std::string password = env->GetStringUTFChars(javaPassword, &javaFalse);
	size_t userhash_len = env->GetArrayLength(javaUserhash);
	byte* userhash = (byte*)malloc(userhash_len);
	env->GetByteArrayRegion(javaUserhash, 0, userhash_len, reinterpret_cast<jbyte*>(userhash));

	// Run double hash function
	byte* generated_key = (byte*)malloc(HASH_DONE_LEN);
	if (!double_hash(password.c_str(), password.size(), userhash, userhash_len, generated_key)) {
		throwException(env, "Couldn't spawn threads");
		return NULL;
	}

	// Return byte array with master key
	jbyteArray output = env->NewByteArray(HASH_DONE_LEN);
	env->SetByteArrayRegion(output, 0, HASH_DONE_LEN, reinterpret_cast<jbyte*>(generated_key));
	secureFree(&generated_key, HASH_DONE_LEN);
	return output;
}

// Generates master key hash from master key
JNIEXPORT jint Java_one_id0_ppass_backend_Crypto_returnMasterKeyLength(JNIEnv *env, jobject obj) {
	return (jint)MASTERKEYLEN;
}

// Encrypts account name and password using master key
JNIEXPORT jbyteArray Java_one_id0_ppass_backend_Crypto_encryptAccountPassword(JNIEnv *env, jobject obj, jbyteArray javaMasterKey, jstring javaAccount, jstring javaPassword) {
	// Get args
	jboolean javaFalse = false;
	std::string accountname = env->GetStringUTFChars(javaAccount, &javaFalse);
	std::string password = env->GetStringUTFChars(javaPassword, &javaFalse);
	size_t masterkey_len = env->GetArrayLength(javaMasterKey);
	byte* masterkey = (byte*)malloc(masterkey_len);
	env->GetByteArrayRegion(javaMasterKey, 0, masterkey_len, reinterpret_cast<jbyte*>(masterkey));

	// Check if masterkey length is right
	if (masterkey_len != MASTERKEYLEN) return NULL;
	// Check if account name and password length are valid. Note that there cannot be a negative size.
	if (accountname.size() > ACCOUNTNAMEMAXLEN || password.size() > PASSWORDMAXLEN || accountname.size() == 0 || password.size() == 0) {
		puts("INVLEN");
		throwException(env, "Invalid account or password length");
		return NULL;
	}

	// Run padding and encryption
	byte* plaintext = (byte*)malloc(PLAINTEXTLEN);
	generatePlaintext((const byte*)accountname.c_str(), accountname.size(), password.c_str(), password.size(), plaintext);
	byte* ciphertext = (byte*)malloc(CIPHERTEXTLEN);
	encrypt(plaintext, masterkey, ciphertext);

	// Free everything and return ciphertext
	jbyteArray output = env->NewByteArray(CIPHERTEXTLEN);
	env->SetByteArrayRegion(output, 0, CIPHERTEXTLEN, reinterpret_cast<jbyte*>(ciphertext));
	secureFree(&plaintext, PLAINTEXTLEN);
	secureFree(&ciphertext, CIPHERTEXTLEN);
	return output;
}

// Decrypts account name and password using master key
JNIEXPORT jobjectArray Java_one_id0_ppass_backend_Crypto_decryptAccountPassword(JNIEnv *env, jobject obj, jbyteArray javaMasterKey, jbyteArray javaCiphertext) {
	// Get args
	size_t masterkey_len = env->GetArrayLength(javaMasterKey);
	byte* masterkey = (byte*)malloc(masterkey_len);
	env->GetByteArrayRegion(javaMasterKey, 0, masterkey_len, reinterpret_cast<jbyte*>(masterkey));
	size_t ciphertext_len = env->GetArrayLength(javaCiphertext);
	byte* ciphertext = (byte*)malloc(ciphertext_len);
	env->GetByteArrayRegion(javaCiphertext, 0, ciphertext_len, reinterpret_cast<jbyte*>(ciphertext));

	// Check if masterkey exists
	if (masterkey_len != MASTERKEYLEN) return NULL;
	// Check if ciphertext length is valid
	if (ciphertext_len != CIPHERTEXTLEN) {
		throwException(env, "Invalid ciphertext length"); // Invalid ciphertext length
		return NULL;
	}

	// Run decryption
	byte* plaintext = (byte*)malloc(PLAINTEXTLEN);
	if (!decrypt(ciphertext, masterkey, plaintext)) {
		throwException(env, "Invalid HMAC"); // HMAC invalid
		return NULL;
	}

	// Run deserialization and copy over to Java String array as output. Note that our strings are null-terminated.
	struct accountname_and_password accnpass = deserializePlaintext(plaintext);
	jobjectArray output = env->NewObjectArray(2, env->FindClass("java/lang/String"), env->NewStringUTF(""));
	env->SetObjectArrayElement(output, 0, env->NewStringUTF((char*)accnpass.accname));
	env->SetObjectArrayElement(output, 1, env->NewStringUTF(accnpass.password));

	// Free plaintext and accnpass pointers and return byte array
	secureFree(&plaintext, PLAINTEXTLEN);
	secureFree(&(accnpass.accname), accnpass.accname_len);
	secureFree((byte**)&(accnpass.password), accnpass.password_len);
	return output;
}

// Generates random password
JNIEXPORT jstring Java_one_id0_ppass_backend_Crypto_generateRandomPassword(JNIEnv *env, jobject obj, jbyteArray javaCharlist, jint javaLength) {
	// Get args
	size_t charlist_len = env->GetArrayLength(javaCharlist);
	char* charlist = (char*)malloc(charlist_len);
	env->GetByteArrayRegion(javaCharlist, 0, charlist_len, reinterpret_cast<jbyte*>(charlist));

	// Password buffer, PRNG and PRNG output buffer
	uint32_t length = javaLength;
	char password[length+1];
	CryptoPP::AutoSeededRandomPool rng(false, 48); // 384 bits of entropy in, maximum 384 bits of entropy out

	// Get randomly generated password bytes. Note that GenerateWord32 is inclusive.
	for (uint32_t i=0; i<length; i++) {
		password[i] = charlist[rng.GenerateWord32(0, charlist_len-1)];
	}

	// Free charlist, return password in a null terminated string
	free(charlist);
	password[length] = '\0';
	return env->NewStringUTF(password);
}

// Encrypts a misc byte array.
JNIEXPORT jbyteArray Java_one_id0_ppass_backend_Crypto_encryptMiscData(JNIEnv *env, jobject obj, jbyteArray javaMiscKey, jbyteArray javaPlaintext) {
	// Get args
	size_t plaintext_len = env->GetArrayLength(javaPlaintext);
	byte* plaintext = (byte*)malloc(plaintext_len);
	env->GetByteArrayRegion(javaPlaintext, 0, plaintext_len, reinterpret_cast<jbyte*>(plaintext));
	size_t misckey_len = env->GetArrayLength(javaMiscKey);
	byte* misckey = (byte*)malloc(misckey_len);
	env->GetByteArrayRegion(javaMiscKey, 0, misckey_len, reinterpret_cast<jbyte*>(misckey));

	// Check if misckey length is valid
	if (misckey_len != MISCKEYLEN) {
		throwException(env, "Invalid misckey length"); // Invalid ciphertext length
		return NULL;
	}

	/* Encrypt using XSalsa20-Poly1305 */
	// Set up output. Input is at plaintext. Our output (ciphertextfull) will be [nonce || hmac || ciphertext]
	size_t ciphertextfull_len = MISCNONCELEN+MISCHMACLEN+plaintext_len;
	byte* ciphertextfull = (byte*)malloc(ciphertextfull_len);
	byte* nonce = ciphertextfull;
	byte* hmacandct = nonce+MISCNONCELEN;
	// Generate nonce
	CryptoPP::OS_GenerateRandomBlock(false, nonce, MISCNONCELEN);
	// Generate HMAC and ct
	tweetnacl_wrapper_encrypt(plaintext,plaintext_len, nonce, misckey, hmacandct);

	/* Cleanup and return */
	// Copy over full ciphertext to buffer
	jbyteArray output = env->NewByteArray(ciphertextfull_len);
	env->SetByteArrayRegion(output, 0, ciphertextfull_len, reinterpret_cast<jbyte*>(ciphertextfull));
	// Clean up what we allocated
	secureFree(&plaintext, plaintext_len);
	secureFree(&misckey, MISCKEYLEN);
	free(ciphertextfull);
	// Return
	return output;
}

// Decrypts a misc byte array
JNIEXPORT jbyteArray Java_one_id0_ppass_backend_Crypto_decryptMiscData(JNIEnv *env, jobject obj, jbyteArray javaMiscKey, jbyteArray javaCiphertextFull) {
	// Get args
	size_t ciphertextfull_len = env->GetArrayLength(javaCiphertextFull);
	byte* ciphertextfull = (byte*)malloc(ciphertextfull_len);
	env->GetByteArrayRegion(javaCiphertextFull, 0, ciphertextfull_len, reinterpret_cast<jbyte*>(ciphertextfull));
	size_t misckey_len = env->GetArrayLength(javaMiscKey);
	byte* misckey = (byte*)malloc(misckey_len);
	env->GetByteArrayRegion(javaMiscKey, 0, misckey_len, reinterpret_cast<jbyte*>(misckey));

	// Check if misckey length is valid
	if (misckey_len != MISCKEYLEN) {
		throwException(env, "Invalid misckey length"); // Invalid ciphertext length
		return NULL;
	}

	/* Decrypt using XSalsa20-Poly1305 */
	// Set up output
	size_t plaintext_len = ciphertextfull_len-MISCNONCELEN-MISCHMACLEN;
	byte* plaintext = (byte*)malloc(plaintext_len);
	// Set up input
	byte* xsalsa20nonce = ciphertextfull;
	byte* xsalsa20hmacandct = xsalsa20nonce+MISCNONCELEN;
	// Decrypt with XSalsa20 and validate HMAC
	int result = tweetnacl_wrapper_decrypt(xsalsa20hmacandct, plaintext_len+MISCHMACLEN, xsalsa20nonce, misckey, plaintext);

	/* Output, Cleanup, and Return */
	// Copy output
	jbyteArray output = env->NewByteArray(plaintext_len);
	env->SetByteArrayRegion(output, 0, plaintext_len, reinterpret_cast<jbyte*>(plaintext));
	// Compare HMACs and return if they are equal. Store result in RAM.
	bool hmacs_equal = (result == 0);
	// Clean up what we allocated
	secureFree(&misckey, MISCKEYLEN);
	secureFree(&plaintext, plaintext_len);
	free(ciphertextfull);
	// Throw and exception if hmacs aren't equal
	if (!hmacs_equal) {
		throwException(env, "Invalid HMAC"); // HMAC invalid
		return NULL;
	}
	// Return output
	return output;
}

// Encrypts or decrypts a one time pad. Note that this is a symmetric operation and therefore to decrypt just replace the plaintext with the ciphertext
JNIEXPORT jbyteArray Java_one_id0_ppass_backend_Crypto_oneTimePad(JNIEnv *env, jobject obj, jbyteArray javaPlaintext, jbyteArray javaKey) {
	// Get args, make sure lengths of plaintext and key are equal. Ciphertext will be the same length as the plaintext.
	size_t key_len = env->GetArrayLength(javaKey);
	byte* key = (byte*)malloc(key_len);
	env->GetByteArrayRegion(javaKey, 0, key_len, reinterpret_cast<jbyte*>(key));
	size_t plaintext_len = env->GetArrayLength(javaPlaintext);
	byte* plaintext = (byte*)malloc(plaintext_len);
	env->GetByteArrayRegion(javaPlaintext, 0, plaintext_len, reinterpret_cast<jbyte*>(plaintext));
	if (!(key_len == plaintext_len)) {
		throwException(env, "Lengths of key and plaintext in OTP must be equal!"); // Invalid lengths
		return NULL;
	}

	// Run OTP
	byte* ciphertext = (byte*)malloc(plaintext_len);
	one_time_pad(plaintext, key, ciphertext, plaintext_len);

	// Copy to output, wipe, and return
	jbyteArray output = env->NewByteArray(plaintext_len);
	env->SetByteArrayRegion(output, 0, plaintext_len, reinterpret_cast<jbyte*>(ciphertext));
	secureFree(&key, key_len);
	secureFree(&plaintext, plaintext_len);
	secureFree(&ciphertext, plaintext_len);
	return output;
}