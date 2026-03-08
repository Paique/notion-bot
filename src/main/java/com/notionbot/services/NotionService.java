package com.notionbot.services;

import com.notionbot.config.Config;
import notion.api.v1.NotionClient;
import notion.api.v1.model.databases.Database;
import notion.api.v1.model.common.PropertyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotionService {
    private static final Logger logger = LoggerFactory.getLogger(NotionService.class);
    private final NotionClient client;
    private final Map<String, String> titlePropertyCache = new ConcurrentHashMap<>();

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

    public void createProject(String title, String description) {
        String dbId = Config.getRequired("DATABASE_PROJECTS_ID");
        createPage(dbId, title, description);
    }

    public void addToSomeday(String title, String description) {
        String dbId = Config.getRequired("DATABASE_SOMEDAY_ID");
        createPage(dbId, title, description);
    }

    public void addToReference(String title, String description) {
        String dbId = Config.getRequired("DATABASE_REFERENCE_ID");
        createPage(dbId, title, description);
    }

    public void addToActions(String title, String description) {
        String dbId = Config.getRequired("DATABASE_ACTIONS_ID");
        createPage(dbId, title, description);
    }

    private String getTitlePropertyName(String databaseId) {
        return titlePropertyCache.computeIfAbsent(databaseId, id -> {
            try {
                Database db = client.retrieveDatabase(id);
                return db.getProperties().entrySet().stream()
                        .filter(entry -> entry.getValue().getType() == PropertyType.Title)
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse("Name"); // Fallback if not found
            } catch (Exception e) {
                logger.error("Failed to discover title property for database {}: {}", id, e.getMessage());
                return "Name";
            }
        });
    }

    public void createPage(String databaseId, String title, String description) {
        try {
            logger.info("Creating page in database {}: {}", databaseId, title);
            String titlePropertyName = getTitlePropertyName(databaseId);

            notion.api.v1.model.pages.PageProperty.RichText titleRichText = new notion.api.v1.model.pages.PageProperty.RichText();
            notion.api.v1.model.pages.PageProperty.RichText.Text titleText = new notion.api.v1.model.pages.PageProperty.RichText.Text();
            titleText.setContent(title);
            titleRichText.setText(titleText);

            notion.api.v1.model.pages.PageProperty property = new notion.api.v1.model.pages.PageProperty();
            property.setTitle(java.util.Collections.singletonList(titleRichText));

            java.util.List<notion.api.v1.model.blocks.Block> children = null;
            if (description != null && !description.isEmpty()) {
                notion.api.v1.model.pages.PageProperty.RichText descRichText = new notion.api.v1.model.pages.PageProperty.RichText();
                notion.api.v1.model.pages.PageProperty.RichText.Text descText = new notion.api.v1.model.pages.PageProperty.RichText.Text();
                descText.setContent(description);
                descRichText.setText(descText);

                notion.api.v1.model.blocks.ParagraphBlock.Element element = new notion.api.v1.model.blocks.ParagraphBlock.Element(
                        java.util.Collections.singletonList(descRichText));

                notion.api.v1.model.blocks.ParagraphBlock block = new notion.api.v1.model.blocks.ParagraphBlock(
                        element);
                children = java.util.Collections.singletonList((notion.api.v1.model.blocks.Block) block);
            }

            client.createPage(
                    notion.api.v1.model.pages.PageParent.database(databaseId),
                    java.util.Collections.singletonMap(titlePropertyName, property),
                    children,
                    null,
                    null);

            logger.info("Successfully created page in {} using property {}: {}", databaseId, titlePropertyName, title);
        } catch (Exception e) {
            logger.error("Failed to create page in Notion: {}", e.getMessage(), e);
        }
    }

    public void close() {
        client.close();
    }
}
