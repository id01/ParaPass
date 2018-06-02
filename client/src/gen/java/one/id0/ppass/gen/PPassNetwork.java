package one.id0.ppass.gen;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes16;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Bytes8;
import org.web3j.abi.datatypes.generated.Int32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import rx.Observable;
import rx.functions.Func1;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.4.0.
 */
public class PPassNetwork extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b506107fb806100206000396000f3006080604052600436106100825763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416631a2eb86781146100875780635aaca93e146100d157806365d310d7146100e9578063665fad291461018c578063797e15cf146101ff578063cea6ab981461022d578063e47620a81461028f575b600080fd5b34801561009357600080fd5b506100bd60043577ffffffffffffffffffffffffffffffffffffffffffffffff19602435166102c5565b604080519115158252519081900360200190f35b3480156100dd57600080fd5b506100bd600435610383565b3480156100f557600080fd5b506101176004356fffffffffffffffffffffffffffffffff19602435166103bf565b6040805160208082528351818301528351919283929083019185019080838360005b83811015610151578181015183820152602001610139565b50505050905090810190601f16801561017e5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561019857600080fd5b50604080516020600460443581810135601f81018490048402850184019095528484526100bd94823594602480356fffffffffffffffffffffffffffffffff1916953695946064949201919081908401838280828437509497506104839650505050505050565b34801561020b57600080fd5b5061021461063c565b60408051600392830b90920b8252519081900360200190f35b34801561023957600080fd5b50610245600435610642565b6040805173ffffffffffffffffffffffffffffffffffffffff909316835277ffffffffffffffffffffffffffffffffffffffffffffffff1990911660208301528051918290030190f35b34801561029b57600080fd5b506100bd60043577ffffffffffffffffffffffffffffffffffffffffffffffff196024351661069f565b60008281526020819052604081205473ffffffffffffffffffffffffffffffffffffffff16156102f457600080fd5b50600082815260208190526040902080547801000000000000000000000000000000000000000000000000830474010000000000000000000000000000000000000000027fffffffff0000000000000000ffffffffffffffffffffffffffffffffffffffff73ffffffffffffffffffffffffffffffffffffffff19909216331791909116179055600192915050565b60008181526020819052604081205473ffffffffffffffffffffffffffffffffffffffff1615156103b6575060016103ba565b5060005b919050565b6000828152602081815260408083206fffffffffffffffffffffffffffffffff1985168452600190810183529281902080548251600260001996831615610100029690960190911694909404601f8101849004840285018401909252818452606093929091908301828280156104765780601f1061044b57610100808354040283529160200191610476565b820191906000526020600020905b81548152906001019060200180831161045957829003601f168201915b5050505050905092915050565b60008381526020819052604081205473ffffffffffffffffffffffffffffffffffffffff1633146104b357600080fd5b6000848152602081815260408083206fffffffffffffffffffffffffffffffff198716845260019081019092529091205460026101009282161592909202600019011604151561054757604080516fffffffffffffffffffffffffffffffff1985168152905185917f8f06ca2343a6ee1a8556a4b6e9077b4417cecba5425c2bfa9604f1a0bbc0214d919081900360200190a25b6000848152602081815260408083206fffffffffffffffffffffffffffffffff19871684526001018252909120835161058292850190610737565b5060408051602080825284518183015284516fffffffffffffffffffffffffffffffff1987169388937fe7cbb1bd240adb34d95bb21ad81e0d966e2c22c86ad53f50166024cf19bf77f4938893919283929083019185019080838360005b838110156105f85781810151838201526020016105e0565b50505050905090810190601f1680156106255780820380516001836020036101000a031916815260200191505b509250505060405180910390a35060019392505050565b60005b90565b60006020819052908152604090205473ffffffffffffffffffffffffffffffffffffffff81169074010000000000000000000000000000000000000000900478010000000000000000000000000000000000000000000000000282565b60008281526020819052604081205473ffffffffffffffffffffffffffffffffffffffff1633148015610730575060008381526020819052604090205477ffffffffffffffffffffffffffffffffffffffffffffffff198381167401000000000000000000000000000000000000000090920478010000000000000000000000000000000000000000000000000216145b9392505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061077857805160ff19168380011785556107a5565b828001600101855582156107a5579182015b828111156107a557825182559160200191906001019061078a565b506107b19291506107b5565b5090565b61063f91905b808211156107b157600081556001016107bb5600a165627a7a72305820c67444b9377f8aff67c0bdafa9ec74b834c68842d23f576309827cc093a476cc0029";

    public static final String FUNC_ADDUSER = "addUser";

    public static final String FUNC_CHECKUSERFREE = "checkUserFree";

    public static final String FUNC_GETPASSWORD = "getPassword";

    public static final String FUNC_PUTPASSWORD = "putPassword";

    public static final String FUNC_GETPPASSNETWORKVERSION = "getPPassNetworkVersion";

    public static final String FUNC_USERS = "users";

    public static final String FUNC_CHECKLOGIN = "checkLogin";

    public static final Event CHANGEDPASSWORD_EVENT = new Event("changedPassword", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Bytes16>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
    ;

    public static final Event ADDEDACCOUNT_EVENT = new Event("addedAccount", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}),
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    protected PPassNetwork(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected PPassNetwork(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
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

    public RemoteCall<Boolean> checkLogin(byte[] _uid, byte[] _pwhash) {
        final Function function = new Function(FUNC_CHECKLOGIN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(_uid), 
                new org.web3j.abi.datatypes.generated.Bytes8(_pwhash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public static RemoteCall<PPassNetwork> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(PPassNetwork.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<PPassNetwork> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(PPassNetwork.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public List<ChangedPasswordEventResponse> getChangedPasswordEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(CHANGEDPASSWORD_EVENT, transactionReceipt);
        ArrayList<ChangedPasswordEventResponse> responses = new ArrayList<ChangedPasswordEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ChangedPasswordEventResponse typedResponse = new ChangedPasswordEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.uid = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.aid = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.newPass = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<ChangedPasswordEventResponse> changedPasswordEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, ChangedPasswordEventResponse>() {
            @Override
            public ChangedPasswordEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(CHANGEDPASSWORD_EVENT, log);
                ChangedPasswordEventResponse typedResponse = new ChangedPasswordEventResponse();
                typedResponse.log = log;
                typedResponse.uid = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.aid = (byte[]) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.newPass = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<ChangedPasswordEventResponse> changedPasswordEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CHANGEDPASSWORD_EVENT));
        return changedPasswordEventObservable(filter);
    }

    public List<AddedAccountEventResponse> getAddedAccountEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ADDEDACCOUNT_EVENT, transactionReceipt);
        ArrayList<AddedAccountEventResponse> responses = new ArrayList<AddedAccountEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AddedAccountEventResponse typedResponse = new AddedAccountEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.uid = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.aid = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<AddedAccountEventResponse> addedAccountEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, AddedAccountEventResponse>() {
            @Override
            public AddedAccountEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADDEDACCOUNT_EVENT, log);
                AddedAccountEventResponse typedResponse = new AddedAccountEventResponse();
                typedResponse.log = log;
                typedResponse.uid = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.aid = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<AddedAccountEventResponse> addedAccountEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDEDACCOUNT_EVENT));
        return addedAccountEventObservable(filter);
    }

    public static PPassNetwork load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new PPassNetwork(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static PPassNetwork load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new PPassNetwork(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static class ChangedPasswordEventResponse {
        public Log log;

        public byte[] uid;

        public byte[] aid;

        public byte[] newPass;
    }

    public static class AddedAccountEventResponse {
        public Log log;

        public byte[] uid;

        public byte[] aid;
    }
}