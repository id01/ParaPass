# Python libs
import secrets;
from getpass import getpass;

# C++ crypto module
from cpp import crypto;

# Web3 imports
import json, web3;
from web3 import Web3;
from web3.auto import w3;
from web3.contract import Contract;
from solc import compile_source;

# Functions to generate a username and account hash
def generate_username_hash(username):
	return crypto.blake2b(username, 32); # Username hash is 32 byte blake2b hash of username
def generate_account_hash(userhash, accountname, masterkey):
	accountnamehash = crypto.blake2b(accountname, 32); # 32 byte blake2b hash for account name only
	return crypto.blake2b(userhash + accountnamehash, 16);
#	masterkeyhash = crypto.blake2b(masterkey, 32); # 32 byte blake2b hash for master key only
#	return crypto.blake2b(userhash + accountnamehash + masterkeyhash, 16); # Hash all three together into 16-byte hash

# Main ParaPass class
class ParaPass:
	# Init function
	def __init__(self, address=None, genesis=False, ethaccount=None):
		# Initialize these variables to None for now
		self.userhash = None
		self.masterkey = None
		# Use the first account as the default unless otherwise specified
		if ethaccount:
			w3.eth.defaultAccount = ethaccount
		else:
			w3.eth.defaultAccount = w3.eth.accounts[0]

		# Get PPassNetwork source and compile
		print("Generating contract code...")
		with open('PPassNetwork.sol', 'r') as PPassSourceFile:
			PPassSource = PPassSourceFile.read();
		compiled_sol = compile_source(PPassSource);
		contract_interface = compiled_sol['<stdin>:PPassNetwork'];

		# Create a new PPassNetwork contract if genesis is true
		if genesis:
			# Create new contract and send for it to be mined
			print("Creating new ParaPass Network contract...")
			PPassCreator = w3.eth.contract(abi=contract_interface['abi'], bytecode=contract_interface['bin'])
			tx_hash = PPassCreator.constructor().transact()
			# Wait until it's mined, then display address. Copy over the new address to the "address" variable.
			print("Waiting for it to be mined...")
			tx_reciept = w3.eth.waitForTransactionReceipt(tx_hash)
			print("Contract mined! Address of contract is " + tx_reciept.contractAddress)
			address = tx_reciept.contractAddress

		# If address is specified, then get a PPassNetwork contract from an address.
		if address:
			# Connect to PPassNetwork
			print("Connecting to PPassNetwork at (%s)..." % (address))
			self.PPassNetwork = w3.eth.contract(address=address, abi=contract_interface['abi'])
			# Check whether we are connected
			if (self.PPassNetwork.functions.checkPPassNetworkState().call() == False):
				raise Exception("Couldn't connect to PPassNetwork!")
		else:
			raise ValueError("Either genesis must be true to create a PPassNetwork or the address of one to connect to must be specified")
		# Connected to ParaPass network
		print("Connected");

	# Function to login or create an account
	def userLogin(self, username, masterpass, createmode=False):
		# Generate username hash and master key
		print("Generating keys...")
		self.userhash = generate_username_hash(username);
		self.masterkey = crypto.generateKey(masterpass, self.userhash);
		# Either create a user account or check if we are the owner of one
		if createmode:
			# Try creating a user account
			try:
				print("Creating user account...")
				tx_hash = self.PPassNetwork.functions.addUser(self.userhash, crypto.hashMasterKey(self.masterkey)).transact()
				tx_reciept = w3.eth.waitForTransactionReceipt(tx_hash)
			except ValueError:
				print("Couldn't create user account; username may have been taken!")
				return
		else:
			# Check whether we are the owner of the username we want to log in as
			print("Attempting login...")
			if not self.PPassNetwork.functions.checkLogin(self.userhash, crypto.hashMasterKey(self.masterkey)).call():
				print("Failed to log in - incorrect username or password")
				return
		# We are now logged in
		print("Logged in!")

	# Function to generate a password
	def generatePassword(self, charlist, length):
		password = ""
		for i in range(length):
			password += secrets.choice(charlist)
		return password

	# Function to put a password
	def putPassword(self, accountname, password):
		# Encrypt account name and password using masterkey
		encryptedpass = crypto.encrypt(self.masterkey, accountname, password)
		# Generate account hash from account name
		accounthash = generate_account_hash(self.userhash, accountname, self.masterkey)
		# Put password
		print("Putting password...")
		tx_hash = self.PPassNetwork.functions.putPassword(self.userhash, accounthash, encryptedpass).transact()
		tx_reciept = w3.eth.waitForTransactionReceipt(tx_hash)
		# Done
		print("Done!")

	# Function to get a password
	def getPassword(self, accountname):
		# Generate account hash from account name
		accounthash = generate_account_hash(self.userhash, accountname, self.masterkey)
		# Get password and return
		print("Getting password...")
		passcrypt = self.PPassNetwork.functions.getPassword(self.userhash, accounthash).call()
		return crypto.decrypt(self.masterkey, passcrypt)

	# Function to logout user
	def userLogout(self):
		del self.masterkey
		del self.userhash

