package xyz.redtorch.node.slave.rpc.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import xyz.redtorch.common.service.RpcClientProcessService;
import xyz.redtorch.node.slave.rpc.service.RpcClientReqHandlerService;
import xyz.redtorch.node.slave.service.SlaveTradeCachesService;
import xyz.redtorch.node.slave.service.SlaveTradeExecuteService;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.CommonReqField;
import xyz.redtorch.pb.CoreField.CommonRspField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;
import xyz.redtorch.pb.CoreRpc.RpcId;
import xyz.redtorch.pb.CoreRpc.RpcSearchContractRsp;
import xyz.redtorch.pb.CoreRpc.RpcSubmitOrderRsp;
import xyz.redtorch.pb.CoreRpc.RpcCancelOrderRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetAccountListRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetContractListRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetOrderListRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetPositionListRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetTickListRsp;
import xyz.redtorch.pb.CoreRpc.RpcGetTradeListRsp;

@Service
public class RpcClientReqHandlerServiceImpl implements RpcClientReqHandlerService {

	private Logger logger = LoggerFactory.getLogger(RpcClientReqHandlerServiceImpl.class);

	@Autowired
	private SlaveTradeExecuteService slaveTradeExecuteService;
	@Autowired
	private RpcClientProcessService rpcClientProcessService;
	@Autowired
	private SlaveTradeCachesService slaveTradeCachesService;
	@Value("${rt.rpc.client.node-id}")
	private Integer nodeId;

	@Override
	public void submitOrder(CommonReqField commonReq, SubmitOrderReqField submitOrderReq) {
		String reqId = commonReq.getReqId();

		int targetNodeId = 0;
		int sourceNodeId = nodeId;

		CommonRspField.Builder commonRspBuilder = CommonRspField.newBuilder() //
				.setSourceNodeId(sourceNodeId) //
				.setTargetNodeId(targetNodeId) //
				.setReqId(reqId) //
				.setRequestStatus(CommonStatusEnum.SUCCESS);

		String orderId = null;
		if (submitOrderReq == null) {
			logger.error("参数submitOrderReq缺失");
			commonRspBuilder.setRequestStatus(CommonStatusEnum.ERROR).setInfo("参数submitOrderReq缺失");
		} else {
			try {
				orderId = slaveTradeExecuteService.submitOrder(commonReq, submitOrderReq);
				commonRspBuilder.setInfo("提交定单完成,原始定单ID:" + submitOrderReq.getOriginOrderId() + "定单ID:" + orderId);
			} catch (Exception e) {
				logger.error("提交定单错误", e);
				commonRspBuilder.setRequestStatus(CommonStatusEnum.ERROR).setInfo(e.getMessage());
			}
		}
		RpcSubmitOrderRsp.Builder rpcSubmitOrderRspBuilder = RpcSubmitOrderRsp.newBuilder();
		if (orderId == null) {
			rpcSubmitOrderRspBuilder.setCommonRsp(commonRspBuilder);
		} else {
			rpcSubmitOrderRspBuilder.setCommonRsp(commonRspBuilder).setOrderId(orderId);
		}
		rpcClientProcessService.sendRoutineCoreRpc(targetNodeId, rpcSubmitOrderRspBuilder.build().toByteString(), reqId, RpcId.SUBMIT_ORDER_RSP);
	}

	@Override
	public void cancelOrder(CommonReqField commonReq, CancelOrderReqField cancelOrderReq) {
		String reqId = commonReq.getReqId();
		
		int targetNodeId = 0;
		int sourceNodeId = nodeId;

		CommonRspField.Builder commonRspBuilder = CommonRspField.newBuilder() //
				.setSourceNodeId(sourceNodeId) //
				.setTargetNodeId(targetNodeId) //
				.setReqId(reqId) //
				.setRequestStatus(CommonStatusEnum.SUCCESS);
		if (cancelOrderReq == null) {
			logger.error("参数cancelOrderReq缺失");
			commonRspBuilder.setRequestStatus(CommonStatusEnum.ERROR).setInfo("参数cancelOrderReq缺失");
		} else {
			try {
				slaveTradeExecuteService.cancelOrder(commonReq, cancelOrderReq);
			} catch (Exception e) {
				logger.error("撤单错误", e);
				commonRspBuilder.setRequestStatus(CommonStatusEnum.ERROR).setInfo(e.getMessage());
			}
		}

		RpcCancelOrderRsp.Builder rpcCancelOrderRspBuilder = RpcCancelOrderRsp.newBuilder().setCommonRsp(commonRspBuilder);
		rpcClientProcessService.sendRoutineCoreRpc(targetNodeId, rpcCancelOrderRspBuilder.build().toByteString(), reqId, RpcId.CANCEL_ORDER_RSP);

	}

