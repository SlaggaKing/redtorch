package xyz.redtorch.desktop.layout.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import xyz.redtorch.common.util.CommonUtils;
import xyz.redtorch.common.util.UUIDStringPoolUtils;
import xyz.redtorch.desktop.rpc.service.RpcClientApiService;
import xyz.redtorch.desktop.service.DesktopTradeCachesService;
import xyz.redtorch.desktop.service.GuiMainService;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetEnum;
import xyz.redtorch.pb.CoreEnum.PriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionTypeEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Component
public class OrderPanelLayout {

	private static final String PRICE_FILL_METHOD_MANUAL = "MANUAL";
	private static final String PRICE_FILL_METHOD_LAST = "LAST";
	private static final String PRICE_FILL_METHOD_ACTIVE = "ACTIVE";
	private static final String PRICE_FILL_METHOD_QUEUE = "QUEUE";
	private static final String PRICE_FILL_METHOD_UPPER_OR_LOWER_LIMIT = "UPPER_OR_LOWER_LIMIT";

	private boolean layoutCreated = false;
	private TabPane tabPane = new TabPane();

	private ToggleGroup priceFillMethodToggleGroup = new ToggleGroup();
	private ToggleGroup priceTypeToggleGroup = new ToggleGroup();
	private TextField priceTextField = new TextField();
	private TextField volumeTextField = new TextField("0");

	private VBox accountVolumeVBox = new VBox();

	private Insets commonInsets = new Insets(5, 0, 0, 0);

	private PriceTypeEnum priceType = PriceTypeEnum.LIMIT;
	private String priceFillMethod = PRICE_FILL_METHOD_LAST;
	private Double price = null;
	private Integer volume = 0;

	private Map<String, Integer> accountVolumeMap = new HashMap<>();

	private TickField tick = null;
	private int dcimalDigits = 4;

	@Autowired
	private GuiMainService guiMainService;
	@Autowired
	private RpcClientApiService rpcClientApiService;
	@Autowired
	private DesktopTradeCachesService desktopTradeCachesService;

	private boolean submitPending = false;

	public void updateAccountVolumeMap(Map<String, Integer> accountVolumeMap) {
		this.accountVolumeMap.putAll(accountVolumeMap);
		this.fillingData();
	}

