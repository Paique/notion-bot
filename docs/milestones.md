# Marcos do Projeto: Notion GTD Assistant

Este documento define as fases de desenvolvimento e os marcos (milestones) que serão seguidos. Para cada marco concluído, será realizado um commit local para revisão.

---

## 🟢 Fase 1: Fundação e Estrutura ✅
- **Marco 1.1**: Inicialização do Projeto Gradle e estrutura de pastas. ✅
- **Marco 1.2**: Configuração de dependências (JDA, Notion, Gemini, Dotenv, Caffeine). ✅
- **Marco 1.3**: Implementação do sistema de configuração e logs. ✅
- **Commit**: `feat: init project structure and base configuration` ✅

---

## 🔵 Fase 2: Conectividade Base
- **Marco 2.1**: Estabelecimento de conexão com Discord (bot online).
- **Marco 2.2**: Implementação do `NotionService` (Teste de conectividade com Inbox).
- **Marco 2.3**: Implementação do `GeminiService` (Teste de prompt simples).
- **Commit**: `feat: setup core service connectivity (Discord, Notion, Gemini)`

---

## 🟡 Fase 3: Captura e Inteligência
- **Marco 3.1**: Implementação do comando `/add` e Context Menu "Capturar GTD".
- **Marco 3.2**: Integração do fluxo de Refinamento (Gemini polindo a entrada).
- **Marco 3.3**: Implementação do sistema de cache (Caffeine) para gerir sessões de captura.
- **Commit**: `feat: implement capture workflows and AI refinement`

---

## 🟠 Fase 4: Árvore de Decisão GTD (Processamento)
- **Marco 4.1**: Implementação dos Handlers de Botões (Início da Árvore de Decisão).
- **Marco 4.2**: Fluxo de "Acionável?" e "Regra dos 2 Minutos".
- **Marco 4.3**: Implementação de Modals para delegação e datas (Tickler).
- **Commit**: `feat: implement interactive GTD decision tree and modals`

---

## 🔴 Fase 5: Integração Total do Ecossistema Notion
- **Marco 5.1**: Mapeamento final e criação de itens em todas as 6 BDs do Notion.
- **Marco 5.2**: Lógica de projetos e conexão entre Projetos e Próximas Ações.
- **Marco 5.3**: Refinamento de metadados (Tags de contexto e tempo sugeridas pela IA).
- **Commit**: `feat: complete notion ecosystem integration and project logic`

---

## 🏁 Fase 6: Polimento e Entrega
- **Marco 6.1**: Tratamento de erros global e "Early Returns" em todos os handlers.
- **Marco 6.2**: Testes de ponta a ponta (E2E).
- **Marco 6.3**: Documentação final de instalação e uso.
- **Commit**: `docs: polish, error handling and documentation`
