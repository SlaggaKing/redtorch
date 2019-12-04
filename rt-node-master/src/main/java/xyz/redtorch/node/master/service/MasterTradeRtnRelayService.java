package xyz.redtorch.node.master.service;

import java.util.List;

import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CommonRtnField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public interface MasterTradeRtnRelayService {
	void onOrder(CommonRtnField commonRtn, OrderField order);

	void onTrade(CommonRtnField commonRtn, TradeField trade);

	void onTick(CommonRtnField commonRtn, TickField tick);

	void onPosition(CommonRtnField commonRtn, PositionField position);

	void onAccount(CommonRtnField commonRtn, AccountField account);

	void onNotice(CommonRtnField commonRtn, NoticeField notice);

	void onOrderList(CommonRtnField commonRtn, List<OrderField> orderList);

	void onTradeList(CommonRtnField commonRtn, List<TradeField> tradeList);

	void onTickList(CommonRtnField commonRtn, List<TickField> tickList);

	void onPositionList(CommonRtnField commonRtn, List<PositionField> positionList);

	void onAccountList(CommonRtnField commonRtn, List<AccountField> accountList);
}
