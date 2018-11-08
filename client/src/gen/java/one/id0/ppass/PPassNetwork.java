package one.id0.ppass;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes16;
import org.web3j.abi.datatypes.generated.Bytes8;
import org.web3j.abi.datatypes.generated.Int32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.6.0.
 */
public class PPassNetwork extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b5061098c806100206000396000f30060806040526004361061008d5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416631a2eb86781146100925780635aaca93e146100dc57806365d310d7146100f4578063665fad2914610197578063797e15cf1461020a578063cea6ab9814610238578063e0fbac231461029a578063e47620a814610302575b600080fd5b34801561009e57600080fd5b506100c860043577ffffffffffffffffffffffffffffffffffffffffffffffff1960243516610338565b604080519115158252519081900360200190f35b3480156100e857600080fd5b506100c860043561041a565b34801561010057600080fd5b506101226004356fffffffffffffffffffffffffffffffff1960243516610443565b6040805160208082528351818301528351919283929083019185019080838360005b8381101561015c578181015183820152602001610144565b50505050905090810190601f1680156101895780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156101a357600080fd5b50604080516020600460443581810135601f81018490048402850184019095528484526100c894823594602480356fffffffffffffffffffffffffffffffff1916953695946064949201919081908401838280828437509497506105039650505050505050565b34801561021657600080fd5b5061021f610626565b60408051600392830b90920b8252519081900360200190f35b34801561024457600080fd5b5061025060043561062c565b6040805173ffffffffffffffffffffffffffffffffffffffff909316835277ffffffffffffffffffffffffffffffffffffffffffffffff1990911660208301528051918290030190f35b3480156102a657600080fd5b506102b2600435610689565b60408051602080825283518183015283519192839290830191858101910280838360005b838110156102ee5781810151838201526020016102d6565b505050509050019250505060405180910390f35b34801561030e57600080fd5b506100c860043577ffffffffffffffffffffffffffffffffffffffffffffffff1960243516610732565b60008281526020819052604081205473ffffffffffffffffffffffffffffffffffffffff161561036757600080fd5b60008381526020818152604080832080543373ffffffffffffffffffffffffffffffffffffffff19909116177fffffffff0000000000000000ffffffffffffffffffffffffffffffffffffffff1674010000000000000000000000000000000000000000780100000000000000000000000000000000000000000000000088040217815581518481528084019283905287855293909252915161041092600190920191906107ca565b5060019392505050565b60009081526020819052604090205473ffffffffffffffffffffffffffffffffffffffff161590565b6000828152602081815260408083206fffffffffffffffffffffffffffffffff19851684526002908101835292819020805482516000196001831615610100020190911694909404601f8101849004840285018401909252818452606093929091908301828280156104f65780601f106104cb576101008083540402835291602001916104f6565b820191906000526020600020905b8154815290600101906020018083116104d957829003601f168201915b5050505050905092915050565b60008381526020819052604081205473ffffffffffffffffffffffffffffffffffffffff16331461053357600080fd5b6000848152602081815260408083206fffffffffffffffffffffffffffffffff198716845260029081019092529091205461010060018216150260001901160415156105e057600084815260208181526040822060019081018054808301825590845291909220600282040180546fffffffffffffffffffffffffffffffff929093166010026101000a918202199092167001000000000000000000000000000000008604919091021790555b6000848152602081815260408083206fffffffffffffffffffffffffffffffff19871684526002018252909120835161061b9285019061089f565b506001949350505050565b60005b90565b60006020819052908152604090205473ffffffffffffffffffffffffffffffffffffffff81169074010000000000000000000000000000000000000000900478010000000000000000000000000000000000000000000000000282565b6000818152602081815260409182902060010180548351818402810184019094528084526060939283018282801561072657602002820191906000526020600020906000905b82829054906101000a9004700100000000000000000000000000000000026fffffffffffffffffffffffffffffffff191681526020019060100190602082600f010492830192600103820291508084116106cf5790505b50505050509050919050565b60008281526020819052604081205473ffffffffffffffffffffffffffffffffffffffff16331480156107c3575060008381526020819052604090205477ffffffffffffffffffffffffffffffffffffffffffffffff198381167401000000000000000000000000000000000000000090920478010000000000000000000000000000000000000000000000000216145b9392505050565b8280548282559060005260206000209060010160029004810192821561088f5791602002820160005b8382111561085157835183826101000a8154816fffffffffffffffffffffffffffffffff0219169083700100000000000000000000000000000000900402179055509260200192601001602081600f010492830192600103026107f3565b801561088d5782816101000a8154906fffffffffffffffffffffffffffffffff0219169055601001602081600f01049283019260010302610851565b505b5061089b929150610919565b5090565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106108e057805160ff191683800117855561090d565b8280016001018555821561090d579182015b8281111561090d5782518255916020019190600101906108f2565b5061089b929150610946565b61062991905b8082111561089b5780546fffffffffffffffffffffffffffffffff1916815560010161091f565b61062991905b8082111561089b576000815560010161094c5600a165627a7a7230582044e67ea736bf6a9282dd1ff5c2b2d30554c28e66e2a8bf561e812b38ca5ad0be0029";

    public static final String FUNC_ADDUSER = "addUser";

    public static final String FUNC_CHECKUSERFREE = "checkUserFree";

    public static final String FUNC_GETPASSWORD = "getPassword";

    public static final String FUNC_PUTPASSWORD = "putPassword";

    public static final String FUNC_GETPPASSNETWORKVERSION = "getPPassNetworkVersion";

    public static final String FUNC_USERS = "users";

    public static final String FUNC_GETALLACCOUNTS = "getAllAccounts";

    public static final String FUNC_CHECKLOGIN = "checkLogin";

    @Deprecated
    protected PPassNetwork(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected PPassNetwork(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected PPassNetwork(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected PPassNetwork(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> addUser(byte[] _uid, byte[] _pwhash) {
        final Function function = new Function(
                FUNC_ADDUSER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(_uid), 
                new org.web3j.abi.datatypes.generated.Bytes8(_pwhash)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<Boolean> checkUserFree(byte[] _uid) {
        final Function function = new Function(FUNC_CHECKUSERFREE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(_uid)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<byte[]> getPassword(byte[] _uid, byte[] _aid) {
        final Function function = new Function(FUNC_GETPASSWORD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(_uid), 
                new org.web3j.abi.datatypes.generated.Bytes16(_aid)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteCall<TransactionReceipt> putPassword(byte[] _uid, byte[] _aid, byte[] newPass) {
        final Function function = new Function(
                FUNC_PUTPASSWORD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(_uid), 
                new org.web3j.abi.datatypes.generated.Bytes16(_aid), 
                new org.web3j.abi.datatypes.DynamicBytes(newPass)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<BigInteger> getPPassNetworkVersion() {
        final Function function = new Function(FUNC_GETPPASSNETWORKVERSION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int32>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<Tuple2<String, byte[]>> users(byte[] param0) {
        final Function function = new Function(FUNC_USERS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Bytes8>() {}));
        return new RemoteCall<Tuple2<String, byte[]>>(
                new Callable<Tuple2<String, byte[]>>() {
                    @Override
                    public Tuple2<String, byte[]> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<String, byte[]>(
                                (String) results.get(0).getValue(), 
                                (byte[]) results.get(1).getValue());
                    }
                });
    }

    public RemoteCall<List> getAllAccounts(byte[] _uid) {
        final Function function = new Function(FUNC_GETALLACCOUNTS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(_uid)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Bytes16>>() {}));
        return new RemoteCall<List>(
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteCall<Boolean> checkLogin(byte[] _uid, byte[] _pwhash) {
        final Function function = new Function(FUNC_CHECKLOGIN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(_uid), 
                new org.web3j.abi.datatypes.generated.Bytes8(_pwhash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public static RemoteCall<PPassNetwork> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(PPassNetwork.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<PPassNetwork> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(PPassNetwork.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<PPassNetwork> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(PPassNetwork.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<PPassNetwork> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(PPassNetwork.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static PPassNetwork load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new PPassNetwork(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static PPassNetwork load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new PPassNetwork(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static PPassNetwork load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new PPassNetwork(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static PPassNetwork load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new PPassNetwork(contractAddress, web3j, transactionManager, contractGasProvider);
    }
}
