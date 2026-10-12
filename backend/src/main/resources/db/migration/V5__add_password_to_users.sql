-- V5__add_password_to_users.sql
-- Adiciona suporte a senha com hash seguro (BCrypt) para todos os usuários

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- Define a senha padrão 'admin123' ($2a$12$Q2p6RaF06kxN.lqVFICY7OyCu33D4PFKjr9I/YBD3gc1PZb1YKcIq) para usuários existentes
UPDATE users
SET password_hash = '$2a$12$Q2p6RaF06kxN.lqVFICY7OyCu33D4PFKjr9I/YBD3gc1PZb1YKcIq'
WHERE password_hash IS NULL;

-- Torna a coluna obrigatória
ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;
