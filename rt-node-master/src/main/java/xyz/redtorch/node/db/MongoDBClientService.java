package xyz.redtorch.node.db;

import xyz.redtorch.common.mongo.MongoDBClient;

public interface MongoDBClientService {
	MongoDBClient getManagementDbClient();

	MongoDBClient getMarketDataDbClient();

	String getManagementDbName();

	String getMarketDataDbName();
}