package com.notionbot;

import com.notionbot.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Initializing Notion GTD Assistant...");

        try {
            // Validation of critical configs
            Config.getRequired("DISCORD_TOKEN");
            Config.getRequired("OWNER_ID");

            logger.info("Configuration loaded successfully.");

            // To be continued in Phase 2: Discord Init
            logger.info("Ready for Phase 2: Discord Connectivity.");

        } catch (Exception e) {
            logger.error("Failed to start the bot: {}", e.getMessage());
            System.exit(1);
        }
    }
}
