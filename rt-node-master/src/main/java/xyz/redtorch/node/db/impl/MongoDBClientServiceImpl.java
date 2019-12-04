package xyz.redtorch.node.db.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import xyz.redtorch.common.mongo.MongoDBClient;
import xyz.redtorch.node.db.MongoDBClientService;

@Component
public class MongoDBClientServiceImpl implements InitializingBean, MongoDBClientService {
	private static Logger logger = LoggerFactory.getLogger(MongoDBClientServiceImpl.class);

	private MongoDBClient managementDbClient;

	@Value("${rt.node.master.db.management.host}")
	private String managementDbHost;
	@Value("${rt.node.master.db.management.port}")
	private int managementDbPort;
	@Value("${rt.node.master.db.management.username}")
	private String managementDbUsername;
	@Value("${rt.node.master.db.management.password}")
	private String managementDbPassword;
	@Value("${rt.node.master.db.management.authdb}")
	private String managementDbAuthDB;
	@Value("${rt.node.master.db.management.dbname}")
	private String managementDbDBName;

	private MongoDBClient marketDataDbClient;

	@Value("${rt.node.master.db.market-data.host}")
	private String marketDataDbHost;
	@Value("${rt.node.master.db.market-data.port}")
	private int marketDataDbPort;
	@Value("${rt.node.master.db.market-data.username}")
	private String marketDataDbUsername;
	@Value("${rt.node.master.db.market-data.password}")
	private String marketDataDbPassword;
	@Value("${rt.node.master.db.market-data.authdb}")
	private String marketDataDbAuthDB;
	@Value("${rt.node.master.db.market-data.dbname}")
	private String marketDataDbDBName;

	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			managementDbClient = new MongoDBClient(managementDbHost, managementDbPort, managementDbUsername, managementDbPassword, managementDbAuthDB);
		} catch (Exception e) {
			logger.error("管理数据库连接失败,程序终止", e);
			System.exit(0);
		}

		try {
			marketDataDbClient = new MongoDBClient(marketDataDbHost, marketDataDbPort, marketDataDbUsername, marketDataDbPassword, marketDataDbAuthDB);
		} catch (Exception e) {
			logger.error("行情数据库连接失败,程序终止", e);
			System.exit(0);
		}
	}

	@Override
	public MongoDBClient getManagementDbClient() {
		return this.managementDbClient;
	}

	@Override
	public MongoDBClient getMarketDataDbClient() {
		return this.marketDataDbClient;
	}

	@Override
	public String getManagementDbName() {
		return this.managementDbDBName;
	}

	@Override
	public String getMarketDataDbName() {
		return this.marketDataDbDBName;
	}

}
