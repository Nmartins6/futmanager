-- V2__seed_demo_data.sql
-- Dados de demonstracao realistas para a Escolinha de Futebol

-- 1. Usuarios
INSERT INTO users (id, email, full_name, role, status, phone) VALUES
('11111111-1111-1111-1111-111111111111', 'admin@futmanager.com', 'Carlos Silva (Coordenação)', 'ADMINISTRADOR', 'ATIVO', '(11) 98765-4321'),
('22222222-2222-2222-2222-222222222222', 'prof.roberto@futmanager.com', 'Roberto Rocha', 'PROFESSOR', 'ATIVO', '(11) 97654-3210'),
('33333333-3333-3333-3333-333333333333', 'prof.marcos@futmanager.com', 'Marcos Assunção', 'PROFESSOR', 'ATIVO', '(11) 96543-2109'),
('44444444-4444-4444-4444-444444444444', 'prof.aline@futmanager.com', 'Aline Pellegrino', 'PROFESSOR', 'ATIVO', '(11) 95432-1098'),
('55555555-5555-5555-5555-555555555555', 'prof.inativo@futmanager.com', 'Lucas Desativado', 'PROFESSOR', 'INATIVO', '(11) 94321-0987');

-- 2. Turmas
INSERT INTO school_classes (id, name, category, location, max_capacity, status, start_date, end_date) VALUES
('c1111111-1111-1111-1111-111111111111', 'Sub-9 Manhã (Fundamentos)', 'Sub-9', 'Campo Sintético 1', 20, 'ATIVO', '2026-01-15', NULL),
('c2222222-2222-2222-2222-222222222222', 'Sub-11 Tarde (Competição)', 'Sub-11', 'Campo Oficial', 25, 'ATIVO', '2026-01-15', NULL),
('c3333333-3333-3333-3333-333333333333', 'Sub-13 Transição', 'Sub-13', 'Campo Sintético 2', 22, 'ATIVO', '2026-02-01', NULL),
('c4444444-4444-4444-4444-444444444444', 'Iniciação Sub-7 Sábado', 'Sub-7', 'Quadra Coberta', 15, 'ATIVO', '2026-03-01', NULL);

-- 3. Professores Vinculados as Turmas
-- Sub-9: Roberto (titular) e Aline (substituta)
INSERT INTO class_teachers (class_id, user_id, is_substitute) VALUES
('c1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', FALSE),
('c1111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', TRUE);

-- Sub-11: Roberto e Marcos
INSERT INTO class_teachers (class_id, user_id, is_substitute) VALUES
('c2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', FALSE),
('c2222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', FALSE);

-- Sub-13: Marcos
INSERT INTO class_teachers (class_id, user_id, is_substitute) VALUES
('c3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', FALSE);

-- Sub-7: Aline
INSERT INTO class_teachers (class_id, user_id, is_substitute) VALUES
('c4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', FALSE);

-- 4. Horarios Recorrentes
-- Sub-9: Segunda e Quarta (08:30 - 10:00)
INSERT INTO class_schedules (class_id, day_of_week, start_time, end_time) VALUES
('c1111111-1111-1111-1111-111111111111', 'MONDAY', '08:30:00', '10:00:00'),
('c1111111-1111-1111-1111-111111111111', 'WEDNESDAY', '08:30:00', '10:00:00');

-- Sub-11: Terca e Quinta (14:30 - 16:00)
INSERT INTO class_schedules (class_id, day_of_week, start_time, end_time) VALUES
('c2222222-2222-2222-2222-222222222222', 'TUESDAY', '14:30:00', '16:00:00'),
('c2222222-2222-2222-2222-222222222222', 'THURSDAY', '14:30:00', '16:00:00');

-- Sub-13: Segunda e Sexta (18:00 - 19:30)
INSERT INTO class_schedules (class_id, day_of_week, start_time, end_time) VALUES
('c3333333-3333-3333-3333-333333333333', 'MONDAY', '18:00:00', '19:30:00'),
('c3333333-3333-3333-3333-333333333333', 'FRIDAY', '18:00:00', '19:30:00');

-- Sub-7: Sabado (09:00 - 10:30)
INSERT INTO class_schedules (class_id, day_of_week, start_time, end_time) VALUES
('c4444444-4444-4444-4444-444444444444', 'SATURDAY', '09:00:00', '10:30:00');

