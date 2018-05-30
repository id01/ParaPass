# Python libs
import sys;
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
	masterkeyhash = crypto.blake2b(masterkey, 32); # 32 byte blake2b hash for master key only
	return crypto.blake2b(userhash + accountnamehash + masterkeyhash, 16); # Hash all three together into 16-byte hash

# Main ParaPass class
class ParaPass:
	# Init function
	def __init__(self, username, masterpass, address=None, genesis=False, ethaccount=None, createuser=False):
		# Use the first account as the default unless otherwise specified
		if ethaccount:
			w3.eth.defaultAccount = ethaccount
		else:
			w3.eth.defaultAccount = w3.eth.accounts[0]

		# Generate username hash and master key
		print("Generating keys...")
		self.userhash = generate_username_hash(username);
		self.masterkey = crypto.generateKey(masterpass, username);

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

		# Either create a user account or check if we are the owner of one
		if createuser:
			# Try creating a user account
			try:
				print("Creating user account...")
				tx_hash = self.PPassNetwork.functions.addUser(self.userhash).transact()
				tx_reciept = w3.eth.waitForTransactionReceipt(tx_hash)
			except ValueError:
				print("Couldn't create user account; username may have been taken!")
				exit(1)
		else:
			# Check whether we are the owner of the username we want to log in as
			print("Checking whether this is our account...")
			if not self.PPassNetwork.functions.checkOwner(self.userhash).call():
				print("This address is not the owner of this user account!")
				exit(1)

		# Done!
		print("Done!")

# ParaPass command line
if __name__ == "__main__":
	# Command line options
	if "-h" in sys.argv:
		print("Usage: python3 backend.py [flags]")
		print("Flags: -c: creates an account instead of logging into one")
		print("       -g: creates a new PPassNetwork instead of connecting to one")
		print("       -a [address]: specifies the address of the PPassNetwork to connect to")

	# Check whether address is specified and put it into address
	try:
		address = sys.argv[sys.argv.index("-a")+1]
	except IndexError:
		print("Address must be specified after -a flag")
		exit(-1);
	except ValueError:
		# Check for genesis flag. If it's there, set the address to None
		if "-g" in sys.argv:
			address = None
		else:
			print("Either -g or -a must be specified")
			exit(-1)

	# Check whether we are creating a user
	if "-c" in sys.argv:
		createuser = True
	else:
		createuser = False

	# Ask for username and password
	print("Starting ParaPass command line...")
	print("Block Number:" + str(w3.eth.blockNumber))
	print("Balance: " + str(w3.eth.getBalance(w3.eth.accounts[0])))
	username = input("Username: ")
	masterpass = getpass()

	# Create ParaPass object
	if address:
		ppass = ParaPass(username, masterpass, address=address, createuser=createuser)
	else:
		ppass = ParaPass(username, masterpass, genesis=True, createuser=createuser)

	# Open command shell
	while True:
		command = input("ParaPass > ").split(' ')

		if len(command) == 0: # No command
			print("")
		elif command[0] == "exit" or command[0] == "quit": # Exit command
			exit(0)
