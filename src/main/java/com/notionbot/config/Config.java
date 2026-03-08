package com.notionbot.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final Dotenv dotenv = Dotenv.load();

    public static String get(String key) {
        String value = dotenv.get(key);
        if (value == null || value.isEmpty()) {
            logger.warn("Environment variable {} is not set!", key);
        }
        return value;
    }

    public static String getRequired(String key) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Critical configuration missing: " + key);
        }
        return value;
    }
}
