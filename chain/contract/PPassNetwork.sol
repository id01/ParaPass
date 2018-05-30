pragma solidity ^0.4.20;

contract PPassNetwork {
	/* Event for changing a password */
	event changedPassword(bytes32 indexed uid, bytes16 indexed aid, bytes newPass);
	/* Event for adding an account */
	event addedAccount(bytes32 indexed uid, bytes16 aid);

	/* This is a struct for a single user */
	struct User {
		address owner; // Owner of this user account
		mapping (bytes16 => bytes) passwords; // Mapping from account hash to password
	}

	/* This is where we keep our User IDs and passwords. */
	/* This mapping maps the user ID (uid) to a User struct. */
	mapping (bytes32 => User) public users;

	/* Constructor */
	constructor() public {
	}

	/* Add User. */
	function addUser(bytes32 _uid) public returns (bool success) {
		/* Make sure that UID is not taken */
		require(users[_uid].owner == 0);
		/* Claim UID in the user mapping and return success */
		users[_uid].owner = msg.sender;
		return true;
	}

	/* Put Password */
	function putPassword(bytes32 _uid, bytes16 _aid, bytes newPass) public returns (bool success) {
		/* Make sure that UID is owned by owner */
		require(users[_uid].owner == msg.sender);
		/* If account is new, trigger addedAccount event */
		if (users[_uid].passwords[_aid].length == 0) {
			emit addedAccount(_uid, _aid);
		}
		/* Put password */
		users[_uid].passwords[_aid] = newPass;
		/* Trigger change password event */
		emit changedPassword(_uid, _aid, newPass);
		return true;
	}

	/* Get Password */
	function getPassword(bytes32 _uid, bytes16 _aid) public view returns (bytes pass) {
		/* Get password */
		return users[_uid].passwords[_aid];
	}

	/* Check whether an address is the owner of a uid */
	function checkOwner(bytes32 _uid) public view returns (bool success) {
		return (users[_uid].owner == msg.sender);
	}

	/* Check whether you are connected to a PPassNetwork */
	function checkPPassNetworkState() public pure returns (bool success) {
		return true;
	}
}