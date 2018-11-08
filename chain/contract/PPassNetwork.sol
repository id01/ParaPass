pragma solidity ^0.4.20;

contract PPassNetwork {
	/* This is a struct for a single user */
	struct User {
		address owner; // Owner of this user account
		bytes8 passhash; // Collisions actually help us here. Increases complexity for the attacker with low chance of happening for the user.
		bytes16[] accounts; // List of all account hashes this user has
		mapping (bytes16 => bytes) passwords; // Mapping from account hash to password
	}

	/* This is where we keep our User IDs and passwords. */
	/* This mapping maps the user ID (uid) to a User struct. */
	mapping (bytes32 => User) public users;

	/* Constructor */
	constructor() public {
	}

	/* Add a user to the contract */
	function addUser(bytes32 _uid, bytes8 _pwhash) public returns (bool success) {
		/* Make sure that UID is not taken */
		require(users[_uid].owner == 0);
		/* Claim UID in the user mapping, copy over pwhash, and return success */
		users[_uid].owner = msg.sender;
		users[_uid].passhash = _pwhash;
		users[_uid].accounts = new bytes16[](0);
		return true;
	}

	/* Checks whether a uid is free (not taken) */
	function checkUserFree(bytes32 _uid) public view returns (bool free) {
		return (users[_uid].owner == 0);
	}

	/* Put Password */
	function putPassword(bytes32 _uid, bytes16 _aid, bytes newPass) public returns (bool success) {
		/* Make sure that UID is owned by owner */
		require(users[_uid].owner == msg.sender);
		/* If account is new, trigger addedAccount event */
		if (users[_uid].passwords[_aid].length == 0) {
			users[_uid].accounts.push(_aid);
		}
		/* Put password */
		users[_uid].passwords[_aid] = newPass;
		return true;
	}

	/* Get Password */
	function getPassword(bytes32 _uid, bytes16 _aid) public view returns (bytes pass) {
		return users[_uid].passwords[_aid];
	}

	/* Get All Accounts for a user */
	function getAllAccounts(bytes32 _uid) public view returns (bytes16[] accounts) {
		return users[_uid].accounts;
	}

	/* Check whether an address is the owner of a uid and has the right password hash */
	function checkLogin(bytes32 _uid, bytes8 _pwhash) public view returns (bool success) {
		return (users[_uid].owner == msg.sender && users[_uid].passhash == _pwhash);
	}

	/* Get PPassNetwork version */
	function getPPassNetworkVersion() public pure returns (int32 success) {
		return 0;
	}
}