-- 5. Responsaveis (prefixo b = hex valido)
INSERT INTO guardians (id, full_name, phone, email) VALUES
('b1111111-1111-1111-1111-111111111111', 'Patrícia Mendes', '(11) 98111-2233', 'patricia.mendes@email.com'),
('b2222222-2222-2222-2222-222222222222', 'Jorge Mendes', '(11) 98111-4455', 'jorge.mendes@email.com'),
('b3333333-3333-3333-3333-333333333333', 'Mariana Alencar', '(11) 97222-3344', 'mariana.alencar@email.com'),
('b4444444-4444-4444-4444-444444444444', 'Eduardo Santos', '(11) 96333-5566', 'eduardo.santos@email.com'),
('b5555555-5555-5555-5555-555555555555', 'Camila Ferreira', '(11) 95444-6677', 'camila.ferreira@email.com'),
('b6666666-6666-6666-6666-666666666666', 'Renato Lima', '(11) 94555-7788', 'renato.lima@email.com');

-- 6. Alunos (prefixo e = hex valido)
INSERT INTO students (id, full_name, birth_date, status, enrollment_date, notes) VALUES
('e1111111-1111-1111-1111-111111111111', 'Gabriel Mendes', '2017-04-12', 'ATIVO', '2026-01-20', 'Canhoto, bom passe. Usa óculos esportivo.'),
('e2222222-2222-2222-2222-222222222222', 'Enzo Alencar', '2017-09-25', 'ATIVO', '2026-01-20', 'Gosta de jogar de volante.'),
('e3333333-3333-3333-3333-333333333333', 'Matheus Santos', '2015-06-18', 'ATIVO', '2026-01-22', 'Atacante veloz.'),
('e4444444-4444-4444-4444-444444444444', 'Lucas Ferreira', '2015-11-03', 'ATIVO', '2026-02-01', 'Goleiro titular Sub-11.'),
('e5555555-5555-5555-5555-555555555555', 'Felipe Lima', '2013-03-08', 'ATIVO', '2026-02-10', 'Capitão da turma Sub-13.'),
('e6666666-6666-6666-6666-666666666666', 'Gabriel Mendes', '2015-08-30', 'ATIVO', '2026-02-15', 'Homônimo do aluno da Sub-9. Joga na Sub-11.'),
('e7777777-7777-7777-7777-777777777777', 'Pedro Henrique Dias', '2019-05-14', 'ATIVO', '2026-03-05', 'Iniciação infantil.'),
('e8888888-8888-8888-8888-888888888888', 'Arthur Oliveira', '2017-02-19', 'INATIVO', '2026-01-20', 'Mudou de cidade em Agosto.');

-- 7. Vinculos Aluno-Responsavel
INSERT INTO student_guardians (student_id, guardian_id, relationship, is_main_contact, is_emergency_contact) VALUES
('e1111111-1111-1111-1111-111111111111', 'b1111111-1111-1111-1111-111111111111', 'Mãe', TRUE, TRUE),
('e1111111-1111-1111-1111-111111111111', 'b2222222-2222-2222-2222-222222222222', 'Pai', FALSE, TRUE),
('e2222222-2222-2222-2222-222222222222', 'b3333333-3333-3333-3333-333333333333', 'Mãe', TRUE, TRUE),
('e3333333-3333-3333-3333-333333333333', 'b4444444-4444-4444-4444-444444444444', 'Pai', TRUE, TRUE),
('e4444444-4444-4444-4444-444444444444', 'b5555555-5555-5555-5555-555555555555', 'Mãe', TRUE, TRUE),
('e5555555-5555-5555-5555-555555555555', 'b6666666-6666-6666-6666-666666666666', 'Pai', TRUE, TRUE),
('e6666666-6666-6666-6666-666666666666', 'b4444444-4444-4444-4444-444444444444', 'Tio', TRUE, FALSE),
('e7777777-7777-7777-7777-777777777777', 'b5555555-5555-5555-5555-555555555555', 'Mãe', TRUE, TRUE);

-- 8. Matriculas em Turmas
-- Sub-9: Gabriel Mendes (1), Enzo Alencar, Arthur Oliveira (saiu em 2026-08-31)
INSERT INTO class_enrollments (student_id, class_id, entry_date, exit_date, status) VALUES
('e1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', '2026-01-20', NULL, 'ATIVO'),
('e2222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111', '2026-01-20', NULL, 'ATIVO'),
('e8888888-8888-8888-8888-888888888888', 'c1111111-1111-1111-1111-111111111111', '2026-01-20', '2026-08-31', 'TRANSFERIDO');

