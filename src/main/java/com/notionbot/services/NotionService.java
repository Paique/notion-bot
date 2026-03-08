package com.notionbot.services;

import com.notionbot.config.Config;
import notion.api.v1.NotionClient;
import notion.api.v1.model.databases.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotionService {
    private static final Logger logger = LoggerFactory.getLogger(NotionService.class);
    private final NotionClient client;

    public NotionService() {
        String token = Config.getRequired("NOTION_TOKEN");
        this.client = new NotionClient(token);
    }

    public boolean testConnection(String databaseId) {
        try {
            logger.info("Testing Notion connection for Database: {}", databaseId);
            Database database = client.retrieveDatabase(databaseId);
            logger.info("Successfully connected to Notion database: {}", database.getTitle().get(0).getPlainText());
            return true;
        } catch (Exception e) {
            logger.error("Failed to connect to Notion: {}", e.getMessage());
            return false;
        }
    }

    public void close() {
        client.close();
    }
}
