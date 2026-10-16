# SecureVault — AES Cloud Backup

**SecureVault** is an enterprise-grade, cloud-security web application for securely uploading, encrypting, storing, managing, downloading, and restoring confidential financial documents.

Built with an AWS-inspired clean management console, SecureVault guarantees **Zero-Plaintext Cloud Storage** via envelope encryption combining **AES-256-GCM** (authenticated encryption) and **AWS Key Management Service (KMS)**, persisting encrypted payloads to **Supabase Storage**.

---

## Architecture Overview

```
                      +-----------------------------+
                      |   React 18 + TypeScript     |
                      |   (Vite + Tailwind CSS)     |
                      +--------------+--------------+
                                     |
                          HTTPS / TLS Transport
                                     |
                      +--------------v--------------+
                      |   Spring Boot 3.3 Backend   |
                      |   (Java 17/21 JCA Engine)   |
                      +-------+-------------+-------+
                              |             |
           Envelope Decrypt / |             | REST API
               Key Generation |             | (Octet-Stream)
                              |             |
                 +------------v----+   +----v--------------------+
                 |     AWS KMS     |   |    Supabase Cloud       |
                 | (Symmetric CMK) |   | - PostgreSQL (Metadata) |
                 +-----------------+   | - Storage (Ciphertext)  |
                                       +-------------------------+
```

### Cryptographic Security Model

1. **Envelope Encryption:**
   - For every backup, a fresh 256-bit Data Encryption Key (DEK) is requested from KMS.
   - The document is encrypted in backend memory using `AES/GCM/NoPadding`.
   - The plaintext DEK is securely zeroed out from memory immediately after encryption.
   - Only the KMS-wrapped (ciphertext) DEK is stored in the database.
2. **Authenticated Encryption (AES-256-GCM):**
   - Integrates confidentiality and integrity verification in a single pass.
   - A cryptographically unique 96-bit nonce (`SecureRandom`) is generated for every backup.
   - A 128-bit Galois authentication tag is verified during restoration before releasing plaintext.
   - If any bit of the ciphertext or tag is modified, decryption fails with an integrity error.
3. **Storage Isolation:**
   - Files are stored in a private Supabase Storage bucket named `encrypted-backups`.
   - The client never uploads directly to Supabase storage.
   - Plaintext files never touch cloud storage disks.

---

## Technology Stack

| Layer | Technologies |
|---|---|
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS, Lucide Icons, TanStack Query, React Hook Form, Zod, Sonner |
| **Backend** | Java 17/21, Spring Boot 3.3.5, Spring Security, Spring Data JPA, Flyway, JCA (AES-GCM), Bucket4j |
| **Database & Storage**| Supabase PostgreSQL, Supabase Storage (`encrypted-backups`), Supabase Auth |
| **Key Management** | AWS KMS (Production) / Local KEK Profile (Development) |

---

## Directory Structure

```
d:/CNSProj2/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/securevault/
│   │   │   │   ├── config/              # SecurityConfig, CORS
│   │   │   │   ├── controller/          # BackupController, HealthController
│   │   │   │   ├── crypto/              # AesGcmEncryptionService, KeyManagementService
│   │   │   │   ├── dto/                 # BackupDto, BackupSummaryDto, UploadResponseDto
│   │   │   │   ├── exception/           # GlobalExceptionHandler, Custom Exceptions
│   │   │   │   ├── model/               # Backup JPA Entity, BackupStatus
│   │   │   │   ├── repository/          # BackupRepository (Owner-scoped)
│   │   │   │   ├── security/            # JwtAuthenticationFilter, SecurityUtils
│   │   │   │   ├── service/             # BackupService
│   │   │   │   └── storage/             # SupabaseStorageService (Java 21 HttpClient)
│   │   │   └── resources/
│   │   │       ├── application.yml      # Base configuration
│   │   │       ├── application-dev.yml  # Dev profile (Local KEK)
│   │   │       ├── application-prod.yml # Prod profile (AWS KMS)
│   │   │       └── db/migration/        # V1__create_backups_table.sql
│   │   └── test/                        # Crypto, Service, and KMS Unit Tests
│   ├── pom.xml
│   └── .env.example
├── frontend/
│   ├── src/
│   │   ├── components/                  # UI components, modals (BackupDetails, Restore, Delete)
│   │   ├── hooks/                       # useAuth (Supabase Auth integration)
│   │   ├── layouts/                     # AppLayout (Navy sidebar, topbar)
│   │   ├── lib/                         # Supabase client, api client, utils
│   │   ├── pages/                       # Overview, Backups, Upload, Security, Settings, Auth
│   │   └── types/                       # TypeScript interfaces
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts
│   └── .env.example
├── .gitignore
└── README.md
```

---

## Prerequisites

- **Java JDK:** 17 LTS or 21 LTS installed (`java -version`)
- **Maven:** 3.8+ installed (`mvn -v`)
- **Node.js:** v18+ or v20+ LTS (`node -v`, `npm -v`)
- **Supabase Account:** with PostgreSQL database and private Storage bucket
- **AWS Account (Production):** with a Customer Managed KMS Key (CMK)

---

## Supabase Setup Instructions

