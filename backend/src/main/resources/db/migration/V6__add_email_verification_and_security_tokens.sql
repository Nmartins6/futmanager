-- V6__add_email_verification_and_security_tokens.sql
-- Adiciona suporte a verificação de e-mail, revogação de sessões e tokens de segurança criptográficos

-- 1. Novos atributos na tabela de usuários
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_version INT NOT NULL DEFAULT 1;

-- 2. Tabela de Tokens de Segurança (Confirmação de e-mail e Recuperação de senha)
CREATE TABLE IF NOT EXISTS security_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    token_type VARCHAR(50) NOT NULL CHECK (token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_security_token_hash UNIQUE (token_hash)
);

-- 3. Índices de alta performance e segurança
CREATE INDEX IF NOT EXISTS idx_security_tokens_user_type ON security_tokens(user_id, token_type);
CREATE INDEX IF NOT EXISTS idx_security_tokens_hash ON security_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_security_tokens_expires ON security_tokens(expires_at);
