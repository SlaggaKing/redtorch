package xyz.redtorch.desktop.layout.base;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import xyz.redtorch.common.constant.CommonConstant;
import xyz.redtorch.common.util.CommonUtils;
import xyz.redtorch.common.util.UUIDStringPoolUtils;
import xyz.redtorch.desktop.rpc.service.RpcClientApiService;
import xyz.redtorch.desktop.service.GuiMainService;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;

@Component
public class OrderLayout {

	private static final Logger logger = LoggerFactory.getLogger(OrderLayout.class);

	public static final int SHOW_ALL = 0;
	public static final int SHOW_CANCELABLE = 1;
	public static final int SHOW_CANCELLED = 2;

	private VBox vBox = new VBox();

	private boolean layoutCreated = false;

	private ObservableList<OrderField> orderObservableList = FXCollections.observableArrayList();

	private List<OrderField> orderList = new ArrayList<>();

	private TableView<OrderField> orderTableView = new TableView<>();

	private int showRadioValue = 0;

	private boolean showRejectedChecked = false;

	private Set<String> selectedOrderIdSet = new HashSet<>();

	@Autowired
	private GuiMainService guiMainService;
	@Autowired
	private RpcClientApiService rpcClientApiService;

	public Node getNode() {
		if (!layoutCreated) {
			createLayout();
			layoutCreated = true;
		}
		return this.vBox;
	}

	public void updateData(List<OrderField> orderList) {
		if (!new HashSet<>(this.orderList).equals(new HashSet<>(orderList))) {
			this.orderList = orderList;
			fillingData();
		}
	}

	public void fillingData() {
		orderTableView.getSelectionModel().clearSelection();

		List<OrderField> newOrderList = new ArrayList<>();
		for (OrderField order : this.orderList) {
			if (guiMainService.getSelectedAccountIdSet().isEmpty() || guiMainService.getSelectedAccountIdSet().contains(order.getAccountId())) {
				if (showRadioValue == SHOW_ALL) {
					if (showRejectedChecked) {
						newOrderList.add(order);
					} else if (OrderStatusEnum.REJECTED_VALUE != order.getOrderStatusValue()) {
						newOrderList.add(order);
					}
				} else if (showRadioValue == SHOW_CANCELABLE) {
					if (CommonConstant.ORDER_STATUS_WORKING_SET.contains(order.getOrderStatus())) {
						newOrderList.add(order);
					}
				} else {
					if (OrderStatusEnum.CANCELLED_VALUE == order.getOrderStatusValue()) {
						newOrderList.add(order);
					}
				}
			}
		}

		orderObservableList.clear();
		orderObservableList.addAll(newOrderList);
		Set<String> newSelectedOrderIdSet = new HashSet<>();
		for (OrderField order : orderObservableList) {
			if (selectedOrderIdSet.contains(order.getOrderId())) {
				orderTableView.getSelectionModel().select(order);
				newSelectedOrderIdSet.add(order.getOrderId());
			}
		}
		selectedOrderIdSet = newSelectedOrderIdSet;

	}