	@Override
	public void searchContract(CommonReqField commonReq, ContractField contract) {
		String reqId = commonReq.getReqId();
		
		int targetNodeId = 0;
		int sourceNodeId = nodeId;

		CommonRspField.Builder commonRspBuilder = CommonRspField.newBuilder() //
				.setSourceNodeId(sourceNodeId) //
				.setTargetNodeId(targetNodeId) //
				.setReqId(reqId) //
				.setRequestStatus(CommonStatusEnum.SUCCESS);

		if (contract == null) {
			logger.error("参数contract缺失");
			commonRspBuilder.setRequestStatus(CommonStatusEnum.ERROR).setInfo("参数contract缺失");
		} else {
			try {
				slaveTradeExecuteService.searchContract(commonReq, contract);
			} catch (Exception e) {
				logger.error("搜寻合约错误", e);
				commonRspBuilder.setRequestStatus(CommonStatusEnum.ERROR).setInfo(e.getMessage());
			}
		}

		RpcSearchContractRsp.Builder rpcSearchContractRspBuilder = RpcSearchContractRsp.newBuilder().setCommonRsp(commonRspBuilder);
		rpcClientProcessService.sendRoutineCoreRpc(targetNodeId, rpcSearchContractRspBuilder.build().toByteString(), reqId, RpcId.SEARCH_CONTRACT_RSP);
	}
	
	// -------------------------------------------------------------------------------------------------------------
	
		@Override
		public void getOrderList(CommonReqField commonReq) {
			String reqId = commonReq.getReqId();

			int targetNodeId = 0;
			int sourceNodeId = nodeId;

			CommonRspField.Builder commonRspBuilder = CommonRspField.newBuilder() //
					.setSourceNodeId(sourceNodeId) //
					.setTargetNodeId(targetNodeId) //
					.setReqId(reqId) //
					.setRequestStatus(CommonStatusEnum.SUCCESS);

			List<OrderField> orderList = slaveTradeCachesService.getOrderList();

			if (orderList == null) {
				orderList = new ArrayList<>();
			}

			RpcGetOrderListRsp.Builder rpcGetOrderListRspBuilder = RpcGetOrderListRsp.newBuilder() //
					.setCommonRsp(commonRspBuilder) //
					.addAllOrder(orderList);
			rpcClientProcessService.sendLz4CoreRpc(targetNodeId, rpcGetOrderListRspBuilder.build().toByteString(), reqId, RpcId.GET_ORDER_LIST_RSP);
		}

		@Override
		public void getTradeList(CommonReqField commonReq) {
			String reqId = commonReq.getReqId();
			
			int targetNodeId = 0;
			int sourceNodeId = nodeId;

			CommonRspField.Builder commonRspBuilder = CommonRspField.newBuilder() //
					.setSourceNodeId(sourceNodeId) //
					.setTargetNodeId(targetNodeId) //
					.setReqId(reqId) //
					.setRequestStatus(CommonStatusEnum.SUCCESS);

			List<TradeField> tradeList = slaveTradeCachesService.getTradeList();
			if (tradeList == null) {
				tradeList = new ArrayList<>();
			}

			RpcGetTradeListRsp.Builder rpcGetTradeListRspBuilder = RpcGetTradeListRsp.newBuilder() //
					.setCommonRsp(commonRspBuilder) //
					.addAllTrade(tradeList);
			rpcClientProcessService.sendLz4CoreRpc(targetNodeId, rpcGetTradeListRspBuilder.build().toByteString(), reqId, RpcId.GET_TRADE_LIST_RSP);
		}


