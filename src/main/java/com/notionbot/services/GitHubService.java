package com.notionbot.services;

import com.notionbot.config.Config;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);
    private final OkHttpClient client;
    private final String token;
    private static final Pattern REPO_PATTERN = Pattern.compile("github\\.com/([^/]+)/([^/\\s#?]+)");

    public GitHubService() {
        this.client = new OkHttpClient();
        this.token = Config.get("GITHUB_TOKEN");
    }

    public String extractRepoContext(String text) {
        Matcher matcher = REPO_PATTERN.matcher(text);
        if (matcher.find()) {
            String owner = matcher.group(1);
            String repo = matcher.group(2).replace(".git", "");
            return fetchReadme(owner, repo);
        }
        return null;
    }

    private String fetchReadme(String owner, String repo) {
        String url = String.format("https://api.github.com/repos/%s/%s/readme", owner, repo);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.raw+json")
                .header("User-Agent", "Notion-GTD-Bot");

        if (token != null && !token.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String content = response.body().string();

                // Tentando não ir a falência
                int maxChars = 4000;
                if (content.length() > maxChars) {
                    logger.info("README for {}/{} is too large ({} chars). Clipping to {} chars.",
                            owner, repo, content.length(), maxChars);
                    return "[TRUNCATED DUE TO SIZE]\n\n" + content.substring(0, maxChars);
                }
                return content;
            }
            logger.warn("Failed to fetch README for {}/{}: {} {}", owner, repo, response.code(), response.message());
        } catch (IOException e) {
            logger.error("Error fetching GitHub README: {}", e.getMessage());
        }
        return null;
    }
}
