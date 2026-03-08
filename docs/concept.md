# Conceptual Design: Discord-Notion GTD Assistant

This bot acts as the capture and processing bridge between the user's daily Discord communication and their productivity system in Notion, strictly following the **GTD (Getting Things Done)** methodology.

The focus is not just on "storing tasks," but on achieving the state of **"mind like water"**, reducing friction in the capture process and using Artificial Intelligence to process chaotic inputs into organized and physical actions.

---

## 1. Primary Goal

Minimize information "capture" friction and automate processing. Instead of opening Notion and navigating through databases, the user sends a message or uses a Discord context menu.

The bot, powered by **Gemini**, guides the user through the GTD decision tree, unfolding projects, suggesting contexts, and organizing everything in Notion.

---

## 2. Core Features and AI (Gemini)

### A. Dynamic Capture (Brain Dump)
- **Self-Capture**: Quickly add a thought via the `/add` command or DM.
- **Message-Capture**: Transform any Discord message (your own or someone else's) into a GTD item via Context Menu (`right-click` -> `Apps` -> `Capture GTD`).
- **Brain Dump Mode**: A conversational mode where the bot helps organize chaotic thoughts in batches.

### B. AI Processing and Refinement
Gemini acts not just as a spellchecker, but as a **GTD master**:

- **Strict Project vs. Action Separation**: If the input is *"Implement OAuth authentication"*, the AI recognizes it's a **Project**. The bot asks: *"What is the absolute first physical and visible action?"* (e.g., *"Read the Discord API documentation"*).
- **Context and Time Extraction**: The AI analyzes the task and applies automatic context tags (e.g., `@PC`, `@Errands`, `@Phone`) and time estimates (e.g., `@15min`, `@1h`).
- **Clear Title Generation**: Converts *"Call utility company"* to *"Call utility company to resolve March billing issue"*.

---

## 3. The GTD Workflow (Implementation)

The bot mirrors the GTD workflow diagram through **Button Interactions** on Discord:

### 📥 Inbox
Everything starts here. The bot records the raw input.

### 🤔 Is it Actionable?
The user (or the AI as a suggestion) decides.

#### If YES (Actionable):
- **Rule of < 2 Minutes?**: If yes, hit the `[Do It Now]` button. The bot records it as completed (quick win log).
- **Projects**: If it requires more than one action, it's created in the **Projects** DB. The bot asks for the next physical action and saves it in the **Next Actions** DB linked to the project.
- **Delegate**: Hit the `[Delegate]` button. Asks for the responsible person's name and creates an entry in the **Waiting For** DB.
- **Defer**: Hit the `[Defer]` button. Item goes to **Next Actions** with context tags created by Gemini.

#### If NO (Not Actionable):
- **Trash**: Discard.
- **Incubate (Tickler vs Someday)**:
    - **Someday/Maybe**: For ideas without a set date (e.g., *"Learn Rust"*).
    - **Tickler**: For deferred decisions with a date (e.g., *"Evaluate buying tickets for WebSummit"* -> The bot creates a reminder for a specific date, keeping the Someday list clean).
- **Reference**: Goes to the **Reference** DB. Gemini automatically categorizes it (e.g., *Code Snippet*, *Finances*, *Checklist*).

---

## 4. Requirements and Configuration

### A. Integrations and Tokens
- **Discord Bot Token**: `Message Content` Intents and permissions for `Slash Commands`/`Context Menus`.
- **Notion Integration Token**: Edit access to GTD databases.
- **Notion Database IDs**: `Inbox`, `Next_Actions`, `Projects`, `Someday_Maybe`, `Reference`, `Waiting_For`.
- **Gemini API Key**: For inference (Gemini 2.5 Flash is ideal for its speed in chat interactions).

### B. Technical Stack
- **Language**: Java 17+ (JDA).
- **HTTP Client/SDKs**: Notion SDK and Google AI SDK.
- **State Management**: In-memory for pending flows with caching (e.g., Caffeine).

---

## 5. Architecture and Clean Code (Event Handlers)

Given the complex flow of GTD decisions, we use the **Early Returns** philosophy to keep the code linear and avoid "If-Else Hell."

### Architectural Directive
Separation of AI logic and Notion API calls must be isolated in Service classes (`NotionService`, `GeminiService`).

### Conceptual Example (Java)

```java
public void onButtonInteraction(ButtonInteractionEvent event) {
    // 1. Early Return: Ignore if not the system owner
    if (!event.getUser().getId().equals(OWNER_ID)) return;

    String componentId = event.getComponentId();

    // 2. Early Return: Ignore buttons that do not belong to this domain
    if (!componentId.startsWith("gtd_")) return;

    // 3. Linear processing
    String action = componentId.split("_")[1];
    
    switch (action) {
        case "2min":
            handleTwoMinuteRule(event);
            break;
        case "delegate":
            promptDelegateModal(event);
            break;
        case "tickler":
            promptDateModal(event);
            break;
        // ...
    }
}
```