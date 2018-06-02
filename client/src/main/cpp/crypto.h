// Header file with function prototypes, #defines, #includes, and structs in crypto.cpp
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

// Misc functions
void wipe(byte* ptr, size_t ptr_len);
void secureFree(byte** ptrptr, size_t ptr_len);

// Compression functions
// Get compressed/decompressed length with z827. Note that this is equivalent to Math.ceil(plain_len*7/8) and compressed_len*8/7
#define z827_compressed_len(plain_len) ((plain_len+1)*7/8)
#define z827_decompressed_len(compressed_len) (compressed_len*8/7)
void z827_compress(const char* plain, const size_t plain_len, byte* compressed);
void z827_decompress(const byte* compressed, const size_t compressed_len, char* plain);

// Serialization functions
struct accountname_and_password { // Struct containing account name and password
	byte* accname;
	size_t accname_len;
	char* password;
	size_t password_len;
};
#define ACCOUNTNAMEMAXLEN 46
#define PASSWORDCOMPRESSEDMAXLEN 56
#define PASSWORDMAXLEN (PASSWORDCOMPRESSEDMAXLEN*8/7)
void generatePlaintext(const byte* accname, const size_t accname_len, const char* password, const size_t password_len, byte* plaintext);
struct accountname_and_password deserializePlaintext(const byte* plaintext);

// Symmetric quadruple encryption functions
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
void encrypt(const byte* plaintext, const byte* masterkey, byte* output);
bool decrypt(const byte* ciphertext, const byte* masterkey, byte* output);

// Hashing functions
#define HASH_PART_LEN 64
#define NUM_HASHES 2
#define HASH_FULL_LEN (HASH_PART_LEN*NUM_HASHES)
bool double_hash(const char* pass, const size_t pass_len, const byte* salt, const size_t salt_len, byte* out);