package xyz.redtorch.desktop.rpc.service;

import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreRpc.RpcGetAccountListRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetContractListRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetMixContractListRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetOrderListRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetPositionListRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetTickListRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetTradeListRsp;

public interface RpcClientApiService {

	boolean asyncSubscribe(ContractField contract, String gatewayId, String reqId);

	boolean subscribe(ContractField contract, String gatewayId, String reqId, Integer timoutSeconds);

	boolean asyncUnsubscribe(ContractField contract, String gatewayId, String reqId);

	boolean unsubscribe(ContractField contract, String gatewayId, String reqId, Integer timoutSeconds);

	boolean asyncSubmitOrder(SubmitOrderReqField submitOrderReq, String reqId);

	String submitOrder(SubmitOrderReqField submitOrderReq, String reqId, Integer timoutSeconds);

	boolean asyncCancelOrder(CancelOrderReqField cancelOrderReq, String reqId);

	boolean cancelOrder(CancelOrderReqField cancelOrderReq, String reqId, Integer timoutSeconds);

	boolean asyncSearchContract(ContractField contract, String reqId);

	boolean searchContract(ContractField contract, String reqId, Integer timoutSeconds);

	boolean asyncGetContractList(String reqId);

	RpcGetContractListRsp getContractList(String reqId, Integer timoutSeconds);

	boolean asyncGetMixContractList(String reqId);

	RpcGetMixContractListRsp getMixContractList(String reqId, Integer timoutSeconds);

	boolean asyncGetTickList(String reqId);

	RpcGetTickListRsp getTickList(String reqId, Integer timoutSeconds);

	boolean asyncGetOrderList(String reqId);

	RpcGetOrderListRsp getOrderList(String reqId, Integer timoutSeconds);

	boolean asyncGetPositionList(String reqId);

	RpcGetPositionListRsp getPositionList(String reqId, Integer timoutSeconds);

	boolean asyncGetTradeList(String reqId);

	RpcGetTradeListRsp getTradeList(String reqId, Integer timoutSeconds);

	boolean asyncGetAccountList(String reqId);

	RpcGetAccountListRsp getAccountList(String reqId, Integer timoutSeconds);
}