-- Sub-11: Matheus Santos, Lucas Ferreira, Gabriel Mendes (homonimo 2), e Enzo Alencar tambem faz treino avancado na Sub-11
INSERT INTO class_enrollments (student_id, class_id, entry_date, exit_date, status) VALUES
('e3333333-3333-3333-3333-333333333333', 'c2222222-2222-2222-2222-222222222222', '2026-01-22', NULL, 'ATIVO'),
('e4444444-4444-4444-4444-444444444444', 'c2222222-2222-2222-2222-222222222222', '2026-02-01', NULL, 'ATIVO'),
('e6666666-6666-6666-6666-666666666666', 'c2222222-2222-2222-2222-222222222222', '2026-02-15', NULL, 'ATIVO'),
('e2222222-2222-2222-2222-222222222222', 'c2222222-2222-2222-2222-222222222222', '2026-03-01', NULL, 'ATIVO');

-- Sub-13: Felipe Lima
INSERT INTO class_enrollments (student_id, class_id, entry_date, exit_date, status) VALUES
('e5555555-5555-5555-5555-555555555555', 'c3333333-3333-3333-3333-333333333333', '2026-02-10', NULL, 'ATIVO');

-- Sub-7: Pedro Henrique Dias
INSERT INTO class_enrollments (student_id, class_id, entry_date, exit_date, status) VALUES
('e7777777-7777-7777-7777-777777777777', 'c4444444-4444-4444-4444-444444444444', '2026-03-05', NULL, 'ATIVO');

-- 9. Aulas Recentes e Chamadas Historicas (prefixo f = hex valido)
-- Aula 1: Sub-9 em 2026-10-05 (Segunda-feira) - Realizada e Finalizada
INSERT INTO lessons (id, class_id, date, start_time, end_time, status, type, notes) VALUES
('f1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', '2026-10-05', '08:30:00', '10:00:00', 'REALIZADA', 'REGULAR', 'Treino de controle de bola e passe curto');

INSERT INTO attendances (id, lesson_id, status, started_by_user_id, started_at, finalized_by_user_id, finalized_at, version) VALUES
('a1111111-1111-1111-1111-111111111111', 'f1111111-1111-1111-1111-111111111111', 'FINALIZADA', '22222222-2222-2222-2222-222222222222', '2026-10-05 08:35:00-03', '22222222-2222-2222-2222-222222222222', '2026-10-05 10:05:00-03', 1);

INSERT INTO attendance_items (attendance_id, student_id, status, notes) VALUES
('a1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', 'PRESENTE', NULL),
('a1111111-1111-1111-1111-111111111111', 'e2222222-2222-2222-2222-222222222222', 'PRESENTE', NULL);

INSERT INTO attendance_audit_logs (attendance_id, action, changed_by_user_id, reason, details, created_at) VALUES
('a1111111-1111-1111-1111-111111111111', 'FINALIZADA', '22222222-2222-2222-2222-222222222222', 'Chamada finalizada normalmente', 'Todos os 2 alunos presentes', '2026-10-05 10:05:00-03');

-- Aula 2: Sub-9 em 2026-10-07 (Quarta-feira) - Realizada e Finalizada com Falta Justificada
INSERT INTO lessons (id, class_id, date, start_time, end_time, status, type, notes) VALUES
('f2222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111', '2026-10-07', '08:30:00', '10:00:00', 'REALIZADA', 'REGULAR', 'Treino tático posicional');

INSERT INTO attendances (id, lesson_id, status, started_by_user_id, started_at, finalized_by_user_id, finalized_at, version) VALUES
('a2222222-2222-2222-2222-222222222222', 'f2222222-2222-2222-2222-222222222222', 'FINALIZADA', '22222222-2222-2222-2222-222222222222', '2026-10-07 08:40:00-03', '22222222-2222-2222-2222-222222222222', '2026-10-07 10:02:00-03', 1);

INSERT INTO attendance_items (attendance_id, student_id, status, notes) VALUES
('a2222222-2222-2222-2222-222222222222', 'e1111111-1111-1111-1111-111111111111', 'PRESENTE', NULL),
('a2222222-2222-2222-2222-222222222222', 'e2222222-2222-2222-2222-222222222222', 'FALTA_JUSTIFICADA', 'Consulta ao ortopedista comunicada pela mãe');