	public void fillingData() {

		Set<String> selectedAccountIdSet = guiMainService.getSelectedAccountIdSet();

		accountVolumeMap = accountVolumeMap.entrySet().stream().filter(map -> selectedAccountIdSet.contains(map.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		System.out.println(JSON.toJSONString(accountVolumeMap));

		accountVolumeVBox.getChildren().clear();
		for (String selectedAccountId : selectedAccountIdSet) {

			AccountField account = desktopTradeCachesService.queryAccountByAccountId(selectedAccountId);
			TextField holderTextField = new TextField("");
			if (account != null) {
				holderTextField.setText(account.getHolder());
			}
			holderTextField.setDisable(true);
			TextField accountIdTextField = new TextField(selectedAccountId);
			accountIdTextField.setEditable(false);
			TextField accountVolumeTextField = new TextField();

			if (accountVolumeMap.containsKey(selectedAccountId)) {
				accountVolumeTextField.setText(accountVolumeMap.get(selectedAccountId) + "");
			} else {
				accountVolumeTextField.setText(volume + "");
			}

			accountVolumeTextField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					String volumeString = newValue;
					if (!newValue.matches("\\d*")) {
						volumeString = newValue.replaceAll("[^\\d]", "");
						accountVolumeTextField.setText(volumeString);
					}

					if (!volumeString.isBlank()) {
						Integer accountVolume = Integer.valueOf(volumeString);
						accountVolumeMap.put(selectedAccountId, accountVolume);
					}

				}
			});

			Button accountVolumeIncreaseButton = new Button("+");
			accountVolumeIncreaseButton.setPrefWidth(120);
			accountVolumeIncreaseButton.setPrefHeight(15);
			Button accountVolumeDecreaseButton = new Button("-");
			accountVolumeDecreaseButton.setPrefWidth(120);
			accountVolumeDecreaseButton.setPrefHeight(15);
			HBox accountVolumeButtonHBox = new HBox();
			accountVolumeButtonHBox.getChildren().addAll(accountVolumeDecreaseButton, accountVolumeIncreaseButton);

			accountVolumeIncreaseButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						Integer accountVolume = volume;
						if (accountVolumeMap.containsKey(selectedAccountId)) {
							accountVolume = accountVolumeMap.get(selectedAccountId);
						}
						if (accountVolume < 1000000) {
							accountVolume += 1;
						}
						accountVolumeMap.put(selectedAccountId, accountVolume);
						accountVolumeTextField.setText("" + accountVolume);
					}
				}
			});

			accountVolumeDecreaseButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						Integer accountVolume = volume;
						if (accountVolumeMap.containsKey(selectedAccountId)) {
							accountVolume = accountVolumeMap.get(selectedAccountId);
						}
						accountVolume -= 1;
						if (accountVolume < 0) {
							accountVolume = 0;
						}
						accountVolumeMap.put(selectedAccountId, accountVolume);
						accountVolumeTextField.setText("" + accountVolume);
					}
				}
			});

			VBox lineVbox = new VBox();
			lineVbox.setPadding(commonInsets);
			lineVbox.getChildren().addAll(holderTextField, accountIdTextField, accountVolumeTextField, accountVolumeButtonHBox);

			accountVolumeVBox.getChildren().add(lineVbox);
		}

	}

	public Node getNode() {
		if (!layoutCreated) {
			createLayout();
			fillingData();
			layoutCreated = true;
		}
		return this.tabPane;
	}

	public void updateData(TickField tick) {
		if (tick == null || this.tick == null || !tick.equals(this.tick)) {
			if (this.tick != null && tick != null && !this.tick.getContract().getUnifiedSymbol().equals(tick.getContract().getUnifiedSymbol())) {
				this.price = null;
				this.priceTextField.setText("");

				this.volume = 0;
				this.volumeTextField.setText("0");

				accountVolumeMap.clear();

				fillingData();
			}
			this.tick = tick;

			if (tick != null) {
				dcimalDigits = CommonUtils.getNumberDecimalDigits(tick.getContract().getPriceTick());
				if (dcimalDigits < 0) {
					dcimalDigits = 0;
				}
			}

			if (PRICE_FILL_METHOD_LAST.equals(priceFillMethod)) {
				if (tick != null && tick.getLastPrice() != Double.MAX_VALUE) {
					price = tick.getLastPrice();
					priceTextField.setText(String.format("%." + dcimalDigits + "f", price));
				} else {
					price = null;
					priceTextField.setText("");
				}
			} else if (PRICE_FILL_METHOD_ACTIVE.equals(priceFillMethod)) {
				price = null;
				priceTextField.setText("对手价");
			} else if (PRICE_FILL_METHOD_QUEUE.equals(priceFillMethod)) {
				price = null;
				priceTextField.setText("排队价");
			} else if (PRICE_FILL_METHOD_UPPER_OR_LOWER_LIMIT.equals(priceFillMethod)) {
				price = null;
				priceTextField.setText("涨跌停");
			}
		}
	}

	private void createLayout() {

		Tab domesticTab = new Tab("内盘");
		domesticTab.setClosable(false);
		tabPane.getTabs().add(domesticTab);
		{
			HBox contentHBox = new HBox();
			tabPane.setPrefWidth(420);
			tabPane.setMinWidth(420);
			domesticTab.setContent(contentHBox);
			VBox leftVBox = new VBox();
			leftVBox.setPrefWidth(140);
			leftVBox.setStyle("-fx-border-color: rgb(220, 220, 220);-fx-border-style: dashed;-fx-border-width: 0 1 0 0;");
			leftVBox.setPadding(new Insets(5));
			contentHBox.getChildren().add(leftVBox);

			RadioButton priceTypeLimitRadioButton = new RadioButton("限价");
			priceTypeLimitRadioButton.setUserData(PriceTypeEnum.LIMIT);
			priceTypeLimitRadioButton.setPrefWidth(60);
			RadioButton priceTypeMarketRadioButton = new RadioButton("市价");
			priceTypeMarketRadioButton.setUserData(PriceTypeEnum.MARKET);
			priceTypeMarketRadioButton.setPrefWidth(60);
			RadioButton priceTypeFOKRadioButton = new RadioButton("FOK");
			priceTypeMarketRadioButton.setUserData(PriceTypeEnum.FOK);
			priceTypeFOKRadioButton.setPrefWidth(60);
			RadioButton priceTypeFAKRadioButton = new RadioButton("FAK");
			priceTypeMarketRadioButton.setUserData(PriceTypeEnum.FAK);
			priceTypeFAKRadioButton.setPrefWidth(60);

			priceTypeLimitRadioButton.setToggleGroup(priceTypeToggleGroup);
			priceTypeMarketRadioButton.setToggleGroup(priceTypeToggleGroup);
			priceTypeFOKRadioButton.setToggleGroup(priceTypeToggleGroup);
			priceTypeFAKRadioButton.setToggleGroup(priceTypeToggleGroup);

			priceTypeLimitRadioButton.setSelected(true);
			priceTypeToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
				public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
					priceType = (PriceTypeEnum) newValue.getUserData();
				}
			});

			HBox priceTypeLine1HBox = new HBox();
			priceTypeLine1HBox.setPadding(commonInsets);
			priceTypeLine1HBox.getChildren().addAll(priceTypeLimitRadioButton, priceTypeMarketRadioButton);
			HBox priceTypeLine2HBox = new HBox();
			priceTypeLine2HBox.setPadding(commonInsets);
			priceTypeLine2HBox.getChildren().addAll(priceTypeFOKRadioButton, priceTypeFAKRadioButton);

			Label priceTypeLabel = new Label("价格类型");
			priceTypeLabel.setPadding(commonInsets);
			leftVBox.getChildren().addAll(priceTypeLabel, priceTypeLine1HBox, priceTypeLine2HBox);

			RadioButton priceFillMethodQueueRadioButton = new RadioButton("排队价");
			priceFillMethodQueueRadioButton.setUserData(PRICE_FILL_METHOD_QUEUE);
			priceFillMethodQueueRadioButton.setPrefWidth(60);
			RadioButton priceFillMethodLastRadioButton = new RadioButton("最新");
			priceFillMethodLastRadioButton.setUserData(PRICE_FILL_METHOD_LAST);
			priceFillMethodLastRadioButton.setPrefWidth(60);
			RadioButton priceFillMethodActiveRadioButton = new RadioButton("对手");
			priceFillMethodActiveRadioButton.setUserData(PRICE_FILL_METHOD_ACTIVE);
			priceFillMethodActiveRadioButton.setPrefWidth(60);
			RadioButton priceFillMethodUpperOrLowerLimitRadioButton = new RadioButton("涨跌停");
			priceFillMethodUpperOrLowerLimitRadioButton.setUserData(PRICE_FILL_METHOD_UPPER_OR_LOWER_LIMIT);
			priceFillMethodUpperOrLowerLimitRadioButton.setPrefWidth(60);
			RadioButton priceFillMethodManualRadioButton = new RadioButton("手动");
			priceFillMethodManualRadioButton.setUserData(PRICE_FILL_METHOD_MANUAL);
			priceFillMethodManualRadioButton.setPrefWidth(60);

			priceFillMethodQueueRadioButton.setToggleGroup(priceFillMethodToggleGroup);
			priceFillMethodLastRadioButton.setToggleGroup(priceFillMethodToggleGroup);
			priceFillMethodActiveRadioButton.setToggleGroup(priceFillMethodToggleGroup);
			priceFillMethodUpperOrLowerLimitRadioButton.setToggleGroup(priceFillMethodToggleGroup);
			priceFillMethodManualRadioButton.setToggleGroup(priceFillMethodToggleGroup);

			priceFillMethodLastRadioButton.setSelected(true);
			priceTextField.setDisable(true);

			priceFillMethodToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
				public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
					priceFillMethod = (String) newValue.getUserData();
					if (PRICE_FILL_METHOD_MANUAL.equals(priceFillMethod)) {
						priceTextField.setDisable(false);
						if (price == null) {
							if (tick != null && tick.getLastPrice() != Double.MAX_VALUE) {
								price = tick.getLastPrice();
								priceTextField.setText(String.format("%." + dcimalDigits + "f", price));
							} else {
								priceTextField.setText("");
							}
						}

					} else {
						priceTextField.setDisable(true);

						if (PRICE_FILL_METHOD_ACTIVE.equals(priceFillMethod)) {
							price = null;
							priceTextField.setText("对手价");
						} else if (PRICE_FILL_METHOD_QUEUE.equals(priceFillMethod)) {
							price = null;
							priceTextField.setText("排队价");
						} else if (PRICE_FILL_METHOD_UPPER_OR_LOWER_LIMIT.equals(priceFillMethod)) {
							price = null;
							priceTextField.setText("涨跌停");
						} else if (PRICE_FILL_METHOD_LAST.equals(priceFillMethod)) {
							if (tick != null && tick.getLastPrice() != Double.MAX_VALUE) {
								price = tick.getLastPrice();
								priceTextField.setText(String.format("%." + dcimalDigits + "f", price));
							} else {
								price = null;
								priceTextField.setText("");
							}
						}
					}
					fillingData();
				}
			});

			HBox priceFillMethodLine1HBox = new HBox();
			priceFillMethodLine1HBox.setPadding(commonInsets);
			priceFillMethodLine1HBox.getChildren().addAll(priceFillMethodQueueRadioButton, priceFillMethodLastRadioButton);
			HBox priceFillMethodLine2HBox = new HBox();
			priceFillMethodLine2HBox.setPadding(commonInsets);
			priceFillMethodLine2HBox.getChildren().addAll(priceFillMethodActiveRadioButton, priceFillMethodUpperOrLowerLimitRadioButton);
			HBox priceFillMethodLine3HBox = new HBox();
			priceFillMethodLine3HBox.setPadding(commonInsets);
			priceFillMethodLine3HBox.getChildren().addAll(priceFillMethodManualRadioButton);

			Label priceFillMethodLabel = new Label("填价方式");
			priceFillMethodLabel.setPadding(commonInsets);
			leftVBox.getChildren().addAll(priceFillMethodLabel, priceFillMethodLine1HBox, priceFillMethodLine2HBox, priceFillMethodLine3HBox);

			priceTextField.setPrefWidth(120);
			priceTextField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					Pattern pattern = Pattern.compile("-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?");

					if (!(newValue.isEmpty() || "-".equals(newValue) || ".".equals(newValue) || "-.".equals(newValue) || pattern.matcher(newValue).matches()
							|| "涨跌停".equals(newValue) || "对手价".equals(newValue) || "排队价".equals(newValue))) {
						priceTextField.setText(oldValue);
					} else {
						if (newValue.isEmpty() || "-".equals(newValue) || ".".equals(newValue) || "-.".equals(newValue) 
								|| "涨跌停".equals(newValue) || "对手价".equals(newValue) || "排队价".equals(newValue)) {
							price = null;
						} else if (pattern.matcher(newValue).matches()) {
							price = Double.valueOf(newValue);
						}
					}

				}

			});

			Button priceIncreaseButton = new Button("+");
			priceIncreaseButton.setPrefWidth(65);
			priceIncreaseButton.setPrefHeight(15);
			Button priceDecreaseButton = new Button("-");
			priceDecreaseButton.setPrefWidth(65);
			priceDecreaseButton.setPrefHeight(15);
			HBox priceButtonHBox = new HBox();
			priceButtonHBox.getChildren().addAll(priceDecreaseButton, priceIncreaseButton);
			Label priceLabel = new Label("价格");
			priceLabel.setPadding(commonInsets);
			leftVBox.getChildren().addAll(priceLabel, priceTextField, priceButtonHBox);

			priceIncreaseButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						if (tick != null && guiMainService.getSelectedContract() != null) {
							if(price==null) {
								if(tick.getLastPrice()!=Double.MAX_VALUE) {
									price = tick.getLastPrice() + guiMainService.getSelectedContract().getPriceTick();
								}
							}else {
								price = price + guiMainService.getSelectedContract().getPriceTick();
							}
							priceFillMethod = PRICE_FILL_METHOD_MANUAL;
							priceFillMethodManualRadioButton.setSelected(true);
							priceTextField.setText(String.format("%." + dcimalDigits + "f", price));
						}
					}
				}
			});

			priceDecreaseButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						if (tick != null && guiMainService.getSelectedContract() != null) {
							if(price==null) {
								if(tick.getLastPrice()!=Double.MAX_VALUE) {
									price = tick.getLastPrice() - guiMainService.getSelectedContract().getPriceTick();
								}
							}else {
								price = price - guiMainService.getSelectedContract().getPriceTick();
							}
							priceFillMethod = PRICE_FILL_METHOD_MANUAL;
							priceFillMethodManualRadioButton.setSelected(true);
							priceTextField.setText(String.format("%." + dcimalDigits + "f", price));
						}
					}
				}
			});

			volumeTextField.setPrefWidth(120);
			volumeTextField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					String volumeString = newValue;
					if (!newValue.matches("\\d*")) {
						volumeString = newValue.replaceAll("[^\\d]", "");
						volumeTextField.setText(volumeString);
					}
					if (volumeString.isBlank()) {
						volume = 0;
					} else {
						volume = Integer.valueOf(volumeString);
					}
				}
			});

			Button volumeIncreaseButton = new Button("+");
			volumeIncreaseButton.setPrefWidth(65);
			volumeIncreaseButton.setPrefHeight(15);
			Button volumeDecreaseButton = new Button("-");
			volumeDecreaseButton.setPrefWidth(65);
			volumeDecreaseButton.setPrefHeight(15);
			HBox volumeButtonHBox = new HBox();
			volumeButtonHBox.getChildren().addAll(volumeDecreaseButton, volumeIncreaseButton);
			Label volumeLabel = new Label("全局数量");
			volumeLabel.setPadding(commonInsets);
			leftVBox.getChildren().addAll(volumeLabel, volumeTextField, volumeButtonHBox);

			volumeIncreaseButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						if (volume < 1000000) {
							volume += 1;
						}
						volumeTextField.setText("" + volume);
						fillingData();
					}
				}
			});

			volumeDecreaseButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						volume -= 1;
						if (volume < 0) {
							volume = 0;
						}
						volumeTextField.setText("" + volume);
						fillingData();
					}
				}
			});

