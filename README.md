# Notion GTD Assistant
**Gemini prompts are hardcoded for now, but will be made configurable in the future.**
**This project is in early development and updates are not guaranteed.**

A powerful Discord bot designed to streamline your **Getting Things Done (GTD)** workflow by integrating Discord and Notion with AI-powered refinement.

## 🌟 Overview
The **Notion GTD Assistant** allows you to capture ideas, tasks, and reference materials directly from Discord. Using Google Gemini AI, it refines your raw captures into clear, actionable items and helps you process them through a complete GTD decision tree, ultimately syncing everything to your organized Notion databases.

## 📑 Documentation
For detailed information about the project's vision, progress, and technical structure, please refer to the following documents:

*   **[Concept & Design](docs/concept.md)**: Explore the core philosophy, workflow diagrams, and feature set.
*   **[Project Milestones](docs/milestones.md)**: Track the development phases and completed goals.
*   **[Technical Stack](docs/tech-stack.md)**: Detailed look at the languages, frameworks (JDA, Notion SDK, Gemini), and architecture used.

## 🚀 Key Features
*   **AI-Powered Refinement**: Automatically polishes raw captures into clear titles using Gemini 2.5 Flash.
*   **Full GTD Decision Tree**: Interactive Discord buttons and modals to guide you through "Actionable?", "2-Minute Rule", "Defer", and "Delegate" choices.
*   **Premium Discord UI**: Clean and informative embeds for all interaction steps.
*   **Direct Notion Integration**: Supports 6 distinct databases (Inbox, Next Actions, Projects, Someday, Reference, Waiting For).
*   **Context Menu Support**: Capture any Discord message directly as a GTD item via a right-click.

## 🛠️ Setup & Installation
The bot is built with Java 17 and Gradle. To run it locally:

1.  **Clone the repository**.
2.  **Configure Environment Variables**: Create a `.env` file in the root based on the requirements listed in the [Tech Stack](docs/tech-stack.md) document.
3.  **Build and Run**:
    ```bash
    ./gradlew run
    ```

## Architecture
Built with **SOLID** principles and a focus on **Early Returns** to ensure a robust and maintainable codebase. The system is decoupled into specialized services for Discord handling, AI inference, and Notion synchronization.