INSERT INTO attendance_audit_logs (attendance_id, action, changed_by_user_id, reason, details, created_at) VALUES
('a2222222-2222-2222-2222-222222222222', 'FINALIZADA', '22222222-2222-2222-2222-222222222222', 'Chamada finalizada', '1 presente, 1 falta justificada', '2026-10-07 10:02:00-03');

-- Aula 3: Sub-11 em 2026-10-06 (Terca-feira) - Realizada e Finalizada com ausencia sem justificativa
INSERT INTO lessons (id, class_id, date, start_time, end_time, status, type, notes) VALUES
('f3333333-3333-3333-3333-333333333333', 'c2222222-2222-2222-2222-222222222222', '2026-10-06', '14:30:00', '16:00:00', 'REALIZADA', 'REGULAR', 'Treino de finalização');

INSERT INTO attendances (id, lesson_id, status, started_by_user_id, started_at, finalized_by_user_id, finalized_at, version) VALUES
('a3333333-3333-3333-3333-333333333333', 'f3333333-3333-3333-3333-333333333333', 'FINALIZADA', '33333333-3333-3333-3333-333333333333', '2026-10-06 14:35:00-03', '33333333-3333-3333-3333-333333333333', '2026-10-06 16:10:00-03', 1);

INSERT INTO attendance_items (attendance_id, student_id, status, notes) VALUES
('a3333333-3333-3333-3333-333333333333', 'e3333333-3333-3333-3333-333333333333', 'PRESENTE', NULL),
('a3333333-3333-3333-3333-333333333333', 'e4444444-4444-4444-4444-444444444444', 'PRESENTE', NULL),
('a3333333-3333-3333-3333-333333333333', 'e6666666-6666-6666-6666-666666666666', 'AUSENTE', 'Sem justificativa informada'),
('a3333333-3333-3333-3333-333333333333', 'e2222222-2222-2222-2222-222222222222', 'PRESENTE', NULL);

-- Aula 4: Sub-11 em 2026-10-08 (Quinta-feira) - Realizada, mas chamada pendente (Demonstra regra de pendencia no dashboard!)
INSERT INTO lessons (id, class_id, date, start_time, end_time, status, type, notes) VALUES
('f4444444-4444-4444-4444-444444444444', 'c2222222-2222-2222-2222-222222222222', '2026-10-08', '14:30:00', '16:00:00', 'REALIZADA', 'REGULAR', 'Treino de bolas paradas');

-- Aula 5: Sub-9 em 2026-10-09 (Sexta-feira) - CANCELADA por chuva forte (nao deve gerar faltas!)
INSERT INTO lessons (id, class_id, date, start_time, end_time, status, type, notes) VALUES
('f5555555-5555-5555-5555-555555555555', 'c1111111-1111-1111-1111-111111111111', '2026-10-09', '08:30:00', '10:00:00', 'CANCELADA', 'EXTRA', 'Cancelada preventivamente devido a tempestade');

-- Aula 6: Aula agendada para hoje / proxima
INSERT INTO lessons (id, class_id, date, start_time, end_time, status, type, notes) VALUES
('f6666666-6666-6666-6666-666666666666', 'c1111111-1111-1111-1111-111111111111', CURRENT_DATE, '08:30:00', '10:00:00', 'AGENDADA', 'REGULAR', 'Treino técnico matutino');

-- 10. Configuracoes da Escolinha
INSERT INTO school_configs (id, config_key, config_value, description) VALUES
('1', 'school_name', 'Escolinha de Futebol Craques do Amanhã', 'Nome da escolinha para relatórios e cabeçalhos'),
('2', 'cnpj', '12.345.678/0001-90', 'CNPJ da instituição'),
('3', 'phone', '(11) 3456-7890 / (11) 98765-4321', 'Telefones de contato'),
('4', 'address', 'Rua dos Esportes, 100 - Centro Esportivo Municipal, São Paulo - SP', 'Endereço da escolinha'),
('5', 'timezone', 'America/Sao_Paulo', 'Fuso horário padrão'),
('6', 'lesson_horizon_days', '30', 'Horizonte de dias futuros para geração de aulas automáticas'),
('7', 'low_frequency_threshold', '75.0', 'Percentual mínimo de frequência antes de sinalizar alerta');
