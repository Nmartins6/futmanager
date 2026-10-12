# Manual do Usuário — FutManager SaaS
### Sistema de Gestão de Chamadas, Frequência e Alunos para Escolinhas de Futebol

**Versão do Sistema:** 1.0 (MVP Multi-Tenant SaaS)  
**Idioma:** Português do Brasil (pt-BR)  
**Ambiente:** Web Responsivo (Computador, Tablet e Celular)

---

## 1. Visão Geral do Sistema

O **FutManager** é uma plataforma SaaS projetada especificamente para escolinhas e academias de futebol. O sistema moderniza e simplifica o controle diário de frequência, o cadastro de atletas e responsáveis, a gestão de professores e a emissão de relatórios esportivos e pedagógicos.

### 👥 Perfis de Acesso

O sistema opera com controle rigoroso de permissões aplicado no backend:

| Perfil | Finalidade | Principais Permissões |
| :--- | :--- | :--- |
| **Administrador (Gestor)** | Direção, coordenação pedagógica e administrativa | Controle total: cadastra alunos, responsáveis, professores e turmas; consulta todas as chamadas e relatórios; importa e exporta dados; altera configurações da escolinha. |
| **Professor / Treinador** | Comissão técnica e treinadores de campo | Foco operacional: visualiza suas turmas autorizadas; executa chamadas pelo celular nas aulas do dia; consulta histórico e relatórios de suas turmas; salva rascunhos e finaliza chamadas. |

---

## 2. Acesso ao Sistema e Criando sua Escolinha (SaaS)

### 2.1. Como Criar uma Nova Escolinha (Auto-atendimento / Onboarding)
Qualquer novo gestor pode iniciar sua operação imediatamente:
1. Na tela de login, clique no botão em destaque: **"✨ Cadastrar Nova Escolinha (SaaS)"**.
2. Preencha o formulário:
   - **Nome da Escolinha:** Exemplo: *Academia de Futebol Camisa 10*.
   - **Nome do Administrador:** Nome completo do gestor responsável.
   - **E-mail de Acesso:** E-mail oficial que será o login do administrador.
   - **Senha de Acesso:** Senha segura do administrador (armazenada com hash BCrypt).
   - **Telefone / WhatsApp:** Contato para suporte e avisos.
   - **Plano Escolhido:**
     - *Starter:* Para escolinhas de pequeno porte (até 50 alunos).
     - *Pro:* Para escolinhas estruturadas (até 150 alunos).
     - *Enterprise:* Ilimitado.
   - **CNPJ e Endereço:** Dados cadastrais opcionais.
3. Clique em **"Criar Escolinha & Acessar"**. O sistema cria o ambiente isolado com 14 dias de teste e realiza o login automático instantâneo.

### 2.2. Login no Sistema
- Digite seu **e-mail** e sua **senha cadastrada** e clique em **"Entrar no Sistema"**.
- É possível alternar a visibilidade da senha clicando no ícone do olho.
- **Credenciais de Demonstração (Ambiente de Testes):**
  - **Senha padrão:** `admin123`
  - `admin@futmanager.com` (Gestor da Escolinha Craques do Amanhã)
  - `prof.roberto@futmanager.com` (Professor de campo da Craques do Amanhã)
  - `admin@leoes.com` (Gestor da Escolinha Leões da Colina)

### 2.3. Redefinição de Senha
- Clique em **"Esqueceu sua senha?"**.
- Informe seu e-mail cadastrado e digite sua nova senha desejada.
- Clique em **"Redefinir Senha"** para atualizar imediatamente suas credenciais.

---

## 3. Gestão de Turmas e Horários de Treino

No menu lateral, acesse **"Turmas & Horários"**.

