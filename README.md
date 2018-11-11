### ParaPass
Passwords for the Paranoid

### Features
* Passwords are stored securely in an Ethereum Blockchain so that no one entity has control over your passwords.
* Password encryption is done end-to-end on the client side. Nobody else has any knowledge of the contents of the passwords.
* Passwords and account names are padded to a constant number of bytes before encryption, so no information about password or account name length are leaked.
* Passwords are quadruple-encrypted with Serpent-CBC over Twofish-CTR over AES-CBC over XSalsa20-Poly1305, four strong modern ciphers, so that passwords stay secret even if one, two, or three are broken.
* Key Derivation is done with two modern KDFs in parallel with very heavy parameters (Scrypt-18-4-0 and Argon2id-8-18-1). Furthermore, the master key is hashed with a unique seed for each encryption, so keys aren't reused.
* The cryptography backend is written in C++ for lighter weight and the rest is written with Java for eventual cross-platform portability.

### Build
* Dependencies
	* C libs including tweetnacl, argon2, and scrypt-jane
	* CryptoPP
	* Pthread
	* JNI
	* JavaFX, AWT
	* Maven (will install other dependencies)
* Build
	* Generate `PPassNetwork.java` by building `PPassNetwork.sol` with the `web3j` command. Put this in `client/src/gen/java/id0/ppass/gen/PPassNetwork.java`
	* Download [argon2](https://github.com/P-H-C/phc-winner-argon2) and [scrypt-jane](https://github.com/floodyberry/scrypt-jane) from their respective repos and
	put them in `client/src/main/cpp/libs`.
	* Run `make` in `client` to build crypto binaries
	* Run PPassBackend for CLI, or PPassUI for GUI via Java
	* Note: WebExtension server can be disabled by changing JSONServer to NullServer

### Todo
* I still need a taskbar icon...

### Changelog
* Version 0.1.1 (11/10/2018)
	* Server selection
	* Cross-platform configuration file paths
	* Moved past passwords to prompt
	* Some minor build and configuration setting changes
* Version 0.1.0 (11/09/2018)
	* Major UI overhaul
	* Autologin "remember me" feature (not secure!)
	* Dual-Factor Authentication - ".ppass" file (with keys encrypted using one time pad) required for login, keystore file no longer necessary. Also increases entropy of key.
	* Removed webextension and server due to security concerns
* Version 0.0.6 (06/19/2018)
	* Created barebones of a webextension (not usable yet without leaking password and probably server key due to lack of masking)
	* Connected "create account" function to GUI
	* Switched cipher modes to CBC-CTR-CBC instead of CBC-CFB-CTR
* Version 0.0.5 (06/08/2018)
	* Moved around a lot of code
	* Minimize to system tray upon close, re-open upon clicking on system tray (using awt)
	* Weeded out a bug where adding a password would cause all past password decryptions to give an "invalid HMAC" error
	* Swapped out XSalsa20 + Poly1305 AES to a more standard XSalsa20-Poly1305
* Version 0.0.4 (06/07/2018)
	* Removed password-change event to reduce gas cost.
	* "Past Passwords" feature added by caching encrypted passwords on the user-side with the SQLite database
* Version 0.0.3 (06/06/2018)
	* SQLite integration
	* (Single-Encrypted) descriptions!!! Timestamps!!! Pins!!!
* Version 0.0.2 (06/05/2018)
	* Moved random password generation to CryptoPP::GenerateWord
	* Added GUI (using JavaFX)
	* Added "get all accounts" functionality to smart contract
	* Fixed some GUI quirks
* Version 0.0.1 (06/01/2018)
	* Rewrote everything a lot of times and weeded out a lot of bugs
