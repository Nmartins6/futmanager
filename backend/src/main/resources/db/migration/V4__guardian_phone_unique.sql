-- V4__guardian_phone_unique.sql
-- Garante telefone obrigatório e chave de unicidade por tenant para responsáveis

-- 1. Tratar registros legados para garantir que telefone não seja nulo ou vazio
UPDATE guardians
SET phone = '0000000000'
WHERE phone IS NULL OR TRIM(phone) = '';

-- 2. Consolidar/Deduplicar responsáveis legados com mesmo telefone no mesmo tenant
DO $$
DECLARE
    rec RECORD;
    keeper_id UUID;
BEGIN
    FOR rec IN 
        SELECT tenant_id, phone
        FROM guardians
        GROUP BY tenant_id, phone
        HAVING COUNT(*) > 1
    LOOP
        -- Escolhe o registro mais antigo como o oficial (keeper)
        SELECT id INTO keeper_id
        FROM guardians
        WHERE tenant_id = rec.tenant_id AND phone = rec.phone
        ORDER BY created_at ASC
        LIMIT 1;

        -- Reatribui vínculos de alunos para o responsável mantido
        UPDATE student_guardians sg
        SET guardian_id = keeper_id
        WHERE guardian_id IN (
            SELECT id FROM guardians
            WHERE tenant_id = rec.tenant_id AND phone = rec.phone AND id != keeper_id
        )
        AND NOT EXISTS (
            SELECT 1 FROM student_guardians existing
            WHERE existing.student_id = sg.student_id AND existing.guardian_id = keeper_id
        );

        -- Remove eventuais vínculos duplicados órfãos
        DELETE FROM student_guardians
        WHERE guardian_id IN (
            SELECT id FROM guardians
            WHERE tenant_id = rec.tenant_id AND phone = rec.phone AND id != keeper_id
        );

        -- Exclui os responsáveis duplicados
        DELETE FROM guardians
        WHERE tenant_id = rec.tenant_id AND phone = rec.phone AND id != keeper_id;
    END LOOP;
END $$;

-- 3. Garantir NOT NULL na coluna phone
ALTER TABLE guardians
ALTER COLUMN phone SET NOT NULL;

-- 4. Adicionar constraint UNIQUE por tenant e telefone
ALTER TABLE guardians
DROP CONSTRAINT IF EXISTS uq_guardians_tenant_phone;

ALTER TABLE guardians
ADD CONSTRAINT uq_guardians_tenant_phone UNIQUE (tenant_id, phone);
