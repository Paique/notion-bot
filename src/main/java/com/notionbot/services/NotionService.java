package com.notionbot.services;

import com.notionbot.config.Config;
import notion.api.v1.NotionClient;
import notion.api.v1.model.blocks.Block;
import notion.api.v1.model.blocks.ParagraphBlock;
import notion.api.v1.model.databases.Database;
import notion.api.v1.model.common.PropertyType;
import notion.api.v1.model.pages.PageParent;
import notion.api.v1.model.pages.PageProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for interacting with the Notion API.
 * Handles page creation across different GTD databases.
 */
public class NotionService {
    private static final Logger logger = LoggerFactory.getLogger(NotionService.class);
    private static final String DUE_DATE_PROPERTY = "Due Date";
    
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

    public void createProject(String title, String description, String dueDate) {
        createPage(Config.getRequired("DATABASE_PROJECTS_ID"), title, description, dueDate);
    }

    public void addToSomeday(String title, String description, String dueDate) {
        createPage(Config.getRequired("DATABASE_SOMEDAY_ID"), title, description, dueDate);
    }

    public void addToReference(String title, String description, String dueDate) {
        createPage(Config.getRequired("DATABASE_REFERENCE_ID"), title, description, dueDate);
    }

    public void addToActions(String title, String description, String dueDate) {
        createPage(Config.getRequired("DATABASE_ACTIONS_ID"), title, description, dueDate);
    }

    private String getTitlePropertyName(String databaseId) {
        return titlePropertyCache.computeIfAbsent(databaseId, id -> {
            try {
                Database db = client.retrieveDatabase(id);
                return db.getProperties().entrySet().stream()
                        .filter(entry -> entry.getValue().getType() == PropertyType.Title)
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse("Name");
            } catch (Exception e) {
                logger.error("Failed to discover title property for database {}: {}", id, e.getMessage());
                return "Name";
            }
        });
    }

    public void createPage(String databaseId, String title, String description, String dueDate) {
        try {
            logger.info("Creating page in database {}: {} (dueDate: {})", databaseId, title, dueDate);
            String titlePropertyName = getTitlePropertyName(databaseId);

            PageProperty.RichText titleRichText = new PageProperty.RichText();
            PageProperty.RichText.Text titleText = new PageProperty.RichText.Text();
            titleText.setContent(title);
            titleRichText.setText(titleText);

            PageProperty property = new PageProperty();
            property.setTitle(Collections.singletonList(titleRichText));

            java.util.Map<String, PageProperty> properties = new java.util.HashMap<>();
            properties.put(titlePropertyName, property);

            if (dueDate != null && !dueDate.isEmpty()) {
                PageProperty dateProperty = new PageProperty();
                PageProperty.Date dateObj = new PageProperty.Date(dueDate, null, null);
                dateProperty.setDate(dateObj);
                properties.put(DUE_DATE_PROPERTY, dateProperty);
            }

            List<Block> children = null;
            if (description != null && !description.isEmpty()) {
                PageProperty.RichText descRichText = new PageProperty.RichText();
                PageProperty.RichText.Text descText = new PageProperty.RichText.Text();
                descText.setContent(description);
                descRichText.setText(descText);

                ParagraphBlock.Element element = new ParagraphBlock.Element(
                        Collections.singletonList(descRichText));

                ParagraphBlock block = new ParagraphBlock(element);
                children = Collections.singletonList(block);
            }

            client.createPage(
                    PageParent.database(databaseId),
                    properties,
                    children,
                    null,
                    null);

            logger.info("Successfully created page in {}: {}", databaseId, title);
        } catch (Exception e) {
            logger.error("Failed to create page in Notion: {}", e.getMessage(), e);
        }
    }

    public void close() {
        client.close();
    }
}
