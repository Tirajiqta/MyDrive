# Software Requirements Specification
## MyDrive — Cloud Document Storage and Archive System

**Version:** 1.0  
**Date:** 2026-04-02  
**Status:** Draft  

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Overall Description](#2-overall-description)
3. [Functional Requirements](#3-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [Document Archive Compliance Requirements](#5-document-archive-compliance-requirements)
6. [External Interface Requirements](#6-external-interface-requirements)
7. [System Constraints](#7-system-constraints)
8. [Appendix](#8-appendix)

---

## 1. Introduction

### 1.1 Purpose

This Software Requirements Specification (SRS) describes the functional and non-functional requirements for **MyDrive**, a cloud-based document storage, sharing, and real-time collaborative editing platform. The document serves as the authoritative reference for development, testing, and compliance validation.

### 1.2 Scope

MyDrive is a web and mobile application that allows users to:

- Store, organize, and manage files and folders in the cloud
- Edit text-based documents collaboratively in real time
- Share files and folders with other users via permissioned links
- Archive documents in a manner compliant with electronic document archive standards

The system consists of four components:

| Component | Technology | Role |
|-----------|-----------|------|
| Java API (`MYDrive`) | Spring Boot 3, Java 21 | Business logic, authentication, metadata |
| File Server | Go | Binary file storage and retrieval |
| Web Frontend | Next.js 15, React 19, TypeScript | Primary user interface |
| Mobile App | TBD | Mobile user interface |

### 1.3 Definitions and Acronyms

| Term | Definition |
|------|-----------|
| **SRS** | Software Requirements Specification |
| **JWT** | JSON Web Token — a compact, signed token used for stateless authentication |
| **CRDT** | Conflict-free Replicated Data Type — a data structure for real-time collaborative editing |
| **OT** | Operational Transformation — an algorithm for collaborative editing conflict resolution |
| **OAIS** | Open Archival Information System (ISO 14721) |
| **SIP** | Submission Information Package (OAIS term) |
| **AIP** | Archival Information Package (OAIS term) |
| **DIP** | Dissemination Information Package (OAIS term) |
| **eIDAS** | EU Regulation on Electronic Identification and Trust Services |
| **GDPR** | General Data Protection Regulation (EU 2016/679) |
| **REST** | Representational State Transfer |
| **MIME** | Multipurpose Internet Mail Extensions — a type identifier for file formats |
| **RBAC** | Role-Based Access Control |
| **MFA** | Multi-Factor Authentication |
| **TLS** | Transport Layer Security |

### 1.4 Overview

Section 2 describes the product context and user classes. Section 3 covers functional requirements organized by feature area. Section 4 covers non-functional requirements. Section 5 covers document archive compliance requirements. Sections 6 and 7 cover interfaces and constraints.

---

## 2. Overall Description

### 2.1 Product Perspective

MyDrive operates as a standalone cloud service accessible via web browser and mobile application. It consists of microservices communicating over HTTP/REST, with a shared MySQL database for metadata and a dedicated file storage service for binary data.

```
┌─────────────────────────────────────────────────────────┐
│                     Client Layer                        │
│         Web (Next.js)          Mobile (TBD)             │
└────────────────────┬────────────────────────────────────┘
                     │ HTTPS / WebSocket
┌────────────────────▼────────────────────────────────────┐
│                  API Gateway / Load Balancer             │
└──────────┬──────────────────────────┬───────────────────┘
           │                          │
┌──────────▼──────────┐   ┌──────────▼──────────┐
│  Java API (MYDrive) │   │  Go File Server      │
│  Auth, Metadata,    │   │  Binary upload /     │
│  Sharing, Plans,    │   │  download / stream   │
│  Real-time collab   │   │                      │
└──────────┬──────────┘   └──────────┬──────────┘
           │                          │
┌──────────▼──────────┐   ┌──────────▼──────────┐
│  MySQL Database     │   │  Object / Block       │
│  (Metadata)         │   │  Storage              │
└─────────────────────┘   └─────────────────────┘
```

### 2.2 Product Functions (Summary)

- User registration, authentication, and profile management
- File and folder CRUD with nested folder trees
- Real-time collaborative document editing (multiple simultaneous editors)
- File sharing via permissioned, optionally time-limited links
- Subscription tiers with storage quotas
- Document archiving with metadata preservation and audit trail
- AI-powered chat assistant for user support
- Multilingual UI (English, Bulgarian)
- Support ticket system

### 2.3 User Classes and Characteristics

| User Class | Description | Technical Level |
|------------|-------------|-----------------|
| **End User (Free)** | Individual storing personal files; limited storage quota | Low |
| **End User (Paid)** | Power user or professional with expanded storage and features | Low–Medium |
| **Collaborator** | User who receives a share link; may edit documents without owning them | Low |
| **Administrator** | Manages user accounts, plans, and system settings | High |
| **Archive Operator** | Responsible for verifying archival integrity and compliance | Medium–High |

### 2.4 Operating Environment

- **Web:** Any modern browser supporting ES2020+ (Chrome 90+, Firefox 88+, Safari 14+, Edge 90+)
- **Mobile:** iOS 15+ and Android 10+ (planned)
- **Server:** Linux-based cloud infrastructure; Java 21 JVM, Go 1.21+
- **Database:** MySQL 8.0+
- **Network:** HTTPS (TLS 1.2+) for all external communication; WebSocket over WSS for real-time editing

### 2.5 Assumptions and Dependencies

- Users have internet access and a supported browser.
- The Go file server is responsible for all binary I/O; the Java API never writes file bytes directly.
- Email delivery depends on a configured SMTP provider (currently Gmail).
- AI chatbot feature depends on availability of the OpenAI API.
- Real-time collaboration depends on a WebSocket server (to be implemented).

---

## 3. Functional Requirements

Requirements are written as: **FR-[Area]-[Number]: [Title]**

### 3.1 Authentication and User Management

**FR-AUTH-01: User Registration**  
The system shall allow a new user to register with a unique username, a unique email address, and a password. Upon successful registration the system shall automatically assign the user a FREE subscription plan.

**FR-AUTH-02: Login**  
The system shall authenticate users by either username or email combined with their password. On success, the system shall return a short-lived JWT access token (1 hour) and an opaque refresh token (30 days).

**FR-AUTH-03: Token Refresh**  
The system shall allow clients to exchange a valid, unexpired refresh token for a new access token and rotate the refresh token. The old refresh token shall be invalidated immediately after rotation.

**FR-AUTH-04: Logout**  
The system shall revoke the user's active refresh token on logout, preventing further token renewal without re-authentication.

**FR-AUTH-05: Forgot Password**  
The system shall send a password-reset link to the user's registered email. The link shall contain a single-use token that expires after 30 minutes.

**FR-AUTH-06: Reset Password**  
The system shall allow users to set a new password by presenting a valid, unexpired, unused password-reset token.

**FR-AUTH-07: Change Password**  
Authenticated users shall be able to change their password by providing their current password and a new password.

**FR-AUTH-08: Current User Profile**  
Authenticated users shall be able to retrieve their own profile information, including name, email, storage usage, and active subscription plan.

**FR-AUTH-09: Multi-Factor Authentication (MFA)**  
The system shall support optional TOTP-based MFA. When enabled, login shall require both credentials and the current TOTP code.

### 3.2 File Management

**FR-FILE-01: Upload File**  
Users shall be able to upload a file to a specific folder (or the root). The system shall store the file in the file server, record its metadata (name, MIME type, size, upload timestamp, owner, parent folder), and update the user's `currentStorageUsed`.

**FR-FILE-02: Download File**  
Users shall be able to download any file they own or have VIEW (or higher) permission on.

**FR-FILE-03: List Files**  
Users shall be able to list all non-deleted files within a specified folder (or root). The list shall include file name, size, MIME type, modification timestamp, and sharing status.

**FR-FILE-04: Rename File**  
Users shall be able to rename a file they own or have EDIT permission on.

**FR-FILE-05: Move File**  
Users shall be able to move a file to a different folder within their own drive.

**FR-FILE-06: Soft Delete File**  
Users shall be able to delete a file. Deleted files shall be marked with an `isDeleted` flag and moved to a logical Trash area; they shall not be immediately purged from storage.

**FR-FILE-07: Restore File**  
Users shall be able to restore a soft-deleted file from Trash within a configurable retention period (default 30 days).

**FR-FILE-08: Permanent Delete**  
After the retention period, or upon explicit user request, a file shall be permanently deleted from storage and its metadata purged.

**FR-FILE-09: Storage Quota Enforcement**  
The system shall prevent file uploads that would cause a user's storage usage to exceed the limit defined by their active subscription plan.

**FR-FILE-10: File Versioning**  
For text-based documents, the system shall maintain a history of saved versions. Users shall be able to view and restore any previous version.

### 3.3 Folder Management

**FR-FOLD-01: Create Folder**  
Users shall be able to create a folder with a given name within another folder or at the root. Duplicate names (case-insensitive) within the same parent shall be rejected.

**FR-FOLD-02: List Folders**  
Users shall be able to list all non-deleted folders within a parent folder (or root).

**FR-FOLD-03: Rename Folder**  
Users shall be able to rename a folder they own.

**FR-FOLD-04: Move Folder**  
Users shall be able to move a folder to a different parent folder. The system shall detect and reject moves that would create a circular reference.

**FR-FOLD-05: Soft Delete Folder**  
Deleting a folder shall recursively soft-delete all contained files and subfolders.

**FR-FOLD-06: Restore Folder**  
Restoring a folder from Trash shall restore it and all its direct children that were deleted in the same operation.

### 3.4 Real-Time Collaborative Editing

**FR-COLLAB-01: Open Document for Editing**  
Users with EDIT permission on a text-based document shall be able to open it in an in-browser editor.

**FR-COLLAB-02: Real-Time Synchronization**  
All changes made by one editor shall be propagated to all other active editors of the same document within 500 ms under normal network conditions.

**FR-COLLAB-03: Conflict Resolution**  
The system shall resolve simultaneous edits from multiple users without data loss using an OT or CRDT-based algorithm.

**FR-COLLAB-04: Presence Indicators**  
The editor shall display the name and cursor position of all other users currently editing the document.

**FR-COLLAB-05: Connection Recovery**  
If a client loses its WebSocket connection, it shall automatically attempt to reconnect and re-sync the document state upon reconnection.

**FR-COLLAB-06: Autosave**  
The system shall automatically save the document state at regular intervals (configurable, default 30 seconds) and upon the last editor leaving.

**FR-COLLAB-07: Edit History**  
The system shall record a log of all edits with the contributing user and timestamp for audit purposes.

### 3.5 Sharing

**FR-SHARE-01: Create Share Link**  
The owner of a file or folder shall be able to generate a share link. The link shall encode a permission level (VIEW, EDIT, or DELETE) and optionally an expiration date.

**FR-SHARE-02: Resolve Share Link**  
Any bearer of a valid share link shall be able to resolve it to the corresponding resource metadata without authentication. The raw token shall never be stored; only its SHA-256 hash shall be persisted in the database.

**FR-SHARE-03: Access Control via Share**  
The system shall enforce the permission level embedded in the share link. A user with VIEW access shall not be able to modify or delete the resource.

**FR-SHARE-04: Revoke Share**  
The resource owner shall be able to revoke any share at any time, immediately invalidating the link.

**FR-SHARE-05: List Shares**  
Authenticated users shall be able to list all active shares they have created, including permission level, expiration, and whether the share has been revoked.

**FR-SHARE-06: Share Expiration**  
Expired shares shall be automatically treated as revoked. Expired share records shall be cleaned up by a scheduled background job.

### 3.6 Subscription Plans

**FR-PLAN-01: Plan Tiers**  
The system shall support at least two plan tiers: FREE (limited storage, e.g. 5 GB) and PAID (expanded storage, e.g. 100 GB+). Plan details shall be stored in the database and be configurable without code changes.

**FR-PLAN-02: Automatic Free Plan on Registration**  
Every newly registered user shall be automatically subscribed to the FREE plan.

**FR-PLAN-03: Upgrade Plan**  
Users shall be able to upgrade their subscription plan. The system shall record the start date, renewal date, and payment method reference.

**FR-PLAN-04: Subscription Status**  
The system shall track subscription statuses: ACTIVE, EXPIRED, CANCELLED.

**FR-PLAN-05: Multilingual Plan Descriptions**  
Plan names and descriptions shall be available in all supported languages via translation entities.

### 3.7 Document Archive

**FR-ARCH-01: Archival Designation**  
Users or administrators shall be able to designate a file or folder as an archived document, locking it against modification or deletion except by an authorized Archive Operator.

**FR-ARCH-02: Archive Metadata**  
Each archived document shall carry mandatory metadata: document title, author, creation date, archival date, document type/category, retention period, language, and a content checksum (SHA-256).

**FR-ARCH-03: Integrity Verification**  
The system shall periodically re-compute the checksum of each archived document and compare it with the stored value. Any mismatch shall trigger an alert and be logged in the audit trail.

**FR-ARCH-04: Audit Trail**  
Every action on an archived document (view, download, metadata change, integrity check, destruction) shall be recorded in an immutable audit log with actor, timestamp, and action type.

**FR-ARCH-05: Retention Policy**  
Each archived document shall be assigned a retention period. Upon expiry, the system shall notify an Archive Operator; the document shall not be automatically destroyed without explicit operator approval.

**FR-ARCH-06: Legal Hold**  
Archive Operators shall be able to place a legal hold on a document, suspending any retention-based destruction for the duration of the hold.

**FR-ARCH-07: Export (DIP)**  
Archive Operators shall be able to export an archived document along with all its metadata in a standard format (PDF/A-1b for documents; a manifest JSON file listing all metadata, checksums, and audit events).

**FR-ARCH-08: Access Restriction**  
Archived documents shall only be accessible to users explicitly granted access by the Archive Operator.

### 3.8 AI Assistant

**FR-AI-01: Contextual Chat**  
The system shall provide a chat interface backed by an LLM (GPT-4o-mini) to answer user questions about the platform.

**FR-AI-02: Interaction Persistence**  
Each user–assistant exchange shall be persisted in the database, optionally linked to a support ticket.

### 3.9 Support Tickets

**FR-SUPP-01: Create Ticket**  
Users shall be able to submit a support ticket with a subject, description, and priority level (LOW, MEDIUM, HIGH, CRITICAL).

**FR-SUPP-02: Ticket Lifecycle**  
Tickets shall progress through statuses: OPEN → IN_PROGRESS → RESOLVED → CLOSED.

**FR-SUPP-03: Agent Assignment**  
An administrator shall be able to assign a ticket to a support agent.

**FR-SUPP-04: GitHub Integration**  
Critical tickets may be mirrored to a GitHub repository as issues; the ticket shall store the resulting GitHub issue URL and ID.

### 3.10 Internationalization

**FR-I18N-01: Language Support**  
The UI and all system-generated content (plan descriptions, FAQ answers, email templates) shall be available in English and Bulgarian.

**FR-I18N-02: User Language Preference**  
Each user shall be able to set a preferred language. The system shall serve content in that language where translations exist, falling back to English otherwise.

---

## 4. Non-Functional Requirements

### 4.1 Performance

**NFR-PERF-01:** File metadata queries (list files in a folder) shall return within 200 ms (p95) for folders containing up to 10,000 items.

**NFR-PERF-02:** File upload throughput shall be at least 50 MB/s per connection to the file server.

**NFR-PERF-03:** Real-time collaborative edits shall be propagated to all connected editors within 500 ms under normal network conditions (< 100 ms RTT).

**NFR-PERF-04:** The Java API shall support at least 500 concurrent authenticated requests without degradation.

### 4.2 Availability and Reliability

**NFR-AVAIL-01:** The system shall target 99.9% uptime (< 8.7 hours downtime per year) for all core services.

**NFR-AVAIL-02:** Database backups shall be performed at least once daily. Recovery Point Objective (RPO) shall not exceed 24 hours; Recovery Time Objective (RTO) shall not exceed 4 hours.

**NFR-AVAIL-03:** The file storage service shall replicate data to at least one secondary location to protect against disk failure.

### 4.3 Security

**NFR-SEC-01:** All data in transit shall be encrypted using TLS 1.2 or higher.

**NFR-SEC-02:** All passwords shall be stored as bcrypt hashes with a work factor of at least 12.

**NFR-SEC-03:** JWT tokens shall be signed with RS256 (asymmetric) keys. Private keys shall never be exposed outside the API service.

**NFR-SEC-04:** Refresh tokens and password-reset tokens shall be stored only as SHA-256 hashes; raw token values shall not be persisted.

**NFR-SEC-05:** The API shall implement rate limiting on authentication endpoints (max 10 failed attempts per minute per IP, then 15-minute block).

**NFR-SEC-06:** The system shall pass OWASP Top 10 vulnerability assessment before production deployment.

**NFR-SEC-07:** CORS shall be configured to allow only explicitly whitelisted origins.

### 4.4 Scalability

**NFR-SCALE-01:** The architecture shall support horizontal scaling of the Java API via stateless design (JWT-based auth, no server-side session).

**NFR-SCALE-02:** The file server shall be deployable behind a load balancer with shared object storage as the backing store.

### 4.5 Maintainability

**NFR-MAINT-01:** Backend API code shall achieve at least 70% unit-test line coverage.

**NFR-MAINT-02:** All public API endpoints shall be documented with OpenAPI 3.0 annotations.

**NFR-MAINT-03:** Database schema changes shall be managed through versioned migration scripts (e.g., Flyway or Liquibase).

### 4.6 Usability

**NFR-USE-01:** The web interface shall conform to WCAG 2.1 Level AA accessibility guidelines.

**NFR-USE-02:** A new user shall be able to upload and share their first file within 5 minutes of registration without external documentation.

### 4.7 Data Protection (GDPR)

**NFR-GDPR-01:** Users shall be able to request an export of all their personal data (right of access, Art. 15 GDPR).

**NFR-GDPR-02:** Users shall be able to request deletion of their account and all associated personal data (right to erasure, Art. 17 GDPR), subject to retention obligations on archived documents.

**NFR-GDPR-03:** The Privacy Policy shall clearly disclose all categories of personal data collected, their purpose, and retention periods.

---

## 5. Document Archive Compliance Requirements

MyDrive's archive module is designed to comply with the following standards and regulations:

| Standard / Regulation | Scope |
|-----------------------|-------|
| ISO 15489-1:2016 (Records Management) | Organizational records management principles |
| ISO 14721:2012 (OAIS) | Archival information package model |
| ISO 19005 (PDF/A) | Long-term preservation file format |
| EU Regulation 910/2014 (eIDAS) | Electronic signatures and trust services |
| GDPR (EU 2016/679) | Personal data in archived documents |
| Bulgarian Electronic Document and Electronic Certification Services Act (ЗЕДЕУУ) | National legal framework for electronic documents |

### 5.1 ISO 15489 — Records Management Compliance

**ACR-15489-01: Authenticity**  
Archived records shall be demonstrably authentic. The system shall record the creator's identity, creation date, and any subsequent changes in an immutable audit trail.

**ACR-15489-02: Reliability**  
A record shall be created at or near the time of the event it documents. The system shall record and display the original creation timestamp of every document.

**ACR-15489-03: Integrity**  
Archived records shall be protected from unauthorized alteration. SHA-256 checksums shall be computed at archival time and re-verified on every access and during scheduled integrity sweeps.

**ACR-15489-04: Usability**  
Archived records shall remain accessible and interpretable over their entire retention period. The system shall store MIME type, character encoding, and format version with each document.

**ACR-15489-05: Retention Schedule**  
Each record class (document type) shall be assigned a retention schedule specifying the minimum retention period. The system shall enforce retention schedules and notify operators when records are due for review or destruction.

**ACR-15489-06: Disposition**  
Records at the end of their retention period shall be either permanently destroyed (with destruction certificate logged) or transferred to a long-term preservation repository.

### 5.2 ISO 14721 (OAIS) — Information Package Model

The archive module shall implement the three OAIS information package types:

**ACR-OAIS-01: Submission Information Package (SIP)**  
When a document is submitted for archiving, the system shall accept: the content file, a descriptor with mandatory metadata fields (see FR-ARCH-02), and optionally a digital signature.

**ACR-OAIS-02: Archival Information Package (AIP)**  
The system shall transform the SIP into an AIP by augmenting it with: a unique archive identifier (UUID), ingest timestamp, checksum (SHA-256), format validation result, and the complete audit event for the ingest. AIPs shall be stored in immutable form.

**ACR-OAIS-03: Dissemination Information Package (DIP)**  
When an Archive Operator requests export (see FR-ARCH-07), the system shall produce a DIP containing: the content file, all AIP metadata rendered as a JSON manifest, and the audit trail for the document.

### 5.3 PDF/A Compliance (ISO 19005)

**ACR-PDFA-01:** Documents uploaded as PDF shall be validated against the PDF/A-1b profile upon archival. Non-conforming PDFs shall be flagged with a warning; archival shall proceed but the non-conformance shall be recorded in the AIP metadata.

**ACR-PDFA-02:** The system shall offer an optional conversion of PDF files to PDF/A-1b as part of the archival workflow.

### 5.4 eIDAS Compliance

**ACR-EIDAS-01: Electronic Signature Attachment**  
The archival workflow shall allow attachment of a qualified or advanced electronic signature (XAdES, PAdES, or CAdES format) to a document.

**ACR-EIDAS-02: Signature Validation**  
The system shall validate attached signatures against a trusted certificate chain at the time of archival. Validation result and the signing certificate's issuer/serial shall be stored in the AIP metadata.

**ACR-EIDAS-03: Timestamp**  
Each AIP shall include a qualified electronic timestamp (RFC 3161) issued by a trusted Time-Stamping Authority (TSA), providing cryptographic proof of the document's existence at a specific point in time.

### 5.5 Audit Trail Requirements

**ACR-AUDIT-01:** The audit log shall be append-only. No audit record shall be modifiable or deletable through normal application interfaces.

**ACR-AUDIT-02:** Each audit entry shall contain: event type, affected document ID, actor (user ID or system), timestamp (UTC, millisecond precision), client IP address, and a hash chain linking entries (each entry's hash includes the previous entry's hash).

**ACR-AUDIT-03:** The audit log shall be exportable in CSV and JSON format for compliance reporting.

**ACR-AUDIT-04:** Audit logs shall be retained for a minimum period of 10 years or the retention period of the associated document, whichever is longer.

---

## 6. External Interface Requirements

### 6.1 User Interfaces

- The web frontend shall be a single-page application (SPA) built with Next.js, responsive across desktop (1024px+) and tablet (768px+) viewports.
- The collaborative editor shall support rich text via a web-based editor component (e.g., TipTap, Quill, or ProseMirror).
- The mobile application (future) shall replicate core file management and viewing features.

### 6.2 API Interfaces

- All backend services shall expose RESTful HTTP APIs returning JSON.
- Real-time collaboration shall use the WebSocket protocol (WSS) on a dedicated endpoint.
- API versioning shall follow the path prefix pattern `/api/v1/`.
- All API responses shall include appropriate HTTP status codes and a consistent error body: `{ "error": "...", "message": "...", "timestamp": "..." }`.

### 6.3 Hardware Interfaces

No direct hardware interfaces. The system runs on commodity cloud hardware.

### 6.4 Software Interfaces

| External Service | Purpose | Protocol |
|-----------------|---------|---------|
| MySQL 8.0+ | Persistent metadata storage | JDBC |
| Object Storage (e.g., MinIO / S3-compatible) | Binary file storage | S3 API (HTTPS) |
| OpenAI API | AI chatbot completions | HTTPS/REST |
| SMTP (Gmail) | Transactional email delivery | STARTTLS/SMTP |
| Time-Stamping Authority (TSA) | RFC 3161 timestamps for archive | HTTPS |

### 6.5 Communication Interfaces

- All external communication shall use HTTPS (TLS 1.2+).
- WebSocket connections for collaboration shall use WSS.
- Internal service-to-service communication may use HTTP over a private network.

---

## 7. System Constraints

**CON-01:** The Java API shall target the Java 21 LTS runtime.

**CON-02:** The database shall be MySQL 8.0 or compatible.

**CON-03:** The file server shall be implemented in Go 1.21+.

**CON-04:** The web frontend shall be built with Next.js 15 (App Router).

**CON-05:** The system shall not store raw credentials (passwords, tokens) in the database; only derived cryptographic representations (bcrypt hashes, SHA-256 hashes) are permitted.

**CON-06:** The system shall comply with GDPR data residency requirements; personal data shall reside within the EU.

---

## 8. Appendix

### 8.1 Use Case Summary

| ID | Use Case | Primary Actor |
|----|----------|--------------|
| UC-01 | Register and log in | End User |
| UC-02 | Upload a file | End User |
| UC-03 | Create and organize folders | End User |
| UC-04 | Edit a document collaboratively | End User, Collaborator |
| UC-05 | Share a file via link | End User |
| UC-06 | Archive a document | End User / Archive Operator |
| UC-07 | Verify archive integrity | Archive Operator |
| UC-08 | Export archived document (DIP) | Archive Operator |
| UC-09 | Place legal hold | Archive Operator |
| UC-10 | Upgrade subscription plan | End User |
| UC-11 | Submit and track support ticket | End User |
| UC-12 | Manage users and plans | Administrator |

### 8.2 Glossary of Archive Document Types

| Category | Examples | Typical Retention |
|----------|---------|-----------------|
| Administrative | Contracts, policies, meeting minutes | 5–10 years |
| Financial | Invoices, receipts, audit reports | 10 years |
| Legal | Court orders, notarial acts | Permanent |
| Personal | HR files, performance reviews | Duration of employment + 5 years |
| Technical | System logs, test reports | 3–5 years |

### 8.3 Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | 2026-04-02 | — | Initial draft |