### 1. Create Storage Bucket
1. Open your Supabase Dashboard.
2. Navigate to **Storage** → **New Bucket**.
3. Name the bucket: `encrypted-backups`.
4. Ensure **Public bucket is unchecked** (Bucket must be **PRIVATE**).
5. Click **Create bucket**.

### 2. Configure Database Tables
The backend includes automated Flyway migrations (`V1__create_backups_table.sql`). When Spring Boot starts, it will automatically create the `backups` table with all indexes and constraints.

If running migrations manually in the Supabase SQL Editor:
```sql
CREATE TABLE IF NOT EXISTS backups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    object_path VARCHAR(1000) NOT NULL UNIQUE,
    original_size_bytes BIGINT NOT NULL,
    encrypted_size_bytes BIGINT,
    mime_type VARCHAR(255) NOT NULL,
    encryption_algorithm VARCHAR(50) NOT NULL DEFAULT 'AES/GCM/NoPadding',
    format_version INTEGER NOT NULL DEFAULT 1,
    nonce BYTEA NOT NULL,
    authentication_tag BYTEA,
    wrapped_dek BYTEA NOT NULL,
    kms_key_reference VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_backups_owner_id ON backups(owner_id);
CREATE INDEX idx_backups_owner_created ON backups(owner_id, created_at DESC);
```

### 3. Retrieve Credentials
From **Project Settings** → **API**:
- Project URL (`SUPABASE_URL`)
- `anon` / `public` Key (used in frontend `.env`)
- `service_role` Secret Key (used in backend `.env` only — never share or commit)
- JWT Secret (used in backend `.env` to verify Supabase Auth tokens)

---

## AWS KMS & IAM Setup (Production)

### 1. Create KMS Key
```bash
aws kms create-key \
  --description "SecureVault AES Cloud Backup Customer Master Key" \
  --key-usage ENCRYPT_DECRYPT \
  --customer-master-key-spec SYMMETRIC_DEFAULT
```
Note the resulting Key ARN (`arn:aws:kms:region:account:key/key-id`).

### 2. Least-Privilege IAM Policy
Attach this policy to your Spring Boot deployment role:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowSecureVaultEnvelopeEncryption",
      "Effect": "Allow",
      "Action": [
        "kms:GenerateDataKey",
        "kms:Decrypt"
      ],
      "Resource": "arn:aws:kms:YOUR_REGION:YOUR_ACCOUNT:key/YOUR_KEY_ID"
    }
  ]
}
```

---

## Local Development Quickstart

### 1. Backend Configuration
Create `backend/.env` from `backend/.env.example`:
```bash
SPRING_PROFILES_ACTIVE=dev
DATABASE_URL=jdbc:postgresql://YOUR_SUPABASE_HOST:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=YOUR_DB_PASSWORD
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_SERVICE_ROLE_KEY=YOUR_SERVICE_ROLE_KEY
SUPABASE_JWT_SECRET=YOUR_JWT_SECRET
CORS_ALLOWED_ORIGINS=http://localhost:5173
LOCAL_KEK=3J8e6k8F9fQ0wR3tY5uI7oP9aS1dF3gH5jK7lZ9xC1v=
```

Run tests:
```bash
cd backend
mvn test
```

Run the backend server:
```bash
mvn spring-boot:run
```
Backend will start on `http://localhost:8080`.

### 2. Frontend Configuration
Create `frontend/.env` from `frontend/.env.example`:
```bash
VITE_SUPABASE_URL=https://YOUR_PROJECT.supabase.co
VITE_SUPABASE_ANON_KEY=YOUR_ANON_KEY
```

Install and run dev server:
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

---

## Backend REST API Reference

All protected endpoints require an `Authorization: Bearer <token>` header containing a verified Supabase Auth JWT.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Public service health check |
| `GET` | `/api/backups` | List user's backups (supports `search`, `page`, `size`, `sort`) |
| `GET` | `/api/backups/summary` | Dashboard summary metrics (total count, storage, recent) |
| `GET` | `/api/backups/{id}` | Detailed cryptographic metadata for a backup |
| `POST` | `/api/backups` | Multipart upload, encrypts with AES-256-GCM, saves to cloud |
| `GET` | `/api/backups/{id}/download-encrypted` | Download raw encrypted ciphertext (`.enc`) |
| `POST`| `/api/backups/{id}/restore` | Unwraps DEK via KMS, verifies tag, restores original file |
| `DELETE`| `/api/backups/{id}` | Deletes object from Supabase and marks deleted in DB |

---

## Verification & Automated Tests

All tests can be executed with Maven:
```bash
cd backend
mvn test
```

Test coverage includes:
1. `AesGcmEncryptionServiceTest`:
   - Valid text file encryption and decryption round-trip.
   - 64 KB binary file round-trip integrity.
   - Nonce freshness (independent nonces for identical inputs).
   - Tamper detection: single-bit modification of ciphertext throws `DecryptionException`.
   - Authentication tag tamper rejection.
   - Wrong key rejection.
   - In-memory key zeroing (`wipeKey`).
2. `LocalDevKeyManagementServiceTest`:
   - DEK generation (256 bits) and local KEK wrapping/unwrapping.
3. `BackupServiceTest`:
   - End-to-end upload and envelope encryption orchestration.
   - Prevention of cross-user access (unauthorized access rejection).
   - Maximum upload size rejection (> 25 MB).
   - Failure rollback (cleans up cloud storage if database insert fails).