### 3.1. Cadastrando uma Nova Turma
1. Clique no botão **"Nova Turma"** no topo da tela.
2. Preencha as informações gerais:
   - **Nome da Turma:** Exemplo: *Sub-11 Terça e Quinta*, *Iniciação Sub-7*.
   - **Categoria / Faixa Etária:** Exemplo: *Sub-7, Sub-9, Sub-11, Sub-13, Sub-15, Feminino*.
   - **Local de Treino:** Campo 1, Quadra Society, Ginásio Principal, etc.
   - **Capacidade Máxima:** Limite de vagas da turma (opcional).
   - **Período de Funcionamento:** Data de início e data de encerramento da temporada (opcionais).
3. **Professores Autorizados:**
   - Selecione os professores que darão treino para a turma.
   - Marque a opção **"Substituto"** caso o professor vá atuar apenas como apoio ou cobertura.
4. **Grade de Horários Semanais:**
   - É possível cadastrar múltiplos dias da semana, cada um com horários próprios (ex.: Terça das 14:00 às 15:30 e Quinta das 14:00 às 15:30).
   - Clique em **"Adicionar Horário"** para incluir novos dias.
5. Clique em **"Salvar Turma"**.

---

## 4. Cadastro de Professores

No menu lateral, acesse **"Professores"** (visível para Administradores).

### 4.1. Convidar / Cadastrar Novo Professor
O sistema **não possui cadastro público** para evitar acesso de terceiros não autorizados.
1. Clique em **"Convidar / Novo Professor"**.
2. Preencha o nome completo, e-mail e telefone do professor.
3. Clique em **"Enviar Convite / Cadastrar"**.
4. O professor é provisionado ativamente na sua escolinha e já pode ser vinculado às turmas.

### 4.2. Ativar ou Inativar Professores
- Na listagem de professores, clique no botão de status para ativar ou inativar um profissional.
- Professores inativados perdem o acesso imediato ao sistema, mas todo o histórico de chamadas que eles realizaram no passado permanece preservado para fins de auditoria.

---

## 5. Cadastro de Alunos e Responsáveis

No menu lateral, acesse **"Alunos"**.

### 5.1. Ficha Cadastral do Aluno
Clique em **"Novo Aluno"** e informe:
- **Nome Completo:** Nome do aluno (o sistema suporta homônimos sem mesclar cadastros).
- **Data de Nascimento:** O sistema **calcula a idade automaticamente** e valida se a data não é futura.
- **Data de Ingresso:** Data de matrícula na escolinha.
- **Foto:** Foto do aluno (opcional). Caso não possua foto, o sistema exibe um avatar acessível com as iniciais do aluno.
- **Observações Gerais:** Preferências, pé dominante, observações de comportamento.

### 5.2. Vínculo com Responsáveis
Um aluno pode ter múltiplos responsáveis vinculados:
- Nome do responsável, Parentesco (Pai, Mãe, Tio, Avô, etc.).
- Telefone / WhatsApp e E-mail.
- **Contato Principal:** Marque quem deve receber comunicações regulares.
- **Contato de Emergência:** Marque quem deve ser acionado prioritariamente em emergências.

### 5.3. Matrícula em Turmas
- Selecione uma ou mais turmas para o aluno.
- O sistema grava a data de entrada na turma. Caso o aluno mude de turma no futuro, basta registrar a saída: **o histórico de vínculos passados nunca é apagado**, garantindo integridade das chamadas antigas.

---

## 6. Agenda de Aulas e Execução da Chamada

O coração do FutManager é o módulo de aulas e chamadas em tempo real. No menu lateral, acesse **"Agenda & Aulas"**.

### 6.1. O que é uma Aula Concreta?
O sistema diferencia o *horário recorrente da turma* (ex.: toda terça às 14h) de uma *aula concreta*. Uma aula concreta possui data, horário de início/término, situação (Agendada, Realizada ou Cancelada) e tipo (Regular, Extra ou Remarcada).

### 6.2. Geração Automática e Idempotente de Aulas
1. Na tela de Aulas, clique em **"Gerar Aulas Futuras"**.
2. O sistema analisa as turmas ativas e projeta as aulas para os próximos 30 dias (horizonte configurável).
3. A operação é **100% idempotente**: ela nunca duplicará aulas já existentes no mesmo dia e horário.

