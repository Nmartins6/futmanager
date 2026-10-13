# 🗺️ Guia de Deploy Descomplicado — Passo a Passo para Iniciantes
### Como colocar o FutManager no ar na internet de forma 100% gratuita

Este guia foi feito para você que **nunca fez um deploy** e não tem familiaridade com comandos técnicos. Siga cada etapa com calma — tudo o que você precisa fazer é abrir alguns sites gratuitos, clicar em botões e colar as informações indicadas.

---

## 📍 1. Onde está o projeto no seu computador?

O sistema foi todo construído e está salvo nesta pasta do seu computador:

> **Caminho da pasta:** `C:\Workspace\futmanager`

### Como abrir essa pasta agora mesmo:
1. No teclado do seu computador, aperte as teclas **Windows (a tecla com o símbolo do Windows) + R** juntas.
2. Na caixinha que vai abrir no canto inferior esquerdo (Executar), digite ou cole:
   ```text
   C:\Workspace\futmanager
   ```
3. Aperte **Enter** (ou clique em **OK**).
4. O Explorador de Arquivos do Windows vai abrir exibindo todos os arquivos do sistema! Lá dentro você verá pastas como `backend`, `frontend` e manuais de uso.

---

## 🌐 2. Entendendo o Deploy (As 4 Peças Gratuitas)

Para que o sistema funcione na internet e os professores possam abrir no celular deles pelo WhatsApp, precisamos de 4 serviços gratuitos:

1. **GitHub:** O cofre na nuvem onde o código fica guardado.
2. **Supabase:** O banco de dados gratuito na nuvem (PostgreSQL) onde os alunos, chamadas e turmas ficam gravados.
3. **Render:** O servidor gratuito na nuvem que roda a inteligência do sistema (Java 21).
4. **Vercel:** O site gratuito na nuvem que exibe as telas bonitas no celular ou computador com link seguro (https).

Nenhum desses serviços pede cartão de crédito nos planos gratuitos.

---

## 🚀 3. Roteiro Passo a Passo

---

### ETAPA 1: Guardar o código no GitHub

O GitHub é onde o código do sistema ficará guardado na nuvem.