# ParaPass command line
if __name__ == "__main__":
	# Print headers and init vars
	print("Starting ParaPass command line...")
	print("Block Number:" + str(w3.eth.blockNumber))
	print("Balance: " + str(w3.eth.getBalance(w3.eth.accounts[0])))
	ethaccount = None
	ppass = None

	# Open command shell
	while True:
		command = input("ParaPass > ").split(' ')

		if len(command) == 0: # No command
			pass
		elif command[0] == "account": # Switches ethereum account. Usage: account [account number]
			if len(command) == 2:
				ethaccount = w3.eth.accounts(int(command[1]))
				print("Switched ethereum account to %d. Balance of account is %d." % (int(command[1]), w3.eth.getBalance(ethaccount)))
			else:
				print("Syntax error. Use help [command] for more information.")
		elif command[0] == "connect": # Uses an existing contract. Usage: connect [address of PPassNetwork instance]
			if len(command) == 2:
				ppass = ParaPass(address=command[1])
			else:
				print("Syntax error. Use help [command] for more information.")
		elif command[0] == "genesis": # Creates a contract. Usage: genesis
			ppass = ParaPass(genesis=True)
		elif command[0] == "loginuser": # Logs in to user account. Usage: loginuser [username]
			if len(command) == 2:
				ppass.userLogin(command[1], getpass());
			else:
				print("Syntax error. Use help [command] for more information.")
		elif command[0] == "setupuser": # Sets up a user account. Usage: setupuser [username]
			if len(command) == 2:
				masterpass = getpass()
				masterpass2 = getpass("Confirm Password: ")
				if masterpass == masterpass2:
					ppass.userLogin(command[1], masterpass, createmode=True)
				else:
					print("Passwords do not match")
			else:
				print("Syntax error. Use help [command] for more information.")
		elif command[0] == "logoutuser": # Logs out the user. Usage: logoutuser
			ppass.userLogout()
		elif command[0] == "putpassword": # Adds a password. Usage: putpassword [accname] [charlist] [length]
			if len(command) == 4:
				ppass.putPassword(command[1], ppass.generatePassword(command[2], int(command[3])))
			else:
				print("Syntax error. Use help [command] for more information.")
		elif command[0] == "getpassword": # Gets a password. Usage: getpassword [accname]
			if len(command) == 2:
				print("Password for %s: %s" % ppass.getPassword(command[1]))
			else:
				print("Syntax error. Use help [command] for more information.")
		elif command[0] == "help": # Display help
			if len(command) > 1:
				if command[1] == "account":
					print("Switches to a different ethereum account. Usage: account [number]")
				elif command[1] == "connect":
					print("Connects to an existing ParaPass network contract. Usage: connect [address]")
				elif command[1] == "genesis":
					print("Creates a ParaPass network contract. Usage: genesis [address]")
				elif command[1] == "loginuser":
					print("Logs in to a user account. Usage: loginuser [username]")
				elif command[1] == "setupuser":
					print("Sets up a user account. Usage: setupuser [username]")
				elif command[1] == "putpassword":
					print("Generates a password and puts it into the chain. Usage: putpassword [accname] [charlist] [length]")
				elif command[1] == "getpassword":
					print("Gets a password from the chain. Usage: getpassword [accname]")
				else:
					print("Command not found.")
			else:
				print("Commands: account, connect, genesis, loginuser, setupuser, help.")
				print("Use help [command] for information on a specific command.")
		elif command[0] == "exit" or command[0] == "quit": # Exit command
			exit(0)
