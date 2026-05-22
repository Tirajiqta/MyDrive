# Software Design Document
## MyDrive — Cloud Document Storage and Archive System

**Version:** 1.0  
**Date:** 2026-04-02  
**Status:** Draft  
**Reference SRS:** SRS.md v1.0  

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [System Architecture](#2-system-architecture)
3. [Component Design](#3-component-design)
4. [Database Design](#4-database-design)
5. [API Design](#5-api-design)
6. [Real-Time Collaboration Design](#6-real-time-collaboration-design)
7. [Archive Module Design](#7-archive-module-design)
8. [Security Design](#8-security-design)
9. [Deployment and Infrastructure](#9-deployment-and-infrastructure)
10. [Appendix](#10-appendix)

---

## 1. Introduction

### 1.1 Purpose

This Software Design Document (SDD) describes the architectural and detailed design of **MyDrive**. It translates the requirements stated in the SRS into concrete design decisions, data models, component responsibilities, and interface contracts. It is intended for developers, architects, and reviewers.

### 1.2 Scope

The document covers all four system components:

- Java/Spring Boot API — `MYDrive`
- Go File Server
- Next.js Web Frontend
- Archive Module (implemented within the Java API)

### 1.3 Design Goals

| Goal | Rationale |
|------|-----------|
| Stateless API | Enables horizontal scaling without sticky sessions |
| Separation of metadata and binary storage | Allows independent scaling of file I/O and business logic |
| OAIS-compliant archive module | Satisfies document archive compliance requirements |
| Real-time collaboration via WebSocket + CRDT | Enables conflict-free multi-user editing |
| Security-by-default | Tokens never stored in plaintext; TLS everywhere |

### 1.4 Definitions

Refer to SRS §1.3 for the full glossary. Additional terms used in this document:

| Term | Definition |
|------|-----------|
| **DTO** | Data Transfer Object — a plain object used to carry data between layers |
| **JPA** | Jakarta Persistence API — ORM specification implemented by Hibernate |
| **CRDT** | Conflict-free Replicated Data Type |
| **Y.js** | A CRDT framework for shared data types in JavaScript |
| **AIP** | Archival Information Package (OAIS) |
| **SIP** | Submission Information Package (OAIS) |
| **DIP** | Dissemination Information Package (OAIS) |
| **bcrypt** | Adaptive password hashing function |
| **S3** | Amazon Simple Storage Service; used here to denote any S3-compatible API |

---

## 2. System Architecture

### 2.1 High-Level Architecture

MyDrive follows a **service-oriented architecture** with four runtime processes and two storage backends:

```
                        ┌──────────────────────────────────────┐
                        │             Internet                 │
                        └──────────┬───────────────────────────┘
                                   │ HTTPS / WSS
                        ┌──────────▼───────────────────────────┐
                        │         Reverse Proxy / TLS          │
                        │         (Nginx / Caddy)              │
                        └──┬───────────────┬────────────────────┘
                           │               │
             ┌─────────────▼───┐     ┌─────▼──────────────┐
             │  Java API        │     │  Go File Server     │
             │  :8080           │     │  :9090              │
             │                 │     │                     │
             │  Auth           │     │  /upload            │
             │  File Metadata  │◄────►  /download/:id      │
             │  Folders        │     │  /delete/:id        │
             │  Sharing        │     │                     │
             │  Archive        │     └─────────┬───────────┘
             │  Plans          │               │ S3 API
             │  AI Chat        │     ┌─────────▼───────────┐
             │  Support        │     │  Object Storage      │
             │  WebSocket Hub  │     │  (MinIO / S3)        │
             └────────┬────────┘     └─────────────────────┘
                      │ JDBC
             ┌────────▼────────┐
             │  MySQL 8         │
             │  mydrivedb       │
             └─────────────────┘
```

### 2.2 Component Responsibilities

| Component | Responsibilities |
|-----------|----------------|
| **Java API** | Authentication, JWT issuance, user/file/folder metadata, sharing, archive logic, subscriptions, AI proxy, support tickets, WebSocket hub for collaboration |
| **Go File Server** | Receives multipart uploads, streams downloads, manages file lifecycle on object storage, computes SHA-256 checksum on ingest |
| **Object Storage** | Persistent binary storage; files addressed by `uniqueName` UUID |
| **MySQL** | All relational metadata: users, files, folders, shares, subscriptions, archive records, audit log |
| **Web Frontend** | Renders UI, manages client-side auth state (JWT in memory, refresh token in HttpOnly cookie), hosts collaborative editor |

### 2.3 Technology Stack Summary

| Layer | Technology | Version |
|-------|-----------|---------|
| Java API | Spring Boot | 3.5.x |
| Java API | Java | 21 LTS |
| Java API | Hibernate / Spring Data JPA | 6.x |
| Java API | Spring Security + JJWT | 6.x / 0.13.x |
| Java API | Spring WebFlux (WebClient) | 6.x |
| Java API | Spring WebSocket | 6.x |
| File Server | Go | 1.21+ |
| File Server | AWS SDK v2 for Go | 1.x |
| Database | MySQL | 8.0+ |
| Object Storage | MinIO (S3-compatible) | Latest |
| Web Frontend | Next.js (App Router) | 15.x |
| Web Frontend | React | 19.x |
| Web Frontend | TypeScript | 5.x |
| Web Frontend | Tailwind CSS | 3.4.x |
| Web Frontend | Y.js (CRDT) | 13.x |

### 2.4 Data Flow: File Upload

```
Client
  │
  ├─1─► POST /api/files/upload  ──────────────────────────────► Java API
  │         (multipart/form-data: file + folderId)                │
  │                                                               │ 2. Validate quota
  │                                                               │ 3. Generate uniqueName (UUID)
  │                                                               │ 4. POST /internal/upload ──► Go File Server
  │                                                               │                                │
  │                                                               │                                │ 5. Stream to S3
  │                                                               │                                │ 6. Compute SHA-256
  │                                                               │◄────── 7. {checksum, size} ─────┘
  │                                                               │ 8. Persist FileEntity (MySQL)
  │◄──────────────────────────── 9. FileResponse ─────────────────┘
```

### 2.5 Data Flow: Real-Time Collaboration

```
Editor A                    Java API (WebSocket Hub)              Editor B
   │                                │                                │
   ├──── WS connect /ws/doc/{id} ───►                                │
   │                                │◄──── WS connect /ws/doc/{id} ──┤
   │                                │                                │
   ├── Y.js update (binary delta) ──►                                │
   │                                ├──── broadcast delta ───────────►
   │                                │                                │
   │                                ├── persist to Y.js doc store ───┤
   │◄──── broadcast delta (echo) ───┤                                │
```

---

## 3. Component Design

### 3.1 Java API — Package Structure

```
com.mydrive
├── config/
│   ├── SecurityConfig.java          # Spring Security filter chain, CORS, public routes
│   ├── JwtConfig.java               # RSA key pair loading, JwtEncoder/Decoder beans
│   ├── WebSocketConfig.java         # STOMP WebSocket broker configuration
│   └── OpenAIConfig.java            # WebClient bean for OpenAI
│
├── domain/                          # JPA entities (source of truth for DB schema)
│   ├── UserEntity.java
│   ├── FileEntity.java
│   ├── FolderEntity.java
│   ├── ShareEntity.java
│   ├── RefreshTokenEntity.java
│   ├── PasswordResetTokenEntity.java
│   ├── PlanEntity.java
│   ├── PlanTranslationEntity.java
│   ├── UserSubscriptionEntity.java
│   ├── LanguageEntity.java
│   ├── FaqEntity.java
│   ├── FaqTranslationEntity.java
│   ├── SupportTicketEntity.java
│   ├── ChatbotInteractionEntity.java
│   ├── ArchiveRecordEntity.java     # Archive module
│   ├── AuditLogEntity.java          # Archive audit trail
│   └── FileVersionEntity.java       # Document versioning
│
├── repository/                      # Spring Data JPA repositories
│
├── service/                         # Business logic
│   ├── AuthService.java
│   ├── FileService.java
│   ├── FolderService.java
│   ├── ShareService.java
│   ├── SubscriptionService.java
│   ├── ArchiveService.java
│   ├── AuditService.java
│   ├── CollaborationService.java
│   ├── ChatbotService.java
│   └── SupportTicketService.java
│
├── controller/                      # REST controllers
│   ├── AuthController.java          # /api/auth/**
│   ├── FileController.java          # /api/files/**
│   ├── FolderController.java        # /api/folders/**
│   ├── ShareController.java         # /api/shares/**, /public/shares/**
│   ├── SubscriptionController.java  # /api/plans/**, /api/subscriptions/**
│   ├── ArchiveController.java       # /api/archive/**
│   ├── ChatbotController.java       # /api/chat
│   └── SupportTicketController.java # /api/support/**
│
├── websocket/
│   ├── CollaborationController.java # STOMP message handler for /app/doc/**
│   └── CollaborationSessionRegistry.java
│
├── scheduler/
│   └── JobScheduler.java            # Token cleanup, integrity sweep, retention checks
│
├── dto/                             # Request/response DTOs
├── mapper/                          # Entity ↔ DTO mappers
├── exception/                       # Custom exceptions + GlobalExceptionHandler
└── util/                            # TokenHashUtil, ChecksumUtil, etc.
```

### 3.2 Java API — Layer Interactions

```
HTTP Request
     │
     ▼
Controller (validate DTO, call service)
     │
     ▼
Service (business logic, transaction boundary)
     │
     ├──► Repository (JPA → MySQL)
     │
     └──► External (Go File Server via RestTemplate / WebClient,
                     OpenAI via WebClient,
                     SMTP via JavaMailSender)
```

- Controllers are `@RestController` beans; they validate input via `@Valid` and translate exceptions via `@RestControllerAdvice`.
- Services are `@Service` beans annotated with `@Transactional` where multiple repository operations must be atomic.
- Repositories extend `JpaRepository<Entity, Long>` and declare custom JPQL queries with `@Query`.

### 3.3 Go File Server — Design

The file server exposes three HTTP endpoints to the Java API over an internal network. It is intentionally minimal:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/internal/upload` | POST | Accepts `multipart/form-data` with field `file` and header `X-Unique-Name`. Streams bytes to S3, computes SHA-256 on the fly, returns `{"checksum":"...","size":...}`. |
| `/internal/download/{uniqueName}` | GET | Streams the object from S3 to the response body. Sets `Content-Type` from S3 object metadata. |
| `/internal/delete/{uniqueName}` | DELETE | Removes the object from S3. Returns 204. |

**Internal authentication:** Requests from the Java API include a shared secret header (`X-Internal-Token`). The file server validates this header on every request.

**S3 interaction:** Uses the AWS SDK v2 for Go. The bucket name and credentials are injected via environment variables.

```
upload flow:
  multipart reader → io.TeeReader → S3 PutObject
                                  └→ sha256.New() → hex digest
```

### 3.4 Web Frontend — Architecture

The frontend uses Next.js App Router with the following route structure:

```
app/
├── (auth)/
│   ├── login/page.tsx
│   ├── register/page.tsx
│   └── forgot-password/page.tsx
├── (drive)/
│   ├── layout.tsx                  # Auth guard, sidebar, header
│   ├── page.tsx                    # Root folder view
│   ├── folder/[id]/page.tsx        # Folder contents
│   ├── file/[id]/page.tsx          # File preview
│   └── editor/[id]/page.tsx        # Collaborative editor
├── (archive)/
│   ├── layout.tsx                  # Archive operator guard
│   └── archive/page.tsx            # Archive management
├── share/[token]/page.tsx          # Public share resolver (no auth)
└── layout.tsx                      # Root layout, font, global styles
```

**State management:**
- Server Components are used for data fetching where possible (Next.js RSC).
- Client Components manage interactive state (editor, upload progress, presence indicators).
- Auth state (access token) is stored in memory (React context); the refresh token is stored in an HttpOnly, Secure, SameSite=Strict cookie.
- File/folder listings use SWR for cache and revalidation.

**Collaborative editor:**
- Built on TipTap (ProseMirror-based) with the `@tiptap/extension-collaboration` plugin.
- Y.js document state is synchronized via `y-websocket` provider connected to the Java API's WebSocket endpoint.
- Presence (cursor positions, user names) is implemented with Y.js Awareness.

---

## 4. Database Design

### 4.1 Entity Relationship Overview

```
UserEntity ─────────────────────────────────────────────────────┐
  │ 1                                                            │
  ├── N ── UserSubscriptionEntity ── 1 ── PlanEntity            │
  │          (active subscription)        (1..N translations)   │
  │                                                              │
  ├── N ── FolderEntity (root folders, self-referential tree)    │
  │                                                              │
  ├── N ── FileEntity                                            │
  │          │ 1..N ── FileVersionEntity                         │
  │          │ 0..1 ── ArchiveRecordEntity ─── N ── AuditLogEntity
  │          │ 0..N ── ShareEntity                               │
  │                                                              │
  ├── N ── RefreshTokenEntity                                    │
  ├── N ── PasswordResetTokenEntity                              │
  ├── N ── ChatbotInteractionEntity ── 0..1 ── SupportTicketEntity
  └── 1 ── LanguageEntity (preferred language)                  │
                                                                 │
FaqEntity ─── N ── FaqTranslationEntity ── 1 ── LanguageEntity  │
PlanEntity ── N ── PlanTranslationEntity ── 1 ── LanguageEntity │
```

### 4.2 Table Definitions

#### `users`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL | |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | |
| `password_hash` | VARCHAR(255) | NOT NULL | bcrypt |
| `current_storage_used` | BIGINT | NOT NULL, DEFAULT 0 | bytes |
| `preferred_language_id` | BIGINT | FK → languages.id | |
| `mfa_enabled` | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| `mfa_secret` | VARCHAR(64) | NULLABLE | TOTP secret, encrypted at rest |
| `created_at` | DATETIME | NOT NULL | |
| `updated_at` | DATETIME | NOT NULL | |

#### `folders`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `name` | VARCHAR(255) | NOT NULL | |
| `owner_id` | BIGINT | FK → users.id, NOT NULL | |
| `parent_id` | BIGINT | FK → folders.id, NULLABLE | NULL = root |
| `is_deleted` | BOOLEAN | NOT NULL, DEFAULT FALSE | soft delete |
| `deleted_at` | DATETIME | NULLABLE | |
| `created_at` | DATETIME | NOT NULL | |
| `updated_at` | DATETIME | NOT NULL | |

*Index:* `(owner_id, parent_id, is_deleted)` for folder listing queries.

#### `files`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `original_name` | VARCHAR(255) | NOT NULL | display name |
| `unique_name` | VARCHAR(36) | UNIQUE, NOT NULL | UUID; S3 object key |
| `mime_type` | VARCHAR(127) | NOT NULL | |
| `size_bytes` | BIGINT | NOT NULL | |
| `checksum_sha256` | CHAR(64) | NOT NULL | hex |
| `owner_id` | BIGINT | FK → users.id, NOT NULL | |
| `folder_id` | BIGINT | FK → folders.id, NULLABLE | NULL = root |
| `is_deleted` | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| `deleted_at` | DATETIME | NULLABLE | |
| `uploaded_at` | DATETIME | NOT NULL | |
| `updated_at` | DATETIME | NOT NULL | |

#### `file_versions`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `file_id` | BIGINT | FK → files.id, NOT NULL | |
| `version_number` | INT | NOT NULL | monotonically increasing |
| `unique_name` | VARCHAR(36) | UNIQUE, NOT NULL | separate S3 object per version |
| `size_bytes` | BIGINT | NOT NULL | |
| `checksum_sha256` | CHAR(64) | NOT NULL | |
| `saved_by_user_id` | BIGINT | FK → users.id | |
| `saved_at` | DATETIME | NOT NULL | |

#### `shares`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `token_hash` | CHAR(64) | UNIQUE, NOT NULL | SHA-256 of raw token |
| `resource_type` | ENUM('FILE','FOLDER') | NOT NULL | |
| `resource_id` | BIGINT | NOT NULL | FK to files or folders (polymorphic) |
| `owner_id` | BIGINT | FK → users.id, NOT NULL | |
| `permission` | ENUM('VIEW','EDIT','DELETE') | NOT NULL | |
| `expires_at` | DATETIME | NULLABLE | |
| `revoked_at` | DATETIME | NULLABLE | |
| `created_at` | DATETIME | NOT NULL | |

#### `refresh_tokens`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `token_hash` | CHAR(64) | UNIQUE, NOT NULL | SHA-256 of raw token |
| `user_id` | BIGINT | FK → users.id, NOT NULL | |
| `expires_at` | DATETIME | NOT NULL | +30 days from creation |
| `created_at` | DATETIME | NOT NULL | |

#### `password_reset_tokens`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `token_hash` | CHAR(64) | UNIQUE, NOT NULL | |
| `user_id` | BIGINT | FK → users.id, NOT NULL | |
| `expires_at` | DATETIME | NOT NULL | +30 min |
| `used_at` | DATETIME | NULLABLE | set on use |
| `created_at` | DATETIME | NOT NULL | |

#### `plans`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `plan_type` | ENUM('FREE','PAID') | NOT NULL | |
| `storage_limit_gb` | INT | NOT NULL | |
| `price_per_month` | DECIMAL(10,2) | NOT NULL | |
| `currency` | CHAR(3) | NOT NULL | ISO 4217 |

#### `user_subscriptions`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `user_id` | BIGINT | FK → users.id, NOT NULL | |
| `plan_id` | BIGINT | FK → plans.id, NOT NULL | |
| `status` | ENUM('ACTIVE','EXPIRED','CANCELLED') | NOT NULL | |
| `started_at` | DATETIME | NOT NULL | |
| `ends_at` | DATETIME | NULLABLE | |
| `renewal_date` | DATETIME | NULLABLE | |
| `payment_method_id` | VARCHAR(100) | NULLABLE | external payment ref |

#### `archive_records`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `archive_uuid` | CHAR(36) | UNIQUE, NOT NULL | AIP identifier |
| `file_id` | BIGINT | FK → files.id, NOT NULL | |
| `title` | VARCHAR(500) | NOT NULL | |
| `author` | VARCHAR(255) | NOT NULL | |
| `document_type` | VARCHAR(100) | NOT NULL | |
| `language_code` | CHAR(5) | NOT NULL | BCP 47 |
| `checksum_sha256` | CHAR(64) | NOT NULL | recomputed on ingest |
| `format_validation` | VARCHAR(50) | NULLABLE | e.g. PDF/A-1b, NON_CONFORMANT |
| `signature_valid` | BOOLEAN | NULLABLE | result of eIDAS sig validation |
| `signature_issuer` | VARCHAR(500) | NULLABLE | |
| `timestamp_token` | BLOB | NULLABLE | RFC 3161 TST |
| `retention_years` | INT | NOT NULL | |
| `retain_until` | DATE | NOT NULL | archival_date + retention_years |
| `legal_hold` | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| `legal_hold_reason` | TEXT | NULLABLE | |
| `archived_at` | DATETIME | NOT NULL | |
| `archived_by_user_id` | BIGINT | FK → users.id | |
| `last_integrity_check` | DATETIME | NULLABLE | |
| `integrity_status` | ENUM('OK','MISMATCH','PENDING') | NOT NULL, DEFAULT 'PENDING' | |

#### `audit_log`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `event_type` | VARCHAR(50) | NOT NULL | see §7.3 |
| `archive_record_id` | BIGINT | FK → archive_records.id, NOT NULL | |
| `actor_user_id` | BIGINT | NULLABLE | NULL = system |
| `actor_ip` | VARCHAR(45) | NULLABLE | IPv4 or IPv6 |
| `occurred_at` | DATETIME(3) | NOT NULL | UTC, ms precision |
| `details` | JSON | NULLABLE | event-specific payload |
| `entry_hash` | CHAR(64) | NOT NULL | SHA-256(prev_hash + event fields) |
| `previous_hash` | CHAR(64) | NOT NULL | 0x00..00 for first entry |

*The `audit_log` table has no UPDATE or DELETE privileges granted to the application database user.*

#### `support_tickets`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `subject` | VARCHAR(255) | NOT NULL | |
| `description` | TEXT | NOT NULL | |
| `status` | ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED') | NOT NULL | |
| `priority` | ENUM('LOW','MEDIUM','HIGH','CRITICAL') | NOT NULL | |
| `submitted_by_user_id` | BIGINT | FK → users.id | |
| `assigned_agent_id` | BIGINT | FK → users.id, NULLABLE | |
| `github_issue_url` | VARCHAR(500) | NULLABLE | |
| `github_issue_id` | BIGINT | NULLABLE | |
| `created_at` | DATETIME | NOT NULL | |
| `updated_at` | DATETIME | NOT NULL | |

#### `chatbot_interactions`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `user_id` | BIGINT | FK → users.id, NOT NULL | |
| `user_message` | TEXT | NOT NULL | |
| `assistant_response` | TEXT | NOT NULL | |
| `ticket_id` | BIGINT | FK → support_tickets.id, NULLABLE | |
| `created_at` | DATETIME | NOT NULL | |

---

## 5. API Design

### 5.1 Conventions

- Base URL: `https://api.mydrive.app/api/v1`
- All requests and responses use `Content-Type: application/json` unless noted.
- Authentication: `Authorization: Bearer <access_token>` on all protected endpoints.
- Error response body:
  ```json
  {
    "error": "ERROR_CODE",
    "message": "Human-readable description",
    "timestamp": "2026-04-02T10:00:00Z"
  }
  ```
- Paginated list responses:
  ```json
  {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
  ```

### 5.2 Authentication Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | None | Register new user |
| POST | `/auth/login` | None | Login, returns tokens |
| POST | `/auth/refresh` | None | Rotate refresh token |
| POST | `/auth/logout` | Bearer | Revoke refresh token |
| POST | `/auth/forgot-password` | None | Send reset email |
| POST | `/auth/reset-password` | None | Consume reset token |
| POST | `/auth/change-password` | Bearer | Change password |
| GET | `/auth/me` | Bearer | Get current user profile |

**POST /auth/register** — Request:
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "S3cur3P@ss!"
}
```
Response `201 Created`:
```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "storageUsedBytes": 0,
  "plan": "FREE"
}
```

**POST /auth/login** — Request:
```json
{ "usernameOrEmail": "alice", "password": "S3cur3P@ss!" }
```
Response `200 OK`:
```json
{
  "accessToken": "<JWT>",
  "refreshToken": "<opaque>",
  "expiresIn": 3600
}
```

### 5.3 File Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/files/upload` | Bearer | Upload a file (multipart) |
| GET | `/files/{id}/download` | Bearer | Download file |
| GET | `/files?folderId={id}` | Bearer | List files in folder |
| PATCH | `/files/{id}` | Bearer | Rename file |
| PUT | `/files/{id}/move` | Bearer | Move file to folder |
| DELETE | `/files/{id}` | Bearer | Soft delete file |
| POST | `/files/{id}/restore` | Bearer | Restore from trash |
| GET | `/files/{id}/versions` | Bearer | List versions |
| POST | `/files/{id}/versions/{v}/restore` | Bearer | Restore a version |

**POST /files/upload** — `Content-Type: multipart/form-data`

Form fields:
- `file` — binary content
- `folderId` — (optional) target folder ID

Response `201 Created`:
```json
{
  "id": 42,
  "name": "report.pdf",
  "mimeType": "application/pdf",
  "sizeBytes": 204800,
  "checksumSha256": "e3b0c44...",
  "folderId": 7,
  "uploadedAt": "2026-04-02T10:00:00Z"
}
```

### 5.4 Folder Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/folders` | Bearer | Create folder |
| GET | `/folders?parentId={id}` | Bearer | List folders |
| PATCH | `/folders/{id}` | Bearer | Rename folder |
| PUT | `/folders/{id}/move` | Bearer | Move folder |
| DELETE | `/folders/{id}` | Bearer | Soft delete folder |
| POST | `/folders/{id}/restore` | Bearer | Restore folder |

### 5.5 Share Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/shares` | Bearer | Create share link |
| GET | `/shares` | Bearer | List own shares |
| DELETE | `/shares/{id}` | Bearer | Revoke share |
| GET | `/public/shares/{token}` | None | Resolve share token |

**POST /shares** — Request:
```json
{
  "resourceType": "FILE",
  "resourceId": 42,
  "permission": "VIEW",
  "expiresAt": "2026-06-01T00:00:00Z"
}
```
Response `201 Created`:
```json
{
  "shareId": 10,
  "token": "<raw token — returned once, never stored>",
  "shareUrl": "https://mydrive.app/share/<raw token>",
  "permission": "VIEW",
  "expiresAt": "2026-06-01T00:00:00Z"
}
```

### 5.6 Archive Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/archive` | Bearer (Operator) | Submit document for archiving (SIP) |
| GET | `/archive` | Bearer (Operator) | List archived documents |
| GET | `/archive/{archiveUuid}` | Bearer (Operator) | Get AIP metadata |
| GET | `/archive/{archiveUuid}/export` | Bearer (Operator) | Download DIP (zip) |
| POST | `/archive/{archiveUuid}/hold` | Bearer (Operator) | Place legal hold |
| DELETE | `/archive/{archiveUuid}/hold` | Bearer (Operator) | Release legal hold |
| GET | `/archive/{archiveUuid}/audit` | Bearer (Operator) | Get audit trail |
| POST | `/archive/{archiveUuid}/integrity` | Bearer (Operator) | Trigger integrity check |
| DELETE | `/archive/{archiveUuid}` | Bearer (Operator) | Destroy record (if retention expired and no hold) |

**POST /archive** — Request:
```json
{
  "fileId": 42,
  "title": "Annual Report 2025",
  "author": "Finance Dept.",
  "documentType": "Financial",
  "languageCode": "en",
  "retentionYears": 10,
  "signatureFormat": "PAdES"
}
```
Response `201 Created`:
```json
{
  "archiveUuid": "550e8400-e29b-41d4-a716-446655440000",
  "checksumSha256": "abc123...",
  "formatValidation": "PDF/A-1b",
  "signatureValid": true,
  "retainUntil": "2036-04-02",
  "archivedAt": "2026-04-02T10:00:00Z"
}
```

---

## 6. Real-Time Collaboration Design

### 6.1 Protocol Selection

Real-time collaboration uses **WebSocket** with **STOMP** framing (supported natively by Spring WebSocket) and **Y.js** (CRDT) on the client side.

Y.js was chosen over OT because:
- No central server needs to track operation history for convergence
- Works naturally with disconnected/reconnect scenarios
- `y-websocket` provider handles all synchronization boilerplate

### 6.2 WebSocket Endpoint

```
WSS /ws/collab
```

STOMP destinations:

| Destination | Direction | Description |
|-------------|-----------|-------------|
| `/app/doc/{id}/update` | Client → Server | Send Y.js binary update |
| `/topic/doc/{id}` | Server → Clients | Broadcast Y.js binary update |
| `/app/doc/{id}/awareness` | Client → Server | Send Y.js awareness state (cursor, user) |
| `/topic/doc/{id}/awareness` | Server → Clients | Broadcast awareness state |

### 6.3 Server-Side Document Store

The Java API maintains an in-memory Y.js document state using the **y-crdt** Java binding (or delegates to a sidecar Node.js process if JVM binding is unavailable). The document state is also persisted to the database at autosave intervals.

```
┌─────────────────────────────────────────────┐
│  CollaborationService                        │
│                                              │
│  Map<Long, YDoc> activeDocs                  │  ← in-memory per document
│                                              │
│  onUpdate(docId, binaryUpdate):              │
│    1. activeDocs[docId].applyUpdate(update)  │
│    2. broadcast to /topic/doc/{docId}        │
│    3. scheduleAutosave(docId)                │
│                                              │
│  autosave(docId):                            │
│    1. encode full state vector               │
│    2. persist to file_versions table         │
│    3. update files.updated_at                │
└─────────────────────────────────────────────┘
```

### 6.4 Connection Lifecycle

```
Client connects:
  1. Authenticate via JWT query param: /ws/collab?token=<JWT>
  2. Subscribe to /topic/doc/{id} and /topic/doc/{id}/awareness
  3. Server sends current full document state (Y.js state vector)

Client disconnects:
  1. CollaborationSessionRegistry removes session
  2. If no more sessions for docId → trigger final autosave
  3. Broadcast updated awareness (user left)
```

### 6.5 Access Control

Before accepting a WebSocket message for document `id`, the server checks that the authenticated user:
1. Owns the file, **or**
2. Holds a valid, non-expired share token with EDIT permission on that file

### 6.6 Edit History

Every Y.js binary update applied to an active document is appended to a `doc_edits` log table (separate from `file_versions`) with `user_id`, `document_id`, and `occurred_at`. This satisfies FR-COLLAB-07 and ACR-AUDIT requirements for archived documents being edited before archival.

---

## 7. Archive Module Design

### 7.1 Archival Workflow (SIP → AIP)

```
Archive Operator
       │
       ├─ POST /api/v1/archive ──────────────────► ArchiveController
       │   {fileId, title, author, ...}                    │
       │                                           ArchiveService.ingest(SIP)
       │                                                    │
       │                                    ┌───────────────┼─────────────────┐
       │                                    │               │                 │
       │                            Download file   Validate format   Validate sig
       │                            from Go FS      (PDF/A check)     (eIDAS)
       │                                    │               │                 │
       │                                    └───────────────┼─────────────────┘
       │                                                    │
       │                                    Compute SHA-256 checksum
       │                                                    │
       │                                    Fetch RFC 3161 timestamp from TSA
       │                                                    │
       │                                    Persist ArchiveRecordEntity
       │                                                    │
       │                                    Write AuditLogEntry (INGEST)
       │                                                    │
       │◄─────────────── AIP summary ───────────────────────┘
```

### 7.2 Integrity Sweep (Scheduled)

`JobScheduler` runs the integrity sweep every 24 hours:

```
for each ArchiveRecordEntity where integrity_status != 'MISMATCH':
  1. Download file bytes from Go File Server
  2. Compute SHA-256
  3. Compare with archive_records.checksum_sha256
  4. If match:  set integrity_status = 'OK', last_integrity_check = now()
  5. If mismatch:
       set integrity_status = 'MISMATCH'
       write AuditLogEntry (INTEGRITY_FAIL)
       send alert email to Archive Operators
```

### 7.3 Audit Event Types

| Event Type | Trigger |
|------------|---------|
| `INGEST` | Document submitted for archiving |
| `VIEW` | AIP metadata viewed |
| `DOWNLOAD` | File content downloaded |
| `METADATA_UPDATE` | Any AIP metadata field changed |
| `INTEGRITY_CHECK_OK` | Scheduled integrity check passed |
| `INTEGRITY_FAIL` | Checksum mismatch detected |
| `LEGAL_HOLD_PLACED` | Legal hold activated |
| `LEGAL_HOLD_RELEASED` | Legal hold removed |
| `RETENTION_EXPIRY_NOTIFIED` | Operator notified of retention expiry |
| `DESTRUCTION_APPROVED` | Operator approved destruction |
| `DESTROYED` | Document permanently deleted; destruction certificate written |
| `EXPORT` | DIP package generated and downloaded |

### 7.4 DIP Export Package Structure

```
archive-export-{archiveUuid}.zip
├── content/
│   └── {original_filename}          ← the archived file
├── manifest.json                    ← AIP metadata + all audit entries
├── timestamp.tst                    ← RFC 3161 timestamp token (binary)
└── signature.{xades|pades|cades}    ← electronic signature (if present)
```

`manifest.json` schema:
```json
{
  "archiveUuid": "...",
  "title": "...",
  "author": "...",
  "documentType": "...",
  "languageCode": "...",
  "checksumSha256": "...",
  "formatValidation": "PDF/A-1b",
  "signatureValid": true,
  "retentionYears": 10,
  "retainUntil": "2036-04-02",
  "archivedAt": "...",
  "archivedBy": "...",
  "auditTrail": [
    {
      "id": 1,
      "eventType": "INGEST",
      "actorUserId": 5,
      "actorIp": "192.168.1.1",
      "occurredAt": "...",
      "details": {},
      "entryHash": "...",
      "previousHash": "0000...0000"
    }
  ]
}
```

### 7.5 Retention and Destruction

```
JobScheduler (daily):
  for each archive_record where retain_until <= today
                             and legal_hold = false
                             and status != DESTRUCTION_APPROVED:
    → create retention expiry notification
    → send email to Archive Operators

On operator POST /archive/{uuid}/approve-destruction:
  → set status = DESTRUCTION_APPROVED
  → write DESTRUCTION_APPROVED audit entry

On operator DELETE /archive/{uuid}:
  → verify: status = DESTRUCTION_APPROVED and legal_hold = false
  → delete S3 object via Go File Server
  → set file.is_deleted = true
  → write DESTROYED audit entry (audit log entry is RETAINED permanently)
  → mark archive_record.destroyed_at = now()
```

---

## 8. Security Design

### 8.1 Authentication Flow

```
Login Request
     │
     ▼
UserRepository.findByUsernameOrEmail()
     │
     ▼
BCryptPasswordEncoder.matches(rawPassword, storedHash)
     │ success
     ▼
Generate access token:
  Claims: sub=email, uid=userId, iat=now, exp=now+1h
  Sign with RSA private key (RS256)

Generate refresh token:
  rawToken = SecureRandom(256 bits) → Base64url
  hash = SHA-256(rawToken)
  Persist RefreshTokenEntity(hash, userId, exp=now+30d)
  Return rawToken to client (never stored)
```

### 8.2 JWT Validation

Every request to a protected endpoint passes through `JwtAuthenticationFilter`:

```
Extract Bearer token from Authorization header
     │
JwtDecoder.decode(token)
  → verifies RS256 signature
  → checks exp claim
  → throws on failure (→ 401)
     │
Extract uid claim → load UserEntity
     │
Set SecurityContextHolder authentication
     │
Proceed to controller
```

### 8.3 Token Storage Strategy

| Token Type | Client Storage | Server Storage |
|-----------|---------------|----------------|
| Access token (JWT) | Memory (JS variable) | Not stored |
| Refresh token | HttpOnly, Secure, SameSite=Strict cookie | SHA-256 hash in `refresh_tokens` |
| Password reset token | URL parameter (one-time link) | SHA-256 hash in `password_reset_tokens` |
| Share token | URL path segment | SHA-256 hash in `shares` |

Storing the access token in memory (not localStorage) prevents XSS token theft. The HttpOnly cookie for the refresh token prevents JavaScript access.

### 8.4 Password Policy

- Minimum length: 8 characters
- Must contain: at least one uppercase letter, one lowercase letter, one digit, one special character
- Stored as `bcrypt(password, cost=12)`
- Reset tokens expire after 30 minutes and are single-use

### 8.5 Authorization Model

Two mechanisms:

1. **Ownership check** — for files/folders, the service layer verifies `resource.ownerId == currentUserId` before any mutation.
2. **Share-based access** — for shared resources, the service resolves the `ShareEntity` (by token hash), checks expiry, revocation, and permission level.

Role hierarchy (Spring Security):
```
ROLE_ARCHIVE_OPERATOR > ROLE_ADMIN > ROLE_USER
```

Archive endpoints require `ROLE_ARCHIVE_OPERATOR` or `ROLE_ADMIN`.

### 8.6 Rate Limiting

Implemented via a `RateLimitFilter` on `/api/auth/**`:
- Sliding window: 10 requests per 60 seconds per remote IP
- On breach: `429 Too Many Requests` + `Retry-After` header
- Block duration: 15 minutes

### 8.7 CORS Configuration

Allowed origins are read from `application.properties` (`mydrive.cors.allowed-origins`). In production, only the frontend origin is listed. No wildcard origins are permitted.

### 8.8 Input Validation

- All request DTOs are annotated with Jakarta Bean Validation constraints (`@NotBlank`, `@Size`, `@Email`, etc.)
- `@Valid` on controller parameters triggers validation before the service layer is invoked
- Validation failures return `400 Bad Request` with a field-level error map

---

## 9. Deployment and Infrastructure

### 9.1 Environment Overview

| Environment | Purpose |
|-------------|---------|
| `development` | Local developer machines; H2 in-memory DB optional |
| `staging` | Pre-production; mirrors production topology |
| `production` | Live system |

### 9.2 Container Architecture

Each service is packaged as a Docker container:

```
docker-compose.yml (development / staging)
├── mydrive-api          # Spring Boot fat JAR
├── file-server          # Go binary
├── mysql                # MySQL 8
├── minio                # S3-compatible object storage
└── nginx                # Reverse proxy + TLS termination
```

### 9.3 Environment Variables

Critical secrets are injected as environment variables (never committed to source control):

| Variable | Used By | Description |
|----------|---------|-------------|
| `DB_URL` | Java API | JDBC connection string |
| `DB_USERNAME` / `DB_PASSWORD` | Java API | MySQL credentials |
| `JWT_PRIVATE_KEY` | Java API | RSA private key (PEM) for JWT signing |
| `JWT_PUBLIC_KEY` | Java API | RSA public key (PEM) for JWT verification |
| `INTERNAL_TOKEN` | Java API, File Server | Shared secret for internal API calls |
| `S3_ENDPOINT` | File Server | MinIO/S3 endpoint URL |
| `S3_BUCKET` | File Server | Bucket name |
| `S3_ACCESS_KEY` / `S3_SECRET_KEY` | File Server | S3 credentials |
| `OPENAI_API_KEY` | Java API | OpenAI API key |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | Java API | Gmail SMTP credentials |
| `TSA_URL` | Java API | Time-Stamping Authority endpoint |

### 9.4 Database Migrations

Schema changes are managed with **Flyway**:

```
src/main/resources/db/migration/
  V1__initial_schema.sql
  V2__add_archive_tables.sql
  V3__add_file_versions.sql
  ...
```

Flyway runs automatically on application startup. Migrations are versioned, sequential, and never modified after release.

### 9.5 Scheduled Jobs Summary

| Job | Frequency | Description |
|-----|-----------|-------------|
| Token cleanup | Hourly | Delete expired refresh and password-reset tokens |
| Share cleanup | Daily | Mark expired shares as revoked |
| Archive integrity sweep | Daily | Recompute checksums for all archived documents |
| Retention notifications | Daily | Notify operators of documents past `retain_until` |
| Trash cleanup | Daily | Permanently delete soft-deleted files older than 30 days |

---

## 10. Appendix

### 10.1 Sequence Diagram: Collaborative Edit Session

```
Alice (Browser)          Java API (WS Hub)         Bob (Browser)
      │                         │                        │
      ├─ WS CONNECT ───────────►│                        │
      │◄─ full doc state ───────┤                        │
      │                         │◄──── WS CONNECT ───────┤
      │                         ├──── full doc state ────►│
      │                         │                        │
      ├─ Y.js update (delta) ──►│                        │
      │                         ├── broadcast delta ─────►│
      │                         ├── applyUpdate(doc)      │
      │                         ├── scheduleAutosave()    │
      │◄─ echo delta ───────────┤                        │
      │                         │                        │
      │                    [30s autosave]                 │
      │                         ├── encode state          │
      │                         ├── INSERT file_versions  │
      │                         ├── UPDATE files          │
```

### 10.2 Sequence Diagram: Archive Ingest

```
Operator                  ArchiveController / Service          Go File Server      TSA
    │                              │                                │               │
    ├─ POST /archive ─────────────►│                               │               │
    │  {fileId, metadata}          │                               │               │
    │                              ├─ GET /internal/download/{id} ►│               │
    │                              │◄─ file bytes ─────────────────┤               │
    │                              │                               │               │
    │                              ├─ compute SHA-256              │               │
    │                              ├─ validate PDF/A               │               │
    │                              ├─ validate eIDAS sig           │               │
    │                              │                               │               │
    │                              ├─ POST TSA (hash) ─────────────────────────────►│
    │                              │◄─ timestamp token (RFC 3161) ──────────────────┤
    │                              │                               │               │
    │                              ├─ INSERT archive_records       │               │
    │                              ├─ INSERT audit_log (INGEST)    │               │
    │                              │                               │               │
    │◄─ 201 AIP summary ───────────┤                               │               │
```

### 10.3 Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | 2026-04-02 | — | Initial draft |