1. Acesse o site **[github.com](https://github.com)** e crie uma conta gratuita (se já tiver uma, faça login).
2. No canto superior direito, clique no botão **`+`** e selecione **"New repository"** (Novo repositório).
3. Preencha os campos:
   - **Repository name:** digite `futmanager`
   - **Public:** deixe marcado como **Public** (ou Private, se preferir)
   - **⚠️ MUITO IMPORTANTE:** Deixe desmarcadas as opções *"Add a README file"*, *"Add .gitignore"* e *"Choose a license"*. O repositório deve ser criado **totalmente vazio**.
4. Clique no botão verde **"Create repository"**.
5. Na tela que abrir, copie o link que termina com `.git`. Ele será algo como:
   ```text
   https://github.com/SEU-USUARIO/futmanager.git
   ```

#### Agora envie os arquivos do seu computador:
1. Abra o terminal (Git Bash, WSL Linux ou Prompt de Comando) na pasta do projeto:
   ```bash
   git push -u origin main
   ```
2. Se o GitHub solicitar autenticação no navegador ou credenciais, confirme o acesso.
3. Pronto! Em instantes todos os arquivos do sistema estarão salvos no seu GitHub.

---

### ETAPA 2: Criar o Banco de Dados no Supabase

O Supabase será o "disco rígido" do sistema na nuvem (PostgreSQL gratuito).

1. Acesse **[supabase.com](https://supabase.com)** e clique em **"Start your project"** (você pode entrar usando sua conta do GitHub em 1 clique).
2. Clique no botão verde **"New Project"**.
3. Preencha:
   - **Name:** `futmanager-db`
   - **Database Password:** Escolha uma senha forte e **anote num bloco de notas** (exemplo: `FutManager2026!`).
   - **Region:** Escolha `South America (São Paulo)` para ficar bem rápido no Brasil (ou `East US`).
   - **Pricing Plan:** Free ($0/month).
4. Clique em **"Create new project"** e espere 1 a 2 minutos até ele terminar de criar.
5. Quando o painel carregar:
   - No menu lateral esquerdo, clique no ícone de engrenagem no final da barra (**Project Settings**).
   - Clique em **Database**.
   - Role a página até encontrar a seção **Connection string**.
   - Clique na aba **JDBC** (ou Connection pooling).
   - O link terá esse formato:
     ```text
     jdbc:postgresql://db.xxxxxxxxxxxxxxxxxxxx.supabase.co:5432/postgres?sslmode=require
     ```
   - Guarde esse endereço no seu bloco de notas junto com a senha que você criou.

> Você **não precisa rodar nenhum comando SQL** no banco! Assim que o sistema ligar, ele cria todas as tabelas, configurações e usuários de teste sozinho via migrações automáticas.

---

### ETAPA 3: Colocar o Backend no ar no Render

O Render é o servidor que executa a inteligência do sistema (Java 21).

1. Acesse **[render.com](https://render.com)** e crie sua conta (pode usar o botão *"Sign in with GitHub"*).
2. No painel principal, clique no botão azul **"New +"** no canto superior e escolha **"Web Service"**.
3. Selecione a opção **"Build and deploy from a Git repository"** e clique em **Next**.
4. Encontre o seu repositório `futmanager` e clique em **"Connect"**.
5. Na tela de configuração, preencha:
   - **Name:** `futmanager-api`
   - **Region:** `Ohio (US East)` ou `Frankfurt`
   - **Branch:** `main`
   - **Language / Runtime:** escolha **Docker**
   - **Dockerfile Path:** `./Dockerfile.backend`
   - **Instance Type:** escolha **Free**
6. Role para baixo até **"Environment Variables"** (Variáveis de Ambiente) e clique em **"Add Environment Variable"** para adicionar estas variáveis:

   | Key (Nome da Variável) | Value (Valor) |
   | :--- | :--- |
   | `SPRING_DATASOURCE_URL` | Cole a URL JDBC do Supabase (ex: `jdbc:postgresql://db.xxxx.supabase.co:5432/postgres?sslmode=require`) |
   | `SPRING_DATASOURCE_USERNAME` | `postgres` |
   | `SPRING_DATASOURCE_PASSWORD` | A senha que você escolheu no Supabase |
   | `JWT_SECRET` | `UmaChaveSuperSecretaDePeloMenos64CaracteresParaGarantirSegurancaDoFutManager2026` |

7. Clique no botão azul no final da página: **"Deploy Web Service"**.
8. O Render vai compilar e iniciar o sistema. Quando aparecer **"Live"** com uma bolinha verde, copie a URL gerada no topo da página. Ela será parecida com:
   ```text
   https://futmanager-api.onrender.com
   ```

---

### ETAPA 4: Colocar as Telas no ar na Vercel (O Site que os usuários acessam)

A Vercel é onde fica o site que os professores e diretores abrem no celular ou computador.

1. Acesse **[vercel.com](https://vercel.com)** e crie sua conta clicando em **"Continue with GitHub"**.
2. No painel, clique em **"Add New..."** > **"Project"**.
3. Encontre o repositório `futmanager` na lista e clique no botão **"Import"**.
4. Na tela que abrir:
   - **Project Name:** `futmanager`
   - **Framework Preset:** Vite
   - **Root Directory:** Clique em **Edit** e selecione a pasta **`frontend`** (clique em Continue).
5. Abra a seção **"Environment Variables"** e adicione:
   - **Key:** `VITE_API_URL`
   - **Value:** Cole a URL do backend gerada no Render com `/api` no final:
     ```text
     https://futmanager-api.onrender.com/api
     ```
6. Clique no botão azul **"Deploy"**.
7. Em cerca de 1 minuto vai chover confetes na tela com a mensagem **"Congratulations!"**!
8. Clique no botão **"Continue to Dashboard"** ou no link exibido (exemplo: `https://futmanager.vercel.app`).

---

## 🎉 4. Seu Sistema Está Oficialmente no Ar!

Abra o link da Vercel no seu computador ou no celular de qualquer professor.

### 🔑 Como testar o login no sistema online:
- **E-mail:** `admin@futmanager.com`
- **Senha:** `admin123`
*(Ou clique em "✨ Cadastrar Nova Escolinha" para cadastrar sua escolinha oficial do zero!)*

### 📱 Para usar como aplicativo no celular:
1. Abra o link no Google Chrome (Android) ou Safari (iPhone).
2. Clique no menu de 3 pontinhos ou no botão de compartilhar.
3. Escolha **"Adicionar à tela de início"**.
4. O FutManager passará a abrir no celular exatamente como um aplicativo nativo!
