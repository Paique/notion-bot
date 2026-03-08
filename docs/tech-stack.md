# Stack Tecnológico: Notion GTD Assistant

Este documento serve como a fonte única de verdade para as decisões tecnológicas e requisitos de ambiente do projeto. Ele garante que o contexto técnico seja preservado ao longo do desenvolvimento.

## 1. Núcleo da Aplicação
- **Linguagem**: Java 17+ (LTS).
- **Sistema de Build**: Gradle (Kotlin DSL ou Groovy).
- **Framework de Discord**: [JDA (Java Discord API)](https://github.com/discord-jda/JDA) v5.x (Beta 20+ para suporte a slash commands e context menus).
- **Runtime**: JVM (OpenJDK).

## 2. Integrações de IA e Dados
- **Motor de IA**: [Google Gemini 3 Flash](https://ai.google.dev/models/gemini).
  - Escopo: Refinamento de texto, sugestão de contextos, desdobramento de projetos.
- **Armazenamento**: [Notion API](https://developers.notion.com/).
  - SDK: `notion-sdk-jvm-core`.
  - Estrutura: Multi-database (Inbox, Actions, Projects, etc.).

## 3. Gestão de Estado e Performance
- **Cache Local**: [Caffeine Cache](https://github.com/ben-manes/caffeine).
  - Motivo: Gestão de sessões de interação (botões/modals) sem persistência em DB para dados efêmeros.
- **JSON Parsing**: [Jackson Databind](https://github.com/FasterXML/jackson-databind).
- **Logs**: [Logback](https://logback.qos.ch/) + SLF4J.

## 4. Requisitos de Ambiente (Tokens & IDs)
A aplicação requer um arquivo `.env` na raiz do projeto com as seguintes chaves:

### Discord
- `DISCORD_TOKEN`: Token do bot.
- `OWNER_ID`: ID do utilizador único permitido.

### Notion
- `NOTION_TOKEN`: Internal Integration Token.
- `DATABASE_INBOX_ID`: ID da BD de Entrada.
- `DATABASE_ACTIONS_ID`: ID da BD de Próximas Ações.
- `DATABASE_PROJECTS_ID`: ID da BD de Projetos.
- `DATABASE_SOMEDAY_ID`: ID da BD de Algum Dia/Talvez.
- `DATABASE_REFERENCE_ID`: ID da BD de Referências.
- `DATABASE_WAITING_ID`: ID da BD de Aguardando (Delegação).

### Gemini
- `GEMINI_API_KEY`: API Key obtida no Google AI Studio.

## 5. Arquitetura de Software
- **SOLID**: Princípios aplicados rigorosamente nas classes de serviço.
- **Pattern: Early Returns**: Handlers do Discord devem filtrar precondições (ex: check de user ID) no início do método.
- **Isolation**: Handlers não fazem chamadas diretas a APIs; delegam para `GeminiService` ou `NotionService`.
