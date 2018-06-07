### ParaPass
Passwords for the paranoid

### Features
* Passwords are stored securly in an Ethereum Blockchain so that past passwords can always be recovered, even if a hacker obtains a private key.
* Password encryption is done end-to-end on the client side. Nobody has any knowledge of the contents of the passwords.
* Passwords and account names are padded to a constant number of bytes before encryption, so no information about password or account name length are leaked.
* Passwords are quadruple-encrypted with Serpent-CTR over Twofish-CFB over AES-CBC over XSalsa20-Poly1305, four strong modern ciphers, so that passwords stay secret even if one, two, or three are broken.
* Key Derivation is done with two modern KDFs in parallel with very heavy parameters (Scrypt-18-4-0 and Argon2id-8-18-1)
* The backend is written in C++ and Java for both lighter weight and eventually cross-platform portability.

### Build
* Dependencies
	* CryptoPP
	* Pthread
	* JNI
	* JavaFX
	* Maven (will install other dependencies)
* Build
	* Generate `PPassNetwork.java` by building `PPassNetwork.sol` with the `web3j` command. Put this in `client/src/gen/java/id0/ppass/gen/PPassNetwork.java`
	* Download [argon2](https://github.com/P-H-C/phc-winner-argon2) and [scrypt-jane](https://github.com/floodyberry/scrypt-jane) from their respective repos and
	put them in `client/src/main/cpp/libs`.
	* Run `make` in `client` to build crypto binaries
	* Build PPassBackend and run for CLI

### Todo
* Remove password-change event to reduce gas cost, "Past Passwords" feature - both backend and frontend - by caching encrypted passwords on the user-side with the SQLite database
* "Remember me" feature using system keyring (implementations may be different for different platforms)
* Minimize to system tray upon close, re-open upon clicking on system tray

### Changelog
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