	private void createLayout() {

		orderTableView.setTableMenuButtonVisible(true);

		TableColumn<OrderField, Pane> unifiedSymbolCol = new TableColumn<>("合约");
		unifiedSymbolCol.setPrefWidth(160);
		unifiedSymbolCol.setCellValueFactory(feature -> {
			VBox vBox = new VBox();
			try {
				OrderField order = feature.getValue();
				Text unifiedSymbolText = new Text(order.getContract().getUnifiedSymbol());
				Text shortNameText = new Text(order.getContract().getShortName());
				vBox.getChildren().add(unifiedSymbolText);
				vBox.getChildren().add(shortNameText);

				if (guiMainService.getSelectedContract() != null
						&& guiMainService.getSelectedContract().getUnifiedSymbol().equals(order.getContract().getUnifiedSymbol())) {
					unifiedSymbolText.getStyleClass().add("trade-remind-color");
				}

				vBox.setUserData(order);
			} catch (Exception e) {
				logger.error("渲染错误", e);
			}
			return new SimpleObjectProperty<>(vBox);
		});

		unifiedSymbolCol.setComparator((Pane p1, Pane p2) -> {
			try {
				OrderField order1 = (OrderField) p1.getUserData();
				OrderField order2 = (OrderField) p2.getUserData();
				return StringUtils.compare(order1.getContract().getUnifiedSymbol(), order2.getContract().getUnifiedSymbol());
			} catch (Exception e) {
				logger.error("排序错误", e);
			}
			return 0;
		});

		orderTableView.getColumns().add(unifiedSymbolCol);

		TableColumn<OrderField, Text> directionCol = new TableColumn<>("方向");
		directionCol.setPrefWidth(40);
		directionCol.setCellValueFactory(feature -> {
			Text directionText = new Text("未知");

			try {
				OrderField order = feature.getValue();

				if (order.getDirection() == DirectionEnum.LONG) {
					directionText.setText("多");
					directionText.getStyleClass().add("trade-long-color");
				} else if (order.getDirection() == DirectionEnum.SHORT) {
					directionText.setText("空");
					directionText.getStyleClass().add("trade-short-color");
				} else if (order.getDirection() == DirectionEnum.NET) {
					directionText.setText("净");
				}

				directionText.setUserData(order);
			} catch (Exception e) {
				logger.error("渲染错误", e);
			}

			return new SimpleObjectProperty<>(directionText);
		});

		directionCol.setComparator((Text t1, Text t2) -> {
			try {
				OrderField order1 = (OrderField) t1.getUserData();
				OrderField order2 = (OrderField) t2.getUserData();
				return Integer.compare(order1.getDirection().getNumber(), order2.getDirection().getNumber());
			} catch (Exception e) {
				logger.error("排序错误", e);
			}
			return 0;
		});

		orderTableView.getColumns().add(directionCol);

		TableColumn<OrderField, String> offsetCol = new TableColumn<>("开平");
		offsetCol.setPrefWidth(40);
		offsetCol.setCellValueFactory(feature -> {
			String offset = "未知";

			try {
				OrderField order = feature.getValue();

				if (order.getOffset() == OffsetEnum.CLOSE) {
					offset = "平";
				} else if (order.getOffset() == OffsetEnum.CLOSE_TODAY) {
					offset = "平今";
				} else if (order.getOffset() == OffsetEnum.CLOSE_YESTERDAY) {

					offset = "平昨";
					;
				} else if (order.getOffset() == OffsetEnum.OPEN) {

					offset = "开";
					;
				}
			} catch (Exception e) {
				logger.error("渲染错误", e);
			}

			return new SimpleStringProperty(offset);
		});
		offsetCol.setComparator((String s1, String s2) -> StringUtils.compare(s1, s2));
		orderTableView.getColumns().add(offsetCol);

		TableColumn<OrderField, String> priceCol = new TableColumn<>("价格");
		priceCol.setPrefWidth(80);
		priceCol.setCellValueFactory(feature -> {
			String priceString = "";
			try {

				OrderField order = feature.getValue();
				ContractField contract = order.getContract();
				int dcimalDigits = CommonUtils.getNumberDecimalDigits(contract.getPriceTick());
				if (dcimalDigits < 0) {
					dcimalDigits = 0;
				}
				String priceStringFormat = "%,." + dcimalDigits + "f";
				priceString = String.format(priceStringFormat, order.getPrice());
			} catch (Exception e) {
				logger.error("渲染错误", e);
			}
			return new SimpleStringProperty(priceString);

		});
		priceCol.setComparator((String s1, String s2) -> {
			try {
				return Double.compare(Double.valueOf(s1.replaceAll(",", "")), Double.valueOf(s2.replaceAll(",", "")));
			} catch (Exception e) {
				logger.error("排序错误", e);
			}
			return 0;
		});
		orderTableView.getColumns().add(priceCol);

		TableColumn<OrderField, Pane> totalVolumeCol = new TableColumn<>("数量");
		totalVolumeCol.setPrefWidth(70);
		totalVolumeCol.setCellValueFactory(feature -> {
			VBox vBox = new VBox();
			try {

				OrderField order = feature.getValue();

				HBox totalVolumeHBox = new HBox();
				Text totalVolumeLabelText = new Text("总计");
				totalVolumeLabelText.setWrappingWidth(35);
				totalVolumeLabelText.getStyleClass().add("trade-label");
				totalVolumeHBox.getChildren().add(totalVolumeLabelText);

				Text totalVolumeText = new Text("" + order.getTotalVolume());
				totalVolumeHBox.getChildren().add(totalVolumeText);
				vBox.getChildren().add(totalVolumeHBox);

				HBox tradedVolumeHBox = new HBox();
				Text tradedVolumeLabelText = new Text("成交");
				tradedVolumeLabelText.setWrappingWidth(35);
				tradedVolumeLabelText.getStyleClass().add("trade-label");
				tradedVolumeHBox.getChildren().add(tradedVolumeLabelText);

				Text tradedVolumeText = new Text("" + order.getTradedVolume());
				tradedVolumeHBox.getChildren().add(tradedVolumeText);
				vBox.getChildren().add(tradedVolumeHBox);

				vBox.setUserData(order);
				if (CommonConstant.ORDER_STATUS_WORKING_SET.contains(order.getOrderStatus())) {
					if (order.getDirection() == DirectionEnum.LONG) {
						totalVolumeText.getStyleClass().add("trade-long-color");
					} else if (order.getDirection() == DirectionEnum.SHORT) {
						totalVolumeText.getStyleClass().add("trade-short-color");
					}
					tradedVolumeText.getStyleClass().add("trade-info-color");
				}
			} catch (Exception e) {
				logger.error("渲染错误", e);
			}

			return new SimpleObjectProperty<>(vBox);
		});

		totalVolumeCol.setComparator((Pane p1, Pane p2) -> {
			try {
				OrderField order1 = (OrderField) p1.getUserData();
				OrderField order2 = (OrderField) p2.getUserData();
				return Integer.compare(order1.getTotalVolume(), order2.getTotalVolume());
			} catch (Exception e) {
				logger.error("排序错误", e);
			}
			return 0;
		});

		orderTableView.getColumns().add(totalVolumeCol);

		TableColumn<OrderField, Text> statusCol = new TableColumn<>("状态");
		statusCol.setPrefWidth(60);
		statusCol.setCellValueFactory(feature -> {

			Text statusText = new Text("未知");
			try {
				OrderField order = feature.getValue();

				if (order.getOrderStatus() == OrderStatusEnum.ALL_TRADED) {
					statusText.setText("全部成交");
				} else if (order.getOrderStatus() == OrderStatusEnum.CANCELLED) {
					statusText.setText("已撤销");
				} else if (order.getOrderStatus() == OrderStatusEnum.NOT_TRADED) {
					statusText.setText("未成交");
					statusText.getStyleClass().add("trade-remind-color");
				} else if (order.getOrderStatus() == OrderStatusEnum.PART_TRADED) {
					statusText.setText("部分成交");
					statusText.getStyleClass().add("trade-remind-color");
				} else if (order.getOrderStatus() == OrderStatusEnum.REJECTED) {
					statusText.setText("拒单");
				} else if (order.getOrderStatus() == OrderStatusEnum.SUBMITTING) {
					statusText.setText("提交中");
					statusText.getStyleClass().add("trade-remind-color");
				}
			} catch (Exception e) {
				logger.error("渲染错误", e);
			}

			return new SimpleObjectProperty<>(statusText);
		});
		statusCol.setComparator((Text t1, Text t2) -> {
			try {
				OrderField order1 = (OrderField) t1.getUserData();
				OrderField order2 = (OrderField) t2.getUserData();
				return Integer.compare(order1.getOrderStatus().getNumber(), order2.getOrderStatus().getNumber());
			} catch (Exception e) {
				logger.error("排序错误", e);
			}
			return 0;
		});

		orderTableView.getColumns().add(statusCol);

		TableColumn<OrderField, String> statusInfoCol = new TableColumn<>("状态信息");
		statusInfoCol.setPrefWidth(130);
		statusInfoCol.setCellValueFactory(feature -> {
			String statusInfo = "";
			try {
				statusInfo = feature.getValue().getStatusInfo();
			} catch (Exception e) {
				logger.error("渲染错误", e);
			}
			return new SimpleStringProperty(statusInfo);
		});
		statusInfoCol.setComparator((String s1, String s2) -> StringUtils.compare(s1, s2));
		orderTableView.getColumns().add(statusInfoCol);

		TableColumn<OrderField, Pane> timeCol = new TableColumn<>("时间");
		timeCol.setPrefWidth(90);
		timeCol.setCellValueFactory(feature -> {
			VBox vBox = new VBox();
			try {
				OrderField order = feature.getValue();

				HBox orderTimeHBox = new HBox();
				Text orderTimeLabelText = new Text("定单");
				orderTimeLabelText.setWrappingWidth(35);
				orderTimeLabelText.getStyleClass().add("trade-label");
				orderTimeHBox.getChildren().add(orderTimeLabelText);

				Text totalVolumeText = new Text("" + order.getOrderTime());
				orderTimeHBox.getChildren().add(totalVolumeText);
				vBox.getChildren().add(orderTimeHBox);

				HBox updateTimeHBox = new HBox();
				Text updateTimeLabelText = new Text("更新");
				updateTimeLabelText.setWrappingWidth(35);
				updateTimeLabelText.getStyleClass().add("trade-label");
				updateTimeHBox.getChildren().add(updateTimeLabelText);

				Text updateTimeText = new Text("" + order.getUpdateTime());
				updateTimeHBox.getChildren().add(updateTimeText);
				vBox.getChildren().add(updateTimeHBox);

				vBox.setUserData(order);
			} catch (Exception e) {
				logger.error("渲染错误", e);
			}
			return new SimpleObjectProperty<>(vBox);
		});

		timeCol.setComparator((Pane p1, Pane p2) -> {
			try {
				OrderField order1 = (OrderField) p1.getUserData();
				OrderField order2 = (OrderField) p2.getUserData();
				return StringUtils.compare(order1.getOrderTime(), order2.getOrderTime());
			} catch (Exception e) {
				logger.error("排序错误", e);
			}
			return 0;
		});

		timeCol.setSortType(SortType.DESCENDING);

		orderTableView.getColumns().add(timeCol);

		TableColumn<OrderField, String> adapterOrderIdCol = new TableColumn<>("编号");
		adapterOrderIdCol.setPrefWidth(60);
		adapterOrderIdCol.setCellValueFactory(feature -> {
			String adapterOrderId = "";
			try {
				adapterOrderId = feature.getValue().getAdapterOrderId();
			} catch (Exception e) {
				logger.error("渲染错误", e);
			}
			return new SimpleStringProperty(adapterOrderId);
		});
		statusInfoCol.setComparator((String s1, String s2) -> StringUtils.compare(s1, s2));
		orderTableView.getColumns().add(adapterOrderIdCol);

		TableColumn<OrderField, String> accountIdCol = new TableColumn<>("账户ID");
		accountIdCol.setPrefWidth(340);
		accountIdCol.setCellValueFactory(feature -> {
			String accountId = "";
			try {
				accountId = feature.getValue().getAccountId();
			} catch (Exception e) {
				logger.error("渲染错误", e);
			}
			return new SimpleStringProperty(accountId);
		});
		accountIdCol.setComparator((String s1, String s2) -> StringUtils.compare(s1, s2));
		orderTableView.getColumns().add(accountIdCol);

		TableColumn<OrderField, String> originOrderIdCol = new TableColumn<>("原始编号");
		originOrderIdCol.setPrefWidth(260);
		originOrderIdCol.setCellValueFactory(feature -> {
			String originOrderId = "";
			try {
				originOrderId = feature.getValue().getOriginOrderId();
			} catch (Exception e) {
				logger.error("渲染错误", e);
			}
			return new SimpleStringProperty(originOrderId);
		});
		originOrderIdCol.setSortable(false);
		orderTableView.getColumns().add(originOrderIdCol);

		SortedList<OrderField> sortedItems = new SortedList<>(orderObservableList);
		orderTableView.setItems(sortedItems);
		sortedItems.comparatorProperty().bind(orderTableView.comparatorProperty());

		orderTableView.getSortOrder().add(timeCol);

		orderTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		orderTableView.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				ObservableList<OrderField> selectedItems = orderTableView.getSelectionModel().getSelectedItems();
				selectedOrderIdSet.clear();
				for (OrderField row : selectedItems) {
					selectedOrderIdSet.add(row.getOrderId());
				}
			}
		});

		orderTableView.setRowFactory(tv -> {
			TableRow<OrderField> row = new TableRow<>();
			row.setOnMousePressed(event -> {
				if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
					ObservableList<OrderField> selectedItems = orderTableView.getSelectionModel().getSelectedItems();
					selectedOrderIdSet.clear();
					for (OrderField order : selectedItems) {
						selectedOrderIdSet.add(order.getOrderId());
					}
					OrderField clickedItem = row.getItem();
					guiMainService.updateSelectedContarct(clickedItem.getContract());
				} else if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {

					CancelOrderReqField.Builder cancelOrderReqFieldBuilder = CancelOrderReqField.newBuilder();
					cancelOrderReqFieldBuilder.setOrderId(row.getItem().getOrderId());
					cancelOrderReqFieldBuilder.setOriginOrderId(row.getItem().getOriginOrderId());
					rpcClientApiService.asyncCancelOrder(cancelOrderReqFieldBuilder.build(), UUIDStringPoolUtils.getUUIDString());
				}
			});
			return row;
		});

		orderTableView.setFocusTraversable(false);

		vBox.getChildren().add(orderTableView);
		VBox.setVgrow(orderTableView, Priority.ALWAYS);

		HBox hBox = new HBox();
		RadioButton allRadioButton = new RadioButton("全部");
		RadioButton cancelableRadioButton = new RadioButton("可撤销");
		RadioButton cancelledRadioButton = new RadioButton("已撤销");
		ToggleGroup toggleGroup = new ToggleGroup();
		allRadioButton.setToggleGroup(toggleGroup);
		allRadioButton.setUserData(SHOW_ALL);
		cancelableRadioButton.setToggleGroup(toggleGroup);
		cancelableRadioButton.setUserData(SHOW_CANCELABLE);
		cancelledRadioButton.setToggleGroup(toggleGroup);
		cancelledRadioButton.setUserData(SHOW_CANCELLED);
		allRadioButton.setSelected(true);
		toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
				showRadioValue = (int) newValue.getUserData();
				fillingData();

			};
		});

		hBox.getChildren().add(allRadioButton);
		hBox.getChildren().add(cancelableRadioButton);
		hBox.getChildren().add(cancelledRadioButton);

		CheckBox showRejectedCheckBox = new CheckBox("显示拒单");
		showRejectedCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				showRejectedChecked = newValue;
				fillingData();
			}
		});

		hBox.getChildren().add(showRejectedCheckBox);

		vBox.getChildren().add(hBox);

	}

}
