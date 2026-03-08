# Tech Stack: Notion GTD Assistant

This document serves as the single source of truth for technological decisions and project environment requirements. It ensures that the technical context is preserved throughout development.

## 1. Core Application
- **Language**: Java 17+ (LTS).
- **Build System**: Gradle (Groovy DSL).
- **Discord Framework**: [JDA (Java Discord API)](https://github.com/discord-jda/JDA) v6.3.1.
- **Runtime**: JVM (OpenJDK).

## 2. IA and Data Integrations
- **AI Engine**: [Google Gemini 2.5 Flash](https://ai.google.dev/models/gemini).
  - Scope: Text refinement, context suggestion, project unfolding.
- **Storage**: [Notion API](https://developers.notion.com/).
  - SDK: `notion-sdk-jvm-core`.
  - Structure: Multi-database (Inbox, Actions, Projects, etc.).

## 3. State Management and Performance
- **Local Cache**: [Caffeine Cache](https://github.com/ben-manes/caffeine).
  - Reason: Management of interaction sessions (buttons/modals) without DB persistence for ephemeral data.
- **JSON Parsing**: [Jackson Databind](https://github.com/FasterXML/jackson-databind).
- **Logs**: [Logback](https://logback.qos.ch/) + SLF4J.

## 4. Environment Requirements (Tokens & IDs)
The application requires a `.env` file in the project root with the following keys:

### Discord
- `DISCORD_TOKEN`: Bot token.
- `OWNER_ID`: Unique ID of the allowed user.

### Notion
- `NOTION_TOKEN`: Internal Integration Token.
- `DATABASE_INBOX_ID`: Inbox DB ID.
- `DATABASE_ACTIONS_ID`: Next Actions DB ID.
- `DATABASE_PROJECTS_ID`: Projects DB ID.
- `DATABASE_SOMEDAY_ID`: Someday/Maybe DB ID.
- `DATABASE_REFERENCE_ID`: Reference DB ID.
- `DATABASE_WAITING_ID`: Waiting For (Delegation) DB ID.

### Gemini
- `GEMINI_API_KEY`: API Key obtained from Google AI Studio.

## 5. Software Architecture
- **SOLID**: Principles strictly applied in service classes.
- **Pattern: Early Returns**: Discord handlers must filter preconditions (e.g., user ID check) at the beginning of the method.
- **Isolation**: Handlers do not make direct API calls; they delegate to `GeminiService` or `NotionService`.
- **UI Premium**: Consistent use of `EmbedBuilder` for professional visuals.