		@Override
		public void getPositionList(CommonReqField commonReq) {
			String reqId = commonReq.getReqId();

			int targetNodeId = 0;
			int sourceNodeId = nodeId;

			CommonRspField.Builder commonRspBuilder = CommonRspField.newBuilder() //
					.setSourceNodeId(sourceNodeId) //
					.setTargetNodeId(targetNodeId) //
					.setReqId(reqId) //
					.setRequestStatus(CommonStatusEnum.SUCCESS);

			List<PositionField> positionList = slaveTradeCachesService.getPositionList();

			if (positionList == null) {
				positionList = new ArrayList<>();
			}

			RpcGetPositionListRsp.Builder rpcGetPositionListRspBuilder = RpcGetPositionListRsp.newBuilder() //
					.setCommonRsp(commonRspBuilder) //
					.addAllPosition(positionList);
			rpcClientProcessService.sendLz4CoreRpc(targetNodeId, rpcGetPositionListRspBuilder.build().toByteString(), reqId, RpcId.GET_POSITION_LIST_RSP);
		}

		@Override
		public void getAccountList(CommonReqField commonReq) {
			String reqId = commonReq.getReqId();

			int targetNodeId = 0;
			int sourceNodeId = nodeId;

			CommonRspField.Builder commonRspBuilder = CommonRspField.newBuilder() //
					.setSourceNodeId(sourceNodeId) //
					.setTargetNodeId(targetNodeId) //
					.setReqId(reqId) //
					.setRequestStatus(CommonStatusEnum.SUCCESS);

			List<AccountField> accountList = slaveTradeCachesService.getAccountList();

			if (accountList == null) {
				accountList = new ArrayList<>();
			}

			RpcGetAccountListRsp.Builder rpcGetAccountListRspBuilder = RpcGetAccountListRsp.newBuilder().setCommonRsp(commonRspBuilder) //
					.addAllAccount(accountList); //
			rpcClientProcessService.sendLz4CoreRpc(targetNodeId, rpcGetAccountListRspBuilder.build().toByteString(), reqId, RpcId.GET_ACCOUNT_LIST_RSP);
		}

		@Override
		public void getContractList(CommonReqField commonReq) {
			String reqId = commonReq.getReqId();

			int targetNodeId = 0;
			int sourceNodeId = nodeId;

			CommonRspField.Builder commonRspBuilder = CommonRspField.newBuilder() //
					.setSourceNodeId(sourceNodeId) //
					.setTargetNodeId(targetNodeId) //
					.setReqId(reqId) //
					.setRequestStatus(CommonStatusEnum.SUCCESS);

			List<ContractField> contractList = slaveTradeCachesService.getContractList();

			if (contractList == null) {
				contractList = new ArrayList<>();
			}

			RpcGetContractListRsp.Builder rpcGetContractListRspBuilder = RpcGetContractListRsp.newBuilder() //
					.setCommonRsp(commonRspBuilder) //
					.addAllContract(contractList);
			rpcClientProcessService.sendLz4CoreRpc(targetNodeId, rpcGetContractListRspBuilder.build().toByteString(), reqId, RpcId.GET_CONTRACT_LIST_RSP);
		}


		@Override
		public void getTickList(CommonReqField commonReq) {
			String reqId = commonReq.getReqId();

			int targetNodeId = 0;
			int sourceNodeId = nodeId;

			CommonRspField.Builder commonRspBuilder = CommonRspField.newBuilder() //
					.setSourceNodeId(sourceNodeId) //
					.setTargetNodeId(targetNodeId) //
					.setReqId(reqId) //
					.setRequestStatus(CommonStatusEnum.SUCCESS);

			List<TickField> tickList = slaveTradeCachesService.getTickList();
			if (tickList == null) {
				tickList = new ArrayList<>();
			}

			RpcGetTickListRsp.Builder rpcGetTickListRspBuilder = RpcGetTickListRsp.newBuilder() //
					.setCommonRsp(commonRspBuilder) //
					.addAllTick(tickList);
			rpcClientProcessService.sendLz4CoreRpc(targetNodeId, rpcGetTickListRspBuilder.build().toByteString(), reqId, RpcId.GET_TICK_LIST_RSP);
		}
}