### 6.3. Remarcações e Cancelamentos
- **Cancelar Aula:** Aulas canceladas recebem motivo e **não geram faltas** para os alunos.
- **Remarcar Aula:** Permite transferir uma aula não realizada para nova data e horário com motivo registrado.
- **Criar Aula Extra:** Permite agendar treinos extras ou amistosos no fim de semana.

---

## 7. Como Realizar a Chamada (Modo Celular do Treinador)

A tela de chamada foi projetada especialmente para ser confortável no celular dentro do campo de futebol:

1. Na tela inicial (**Painel Inicial**) ou na **Agenda**, localize a aula do dia e clique no botão verde **"Fazer Chamada"**.
2. **Lista de Alunos Elegíveis:** O sistema lista exclusivamente os alunos que estavam matriculados na turma na data da aula.
3. **Opções de Presença:** Para cada aluno, clique no botão desejado:
   - 🟢 **Presente (P):** Aluno participou do treino.
   - 🔴 **Ausente (A):** Aluno faltou sem aviso prévio.
   - 🟡 **Falta Justificada (J):** Aluno avisou que faltaria (viagem, prova escolar, indisposição). É possível digitar o motivo no campo de observação.
4. **Agilidade no Campo:**
   - Use o botão superior **"Marcar Todos como Presentes"** para adiantar o preenchimento, e depois apenas alterne quem faltou.
5. **Salvando Rascunho:**
   - Você pode clicar em **"Salvar Rascunho"** a qualquer momento durante o treino. Os dados ficam salvos em segurança sem fechar a aula.
6. **Finalizando a Chamada:**
   - Para finalizar, **todos os alunos devem estar classificados**.
   - Clique em **"Finalizar Chamada"**. A aula passa para a situação *Realizada* e os percentuais de frequência são atualizados no painel da escolinha.
7. **Proteção Contra Sobrescrita Simultânea:**
   - Se dois professores abrirem a chamada ao mesmo tempo, o sistema possui bloqueio de versão otimista (*Optimistic Locking*), impedindo que o trabalho de um sobrescreva silenciosamente o do outro.
8. **Auditoria Completa:**
   - O backend registra quem iniciou a chamada, quem finalizou, horário exato em UTC e qualquer alteração posterior feita com motivo obrigatório.

---

## 8. Estatísticas e Regras de Frequência

No menu lateral, acesse **"Estatísticas"**.

### 8.1. Regra Oficial de Cálculo da Frequência

$$\text{Frequência (\%)} = \left(\frac{\text{Total de Presenças}}{\text{Total de Aulas Contabilizadas}}\right) \times 100$$

- **Aulas Contabilizadas:** Soma de *Presenças* + *Ausências sem Justificativa* + *Faltas Justificadas*.
- **Falta Justificada:** É exibida separadamente para transparência, mas conta como ausência no cálculo esportivo.
- **Alunos em Múltiplas Turmas:** O cálculo consolidado do aluno é feito pela soma direta de presenças dividido pela soma de aulas elegíveis, **nunca pela média simples** dos percentuais das turmas.
- Se não houver aulas no período, o sistema exibe "Sem dados" com segurança contra divisão por zero.

### 8.2. Alertas de Baixa Frequência
- O sistema destaca automaticamente os alunos que estiverem com frequência abaixo da meta da escolinha (padrão: 75%).
- Isso permite que a coordenação entre em contato com os responsáveis via WhatsApp antes do aluno abandonar os treinos.

### 8.3. Comparativo entre Turmas e Evolução Mensal
- Gráficos e tabelas comparam a assiduidade entre categorias (ex.: Sub-11 vs Sub-13) e mostram a evolução mês a mês ao longo do ano letivo.

---

