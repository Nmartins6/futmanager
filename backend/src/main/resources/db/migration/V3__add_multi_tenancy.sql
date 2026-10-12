-- V3__add_multi_tenancy.sql
-- Adiciona suporte Multi-Tenant nativo para arquitetura SaaS B2B

-- 1. Tabela de Escolinhas / Tenants
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(60) NOT NULL UNIQUE,
    cnpj VARCHAR(20),
    plan VARCHAR(30) NOT NULL DEFAULT 'STARTER',
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    max_students INT DEFAULT 50,
    phone VARCHAR(30),
    address VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Inserir Escolinha Padrão (Craques do Amanhã) para os dados já existentes
INSERT INTO tenants (id, name, slug, cnpj, plan, status, max_students, phone, address)
VALUES ('d0000000-0000-0000-0000-000000000001', 'Escolinha de Futebol Craques do Amanhã', 'craques-do-amanha', '12.345.678/0001-90', 'PRO', 'ATIVO', 150, '(11) 98765-4321', 'Rua dos Esportes, 100 - Centro Esportivo Municipal, São Paulo - SP');

-- 3. Inserir Segunda Escolinha de Demonstração (Leões da Colina) para validar o isolamento de dados
INSERT INTO tenants (id, name, slug, cnpj, plan, status, max_students, phone, address)
VALUES ('d0000000-0000-0000-0000-000000000002', 'Academia de Futebol Leões da Colina', 'leoes-da-colina', '98.765.432/0001-10', 'STARTER', 'ATIVO', 50, '(21) 99887-6655', 'Av. Brasil, 500, Rio de Janeiro - RJ');

-- 4. Adicionar tenant_id em users
ALTER TABLE users ADD COLUMN tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;
UPDATE users SET tenant_id = 'd0000000-0000-0000-0000-000000000001';
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;

-- Usuário Administrador da segunda escolinha
INSERT INTO users (id, email, full_name, role, status, phone, tenant_id)
VALUES ('66666666-6666-6666-6666-666666666666', 'admin@leoes.com', 'Rodrigo Mendes (Coordenação Leões)', 'ADMINISTRADOR', 'ATIVO', '(21) 99887-1122', 'd0000000-0000-0000-0000-000000000002');

-- 5. Adicionar tenant_id em students
ALTER TABLE students ADD COLUMN tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;
UPDATE students SET tenant_id = 'd0000000-0000-0000-0000-000000000001';
ALTER TABLE students ALTER COLUMN tenant_id SET NOT NULL;

-- 6. Adicionar tenant_id em guardians
ALTER TABLE guardians ADD COLUMN tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;
UPDATE guardians SET tenant_id = 'd0000000-0000-0000-0000-000000000001';
ALTER TABLE guardians ALTER COLUMN tenant_id SET NOT NULL;

-- 7. Adicionar tenant_id em school_classes
ALTER TABLE school_classes ADD COLUMN tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;
UPDATE school_classes SET tenant_id = 'd0000000-0000-0000-0000-000000000001';
ALTER TABLE school_classes ALTER COLUMN tenant_id SET NOT NULL;

-- 8. Adicionar tenant_id em school_configs
ALTER TABLE school_configs ADD COLUMN tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;
UPDATE school_configs SET tenant_id = 'd0000000-0000-0000-0000-000000000001';
ALTER TABLE school_configs ALTER COLUMN tenant_id SET NOT NULL;

-- Ajustar chave única de configurações para ser por tenant
ALTER TABLE school_configs DROP CONSTRAINT IF EXISTS school_configs_config_key_key;
ALTER TABLE school_configs ADD CONSTRAINT uq_school_configs_tenant_key UNIQUE (tenant_id, config_key);
ALTER TABLE school_configs ALTER COLUMN id SET DEFAULT gen_random_uuid()::varchar;

-- Inserir configurações da segunda escolinha
INSERT INTO school_configs (id, tenant_id, config_key, config_value, description) VALUES
('c0000000-0000-0000-0000-000000000021', 'd0000000-0000-0000-0000-000000000002', 'school_name', 'Academia de Futebol Leões da Colina', 'Nome da escolinha'),
('c0000000-0000-0000-0000-000000000022', 'd0000000-0000-0000-0000-000000000002', 'cnpj', '98.765.432/0001-10', 'CNPJ da instituição'),
('c0000000-0000-0000-0000-000000000023', 'd0000000-0000-0000-0000-000000000002', 'phone', '(21) 99887-6655', 'Telefones de contato'),
('c0000000-0000-0000-0000-000000000024', 'd0000000-0000-0000-0000-000000000002', 'address', 'Av. Brasil, 500, Rio de Janeiro - RJ', 'Endereço da escolinha'),
('c0000000-0000-0000-0000-000000000025', 'd0000000-0000-0000-0000-000000000002', 'timezone', 'America/Sao_Paulo', 'Fuso horário padrão'),
('c0000000-0000-0000-0000-000000000026', 'd0000000-0000-0000-0000-000000000002', 'lesson_horizon_days', '30', 'Horizonte de dias futuros'),
('c0000000-0000-0000-0000-000000000027', 'd0000000-0000-0000-0000-000000000002', 'low_frequency_threshold', '75.0', 'Percentual mínimo de frequência');

-- 9. Índices de alta performance para particionamento de consultas por tenant
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_students_tenant_id ON students(tenant_id);
CREATE INDEX idx_guardians_tenant_id ON guardians(tenant_id);
CREATE INDEX idx_school_classes_tenant_id ON school_classes(tenant_id);
CREATE INDEX idx_school_configs_tenant_id ON school_configs(tenant_id);