//	        RadioButton timeConditionTypeGTCRadioButton = new RadioButton("GTC(撤销前有效)");
//	        timeConditionTypeGTCRadioButton.setPrefWidth(120);
//	        RadioButton timeConditionTypeGFDRadioButton = new RadioButton("GFD(当日有效)");
//	        timeConditionTypeGFDRadioButton.setPrefWidth(120);
//	        ToggleGroup timeConditionTypeToggleGroup = new ToggleGroup();
//	        timeConditionTypeGTCRadioButton.setToggleGroup(timeConditionTypeToggleGroup);
//	        timeConditionTypeGFDRadioButton.setToggleGroup(timeConditionTypeToggleGroup);
//	        
//	        HBox timeConditionTypeLine1HBox = new HBox();
//	        timeConditionTypeLine1HBox.setPadding(commonInsets);
//	        timeConditionTypeLine1HBox.getChildren().addAll(timeConditionTypeGTCRadioButton);
//	        HBox timeConditionTypeLine2HBox = new HBox();
//	        timeConditionTypeLine2HBox.setPadding(commonInsets);
//	        timeConditionTypeLine2HBox.getChildren().addAll(timeConditionTypeGFDRadioButton);
//	        Label timeConditionTypeLabel = new Label("时效");
//	        timeConditionTypeLabel.setPadding(commonInsets);
//	        leftVBox.getChildren().addAll(timeConditionTypeLabel,timeConditionTypeLine1HBox,timeConditionTypeLine2HBox);

			VBox rightVBox = new VBox();
			contentHBox.getChildren().add(rightVBox);
			rightVBox.setPrefWidth(275);
			rightVBox.setPadding(new Insets(5));

			VBox accountVolumeWrapVBox = new VBox();
			rightVBox.getChildren().add(accountVolumeWrapVBox);
			VBox.setVgrow(accountVolumeWrapVBox, Priority.ALWAYS);
			accountVolumeWrapVBox.setStyle("-fx-border-color: rgb(220, 220, 220);-fx-border-style: dashed;-fx-border-width: 0 1 0 0;");
			accountVolumeWrapVBox.setPadding(new Insets(5, 0, 0, 0));

			ScrollPane accountVolumeScrollPane = new ScrollPane();
			accountVolumeScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
			accountVolumeWrapVBox.getChildren().addAll(new Text("特定账户数量"), accountVolumeScrollPane);
			VBox.setVgrow(accountVolumeScrollPane, Priority.ALWAYS);

			accountVolumeVBox.setPadding(new Insets(0, 0, 0, 2));
			accountVolumeScrollPane.setContent(accountVolumeVBox);

			Insets buttonMarginInsets = new Insets(3, 3, 0, 0);

			String buttonStyle = "-fx-light-text-color: rgb(255, 255, 255); -fx-mid-text-color: rgb(255, 255, 255); -fx-dark-text-color: rgb(255, 255, 255);";

			VBox buttonVBox = new VBox();
			rightVBox.getChildren().add(buttonVBox);

			Button buyButton = new Button("多开");
			buyButton.getStyleClass().add("trade-long-color-background");
			buyButton.setStyle(buttonStyle);
			buyButton.setPrefWidth(60);
			buyButton.setPrefHeight(40);
			HBox.setMargin(buyButton, buttonMarginInsets);
			Button shortButton = new Button("空开");
			shortButton.getStyleClass().add("trade-short-color-background");
			shortButton.setStyle(buttonStyle);
			shortButton.setPrefWidth(60);
			shortButton.setPrefHeight(40);
			HBox.setMargin(shortButton, buttonMarginInsets);

			Button coverButton = new Button("平空");
			coverButton.getStyleClass().add("trade-long-color-background");
			coverButton.setStyle(buttonStyle);
			coverButton.setPrefWidth(60);
			coverButton.setPrefHeight(40);
			HBox.setMargin(coverButton, buttonMarginInsets);
			Button sellButton = new Button("平多");
			sellButton.getStyleClass().add("trade-short-color-background");
			sellButton.setStyle(buttonStyle);
			sellButton.setPrefWidth(60);
			sellButton.setPrefHeight(40);
			HBox.setMargin(sellButton, buttonMarginInsets);

			Button coverTDButton = new Button("平今空");
			coverTDButton.getStyleClass().add("trade-long-color-background");
			coverTDButton.setStyle(buttonStyle);
			coverTDButton.setPrefWidth(60);
			coverTDButton.setPrefHeight(40);
			HBox.setMargin(coverTDButton, buttonMarginInsets);
			Button sellTDButton = new Button("平今多");
			sellTDButton.getStyleClass().add("trade-short-color-background");
			sellTDButton.setStyle(buttonStyle);
			sellTDButton.setPrefWidth(60);
			sellTDButton.setPrefHeight(40);
			HBox.setMargin(sellTDButton, buttonMarginInsets);

			Button coverYDButton = new Button("平昨空");
			coverYDButton.getStyleClass().add("trade-long-color-background");
			coverYDButton.setStyle(buttonStyle);
			coverYDButton.setPrefWidth(60);
			coverYDButton.setPrefHeight(40);
			HBox.setMargin(coverYDButton, buttonMarginInsets);
			Button sellYDButton = new Button("平昨多");
			sellYDButton.getStyleClass().add("trade-short-color-background");
			sellYDButton.setStyle(buttonStyle);
			sellYDButton.setPrefWidth(60);
			sellYDButton.setPrefHeight(40);
			HBox.setMargin(sellYDButton, buttonMarginInsets);
			buyButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						submitOrder(DirectionEnum.LONG, OffsetEnum.OPEN);
					}
				}
			});

			shortButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						submitOrder(DirectionEnum.SHORT, OffsetEnum.OPEN);
					}
				}
			});

			coverButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						submitOrder(DirectionEnum.LONG, OffsetEnum.CLOSE);
					}
				}
			});

			sellButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						submitOrder(DirectionEnum.SHORT, OffsetEnum.CLOSE);
					}
				}
			});

			coverTDButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						submitOrder(DirectionEnum.LONG, OffsetEnum.CLOSE_TODAY);
					}
				}
			});

			sellTDButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						submitOrder(DirectionEnum.SHORT, OffsetEnum.CLOSE_TODAY);
					}
				}
			});
			coverYDButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						submitOrder(DirectionEnum.LONG, OffsetEnum.CLOSE_YESTERDAY);
					}
				}
			});

			sellYDButton.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					if (e.getButton() == MouseButton.PRIMARY) {
						submitOrder(DirectionEnum.SHORT, OffsetEnum.CLOSE_YESTERDAY);
					}
				}
			});

			HBox buttonLine1HBox = new HBox();
			buttonLine1HBox.getChildren().addAll(buyButton, coverButton, coverTDButton, coverYDButton);
			HBox buttonLine2HBox = new HBox();
			buttonLine2HBox.getChildren().addAll(shortButton, sellButton, sellTDButton, sellYDButton);

			buttonVBox.getChildren().addAll(buttonLine1HBox, buttonLine2HBox);

		}

	}

	private void submitOrder(DirectionEnum direction, OffsetEnum offset) {
		if (submitPending) {
			return;
		}

		submitPending = true;

		Set<String> finalAccountIdSet = guiMainService.getSelectedAccountIdSet();

		if (finalAccountIdSet.size() == 0) {
			Alert selectAccountAlert = new Alert(AlertType.ERROR);
			selectAccountAlert.setTitle("错误");
			selectAccountAlert.setHeaderText("提交定单错误");
			selectAccountAlert.setContentText("请至少选择一个账户!");

			selectAccountAlert.showAndWait();
			submitPending = false;
			return;
		}

		ContractField finalContract = null;
		if (tick != null) {
			finalContract = tick.getContract();
		}

		if (finalContract == null) {

			Alert selectContractAlert = new Alert(AlertType.ERROR);
			selectContractAlert.setTitle("错误");
			selectContractAlert.setHeaderText("提交定单错误");
			selectContractAlert.setContentText("请选择合约!");

			selectContractAlert.showAndWait();

			submitPending = false;
			return;
		}

		Double finalPrice = null;

		if (PRICE_FILL_METHOD_LAST.equals(priceFillMethod)) {
			if (tick != null) {
				finalPrice = tick.getLastPrice();
			}
		} else if (PRICE_FILL_METHOD_ACTIVE.equals(priceFillMethod)) {
			if (DirectionEnum.LONG.equals(direction)) {
				if (tick != null && tick.getAskPriceList().size() > 0) {
					finalPrice = tick.getAskPriceList().get(0);
				}
			} else if (DirectionEnum.SHORT.equals(direction)) {
				if (tick != null && tick.getBidPriceList().size() > 0) {
					finalPrice = tick.getBidPriceList().get(0);
				}
			}
		} else if (PRICE_FILL_METHOD_QUEUE.equals(priceFillMethod)) {
			if (DirectionEnum.LONG.equals(direction)) {
				if (tick != null && tick.getBidPriceList().size() > 0) {
					finalPrice = tick.getBidPriceList().get(0);
				}
			} else if (DirectionEnum.SHORT.equals(direction)) {
				if (tick != null && tick.getAskPriceList().size() > 0) {
					finalPrice = tick.getAskPriceList().get(0);
				}
			}
		} else if (PRICE_FILL_METHOD_UPPER_OR_LOWER_LIMIT.equals(priceFillMethod)) {
			if (DirectionEnum.LONG.equals(direction)) {
				if (tick != null) {
					finalPrice = tick.getUpperLimit();
				}
			} else if (DirectionEnum.SHORT.equals(direction)) {
				if (tick != null) {
					finalPrice = tick.getLowerLimit();
				}
			}
		} else if (PRICE_FILL_METHOD_MANUAL.equals(priceFillMethod)) {
			finalPrice = price;
		}

		if (priceType != PriceTypeEnum.MARKET) {
			if (finalPrice == null || finalPrice == Double.MAX_VALUE) {
				Alert priceAlert = new Alert(AlertType.ERROR);
				priceAlert.setTitle("错误");
				priceAlert.setHeaderText("定单错误");
				priceAlert.setContentText("无法获取到正确的价格!");

				priceAlert.showAndWait();

				submitPending = false;
				return;
			}
		} else {
			if (finalPrice == null) {
				finalPrice = 0.0;
			}
		}

		SubmitOrderReqField.Builder submitOrderReqFieldBuilder = SubmitOrderReqField.newBuilder();
		submitOrderReqFieldBuilder.setContract(finalContract);
		submitOrderReqFieldBuilder.setCurrency(finalContract.getCurrency());
		submitOrderReqFieldBuilder.setDirection(direction);
		submitOrderReqFieldBuilder.setOffset(offset);
		submitOrderReqFieldBuilder.setPriceType(priceType);
		submitOrderReqFieldBuilder.setTimeConditionType(TimeConditionTypeEnum.GTC);
		submitOrderReqFieldBuilder.setPrice(finalPrice);

		Alert confirmAlert = new Alert(AlertType.CONFIRMATION, "确认提交订单？", ButtonType.YES, ButtonType.NO);
		confirmAlert.showAndWait();

		if (confirmAlert.getResult() == ButtonType.YES) {
			for (String accountId : finalAccountIdSet) {
				AccountField account = desktopTradeCachesService.queryAccountByAccountId(accountId);
				if (account == null) {
					Alert accountAlert = new Alert(AlertType.ERROR);
					accountAlert.setTitle("错误");
					accountAlert.setHeaderText("定单错误");
					accountAlert.setContentText("无法找到指定账户!账户ID:" + accountId);
					accountAlert.show();
				}

				submitOrderReqFieldBuilder.setAccountCode(account.getCode());
				submitOrderReqFieldBuilder.setGatewayId(account.getGateway().getGatewayId());

				if (accountVolumeMap.containsKey(accountId) && accountVolumeMap.get(accountId) != 0) {
					submitOrderReqFieldBuilder.setVolume(accountVolumeMap.get(accountId));
					submitOrderReqFieldBuilder.setOriginOrderId(UUIDStringPoolUtils.getUUIDString());
					rpcClientApiService.asyncSubmitOrder(submitOrderReqFieldBuilder.build(), UUIDStringPoolUtils.getUUIDString());
				} else if (volume != 0) {
					submitOrderReqFieldBuilder.setVolume(volume);
					submitOrderReqFieldBuilder.setOriginOrderId(UUIDStringPoolUtils.getUUIDString());
					rpcClientApiService.asyncSubmitOrder(submitOrderReqFieldBuilder.build(), UUIDStringPoolUtils.getUUIDString());
				}

			}
		}

		submitPending = false;

	}
}
