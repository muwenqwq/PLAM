# AI Role Play Companion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add persistent, highly customizable AI companion role cards and enable role-play mode in learning chat.

**Architecture:** Store role cards in MySQL under the current user, bind an optional role to each conversation, pass the selected role from Java backend to Python AI service, and render role management plus chat enablement in Vue.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL 8 SQL migrations, FastAPI/Pydantic, Vue 3, Element Plus, Pinia/Vite.

---

### Task 1: Backend red tests

**Files:**
- Test: `backend/src/test/java/com/edustudio/module/companion/CompanionRoleControllerTest.java`
- Test: update `backend/src/test/java/com/edustudio/module/chat/ChatControllerTest.java`

- [ ] Write a controller test that creates a role with background, personality, speaking style, boundaries and custom prompt, lists it, sets it default, and applies it to a conversation.
- [ ] Run `mvn -q -Dtest=CompanionRoleControllerTest test` and verify it fails because the controller does not exist.

### Task 2: Backend implementation

**Files:**
- Modify: `backend/src/main/resources/sql/schema.sql`
- Modify: `backend/src/main/resources/sql/seed.sql`
- Create: `backend/src/main/java/com/edustudio/module/companion/**`
- Modify: `backend/src/main/java/com/edustudio/module/chat/**`
- Modify: `backend/src/main/java/com/edustudio/integration/ai/dto/AiChatRequest.java`

- [ ] Add `ai_companion_role` table and conversation role columns.
- [ ] Add entity, mapper, DTO, VO, service, controller.
- [ ] Add role ownership checks through `LoginUserHolder.requireCurrentUserId()`.
- [ ] Add default role seed rows for the demo student.
- [ ] Extend chat service to resolve conversation role and pass `role_play` to AI.
- [ ] Run backend tests and fix until green.

### Task 3: AI service red/green

**Files:**
- Test: `ai-service/tests/test_role_play_chat.py`
- Modify: `ai-service/app/schemas/chat.py`
- Modify: `ai-service/app/core/mock_provider.py`
- Modify: `ai-service/app/core/openai_compatible_provider.py`

- [ ] Write a pytest asserting role settings are accepted and mock reply reflects role name/style.
- [ ] Run pytest and verify red.
- [ ] Add `role_play_enabled` and `companion_role` fields to schema.
- [ ] Inject role prompt into OpenAI-compatible messages.
- [ ] Make mock provider visibly adapt reply.
- [ ] Run pytest and fix until green.

### Task 4: Frontend integration

**Files:**
- Create: `frontend/src/api/companionRole.ts`
- Create: `frontend/src/views/RoleCompanion.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layout/BasicLayout.vue`
- Modify: `frontend/src/views/ChatAssistant.vue`

- [ ] Add role API wrapper.
- [ ] Add role management page with templates and high freedom form.
- [ ] Add sidebar route.
- [ ] Add chat role selector and role-play switch.
- [ ] Run `npm run build` and fix until green.

### Task 5: End-to-end verification

**Files:**
- Existing smoke scripts if useful.

- [ ] Run backend targeted tests.
- [ ] Run AI service pytest.
- [ ] Run frontend build.
- [ ] Start services or use existing services and verify chat sends role metadata.