## 9. Relatórios Oficiais (PDF e Planilhas XLSX)

No menu lateral, acesse **"Relatórios"**.

Todos os relatórios gerados trazem o nome da escolinha, CNPJ, período selecionado, filtros aplicados e data/hora de emissão.

### Tipos de Relatórios Disponíveis:
1. **Frequência Individual do Aluno (PDF / XLSX):** Ficha completa do aluno com foto, dados dos responsáveis, detalhamento aula a aula e percentual por turma.
2. **Frequência por Turma (PDF / XLSX):** Lista de chamada da turma com percentual individual de cada aluno.
3. **Resumo Mensal (PDF / XLSX):** Consolidado de aulas realizadas, presenças e faltas da escolinha.
4. **Alunos com Baixa Frequência (PDF / XLSX):** Lista de alunos em situação crítica de assiduidade para ação da coordenação.
5. **Chamadas Realizadas por Professor (PDF):** Relatório de horas e aulas ministradas por cada treinador.
6. **Aulas com Chamada Pendente (PDF):** Lista de aulas realizadas cuja chamada ainda não foi finalizada.

---

## 10. Importação e Exportação de Dados

No menu lateral, acesse **"Importar / Exportar"** (Administrador).

### 10.1. Importação em Lote via Planilha (CSV ou Excel XLSX)
Permite cadastrar dezenas de alunos e responsáveis de uma só vez:
1. Clique em **"Baixar Modelo CSV"** ou **"Baixar Modelo Excel"**.
2. Preencha as colunas da planilha seguindo as instruções:
   - Nome do Aluno, Data de Nascimento (formato `DD/MM/AAAA` ou `AAAA-MM-DD`).
   - Nome do Responsável, Telefone e Parentesco.
   - Nome da Turma (opcional, para matrícula automática).
3. Selecione o arquivo e envie.
4. O sistema exibe uma **Pré-visualização Interativa**:
   - Valida linha por linha.
   - Aponta eventuais erros de digitação com destaque em vermelho.
   - Sinaliza possíveis homônimos ou alunos duplicados.
5. Se estiver tudo correto, clique em **"Confirmar e Efetivar Importação"**.
6. O processamento é **100% transacional**: se houver falha crítica, nada é gravado pela metade.

### 10.2. Exportação Segura
- Exporte listas completas de alunos ou chamadas em formato CSV ou Excel XLSX.
- As exportações contam com proteção nativa contra injeção de fórmulas (*Formula Injection Sanitization*).

---

## 11. Configurações da Escolinha

No menu lateral, acesse **"Configurações"**.

Permite que o Administrador personalize os parâmetros do seu negócio:
- **Dados Cadastrais:** Nome oficial da escolinha, CNPJ, telefone de contato e endereço físico.
- **Fuso Horário Padrão:** Configurado para `America/Sao_Paulo`.
- **Meta de Frequência (%):** Percentual mínimo desejado (padrão: 75%).
- **Horizonte de Aulas Futuras:** Quantos dias à frente o gerador deve projetar (padrão: 30 dias).
- **Gestão do Plano SaaS:** Exibe o plano atual (Starter ou Pro), limite máximo de alunos permitidos e data de término do período de avaliação.

---

## 12. Dúvidas Frequentes (FAQ)

**1. O professor pode ver dados de turmas de outros professores?**  
*Não. Quando o professor faz login, ele visualiza apenas as turmas nas quais foi autorizado como titular ou substituto.*

**2. O que acontece se a internet cair enquanto faço a chamada?**  
*O sistema informa claramente a falha de conexão e impede que você acredite que a chamada foi salva quando não foi. Assim que o sinal restabelecer, basta clicar em salvar rascunho.*

**3. Uma escolinha pode acessar dados de outra escolinha no sistema?**  
*De forma alguma. Cada escolinha possui isolamento estrito de banco de dados (`tenant_id`). Todas as buscas, chamadas e relatórios são filtrados pela sua própria instituição.*
