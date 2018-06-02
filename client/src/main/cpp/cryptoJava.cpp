// Java header
#include "one_id0_ppass_Crypto.h"

// Libs and crypto header file
#include <string>
#include "crypto.h"

// Throws an exception
jint throwException(JNIEnv *env, const char *message)
{
	jclass Exception = env->FindClass("java/lang/Exception");
	return env->ThrowNew(Exception, message);
}

// Generates blake2b hash (username hash is 32 byte blake2, account hash is 16 byte blake2)
JNIEXPORT jbyteArray JNICALL Java_one_id0_ppass_Crypto_blake2b(JNIEnv *env, jobject obj, jbyteArray javaTohash, jint digest_len) {
	// Get args
	size_t tohash_len = env->GetArrayLength(javaTohash);
	byte* tohash = (byte*)malloc(tohash_len);
	env->GetByteArrayRegion(javaTohash, 0, tohash_len, reinterpret_cast<jbyte*>(tohash));

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
JNIEXPORT void JNICALL Java_one_id0_ppass_Crypto_generateRandomBytes(JNIEnv *env, jobject obj, jbyteArray javaBytes) {
	// Get length of byte array passed, and malloc a buffer of the same length
	size_t bytes_len = env->GetArrayLength(javaBytes);
	byte* bytes = (byte*)malloc(bytes_len);

	// Generate random numbers, copy over to Java buffer, then free
	CryptoPP::OS_GenerateRandomBlock(false, bytes, bytes_len);
	env->SetByteArrayRegion(javaBytes, 0, bytes_len, reinterpret_cast<jbyte*>(bytes));
	free(bytes);
}

// Generates key from password and username hash
JNIEXPORT jbyteArray JNICALL Java_one_id0_ppass_Crypto_generateKey(JNIEnv *env, jobject obj, jstring javaPassword, jbyteArray javaUserhash) {
	// Get args
	jboolean javaFalse = false;
	std::string password = env->GetStringUTFChars(javaPassword, &javaFalse);
	size_t userhash_len = env->GetArrayLength(javaUserhash);
	byte* userhash = (byte*)malloc(userhash_len);
	env->GetByteArrayRegion(javaUserhash, 0, userhash_len, reinterpret_cast<jbyte*>(userhash));

	// Run double hash function
	byte* generated_key = (byte*)malloc(MASTERKEYLEN);
	if (!double_hash(password.c_str(), password.size(), userhash, userhash_len, generated_key)) {
		throwException(env, "Couldn't spawn threads");
		return NULL;
	}

	// Return byte array with master key
	jbyteArray output = env->NewByteArray(MASTERKEYLEN);
	env->SetByteArrayRegion(output, 0, MASTERKEYLEN, reinterpret_cast<jbyte*>(generated_key));
	secureFree(&generated_key, MASTERKEYLEN);
	return output;
}

// Generates master key hash from master key
JNIEXPORT jint Java_one_id0_ppass_Crypto_returnMasterKeyLength(JNIEnv *env, jobject obj) {
	return (jint)MASTERKEYLEN;
}

// Encrypts account name and password using master key
JNIEXPORT jbyteArray Java_one_id0_ppass_Crypto_encryptAccountPassword(JNIEnv *env, jobject obj, jbyteArray javaMasterKey, jstring javaAccount, jstring javaPassword) {
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
JNIEXPORT jbyteArray Java_one_id0_ppass_Crypto_decryptAccountPassword(JNIEnv *env, jobject obj, jbyteArray javaMasterKey, jbyteArray javaCiphertext) {
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

	// Run deserialization and copy over to Java byte array as output, separated by NULL
	struct accountname_and_password accnpass = deserializePlaintext(plaintext);
	jbyteArray output = env->NewByteArray(accnpass.accname_len+accnpass.password_len+1);
	env->SetByteArrayRegion(output, 0, accnpass.accname_len, reinterpret_cast<jbyte*>(accnpass.accname));
	env->SetByteArrayRegion(output, accnpass.accname_len+1, accnpass.password_len, reinterpret_cast<jbyte*>(accnpass.password));

	// Free plaintext and accnpass pointers and return byte array
	secureFree(&plaintext, PLAINTEXTLEN);
	secureFree(&(accnpass.accname), accnpass.accname_len);
	secureFree((byte**)&(accnpass.password), accnpass.password_len);
	return output;
}