-- V1: Create backups table
CREATE TABLE IF NOT EXISTS backups (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    object_path     VARCHAR(1000) NOT NULL UNIQUE,
    original_size_bytes BIGINT NOT NULL,
    encrypted_size_bytes BIGINT,
    mime_type       VARCHAR(255) NOT NULL,
    encryption_algorithm VARCHAR(50) NOT NULL DEFAULT 'AES/GCM/NoPadding',
    format_version  INTEGER NOT NULL DEFAULT 1,
    nonce           BYTEA NOT NULL,
    authentication_tag BYTEA,
    wrapped_dek     BYTEA NOT NULL,
    kms_key_reference VARCHAR(500) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for owner queries (most common access pattern)
CREATE INDEX idx_backups_owner_id ON backups(owner_id);
CREATE INDEX idx_backups_owner_created ON backups(owner_id, created_at DESC);
CREATE INDEX idx_backups_status ON backups(status);
