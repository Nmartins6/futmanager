# Guia Definitivo: Deploy 100% Gratuito para Testes — FutManager SaaS

Este guia ensina o passo a passo exato para colocar o **FutManager** no ar gratuitamente, com banco de dados em nuvem, backend em execução contínua com HTTPS e frontend responsivo acessível pelo celular ou computador de qualquer lugar do mundo.

---

## 🏗️ Arquitetura de Custo R$ 0,00 (Free Tier)

| Camada | Provedor Gratuito | Especificações do Plano Free | O que hospeda |
| :--- | :--- | :--- | :--- |
| **Banco de Dados** | [Supabase](https://supabase.com) | PostgreSQL 15/16 dedicado, 500 MB de armazenamento, SSL nativo, Backups diários | Banco de dados relacional e migrações Flyway |
| **Backend API** | [Render](https://render.com) | Web Service Docker, 512 MB RAM, HTTPS automático, Auto-deploy via GitHub | API Spring Boot 3.3.4 (Java 21) |
| **Frontend Web** | [Vercel](https://vercel.com) | CDN Global Anycast, SSL instantâneo, Edge Network rápida, Tráfego ilimitado | Aplicação React 18 + Vite + Tailwind |

---

## 📋 Pré-requisitos
1. Conta gratuita no [GitHub](https://github.com)
2. Conta gratuita no [Supabase](https://supabase.com)
3. Conta gratuita no [Render](https://render.com)
4. Conta gratuita no [Vercel](https://vercel.com)

---

## Passo 1: Criar o Banco PostgreSQL no Supabase (3 minutos)

1. Acesse [supabase.com](https://supabase.com) e faça login (pode usar o login com GitHub).
2. Clique em **"New Project"**.
3. Preencha as informações:
   - **Name:** `futmanager-db`
   - **Database Password:** Escolha uma senha forte (guarde essa senha com cuidado!).
   - **Region:** Selecione `South America (São Paulo)` para menor latência ou `East US`.
   - **Pricing Plan:** Free ($0/month).
4. Clique em **"Create new project"** e aguarde cerca de 1 a 2 minutos enquanto o banco é provisionado.
5. Quando o projeto estiver pronto:
   - No menu lateral esquerdo, clique no ícone da engrenagem **Project Settings** > **Database**.
   - Role até a seção **Connection string** e clique na aba **JDBC** (ou **URI**).
   - Copie a URL JDBC. Ela terá um formato parecido com:
     ```text
     jdbc:postgresql://db.xxxxxxxxxxxxxxxxxxxx.supabase.co:5432/postgres?sslmode=require
     ```
   - Anote os 3 dados de conexão:
     - **URL:** `jdbc:postgresql://db.xxxxxxxxxxxxxxxxxxxx.supabase.co:5432/postgres?sslmode=require`
     - **Usuário:** `postgres`
     - **Senha:** *(A senha que você definiu ao criar o projeto)*

> **Dica Flyway:** Não precisa rodar nenhum script SQL manual! Assim que o backend iniciar pela primeira vez conectado a esse banco, o Flyway criará automaticamente todas as tabelas e migrações (V1 a V5: multi-tenant, restrições de idade/responsáveis e autenticação segura com hash BCrypt).

---

## Passo 2: Subir o Código para o GitHub (2 minutos)

Se o projeto ainda não estiver no seu GitHub:

1. Crie um novo repositório vazio no seu GitHub (ex.: `futmanager`).
2. No seu terminal local na pasta `c:\Workspace\futmanager`:
   ```bash
   git init
   git add .
   git commit -m "feat: FutManager Multi-Tenant SaaS pronto para deploy"
   git branch -M main
   git remote add origin https://github.com/SEU_USUARIO/futmanager.git
   git push -u origin main
   ```

---

## Passo 3: Deploy do Backend no Render.com (4 minutos)

1. Acesse [render.com](https://render.com) e faça login com sua conta do GitHub.
2. No painel, clique no botão azul **"New +"** no canto superior direito e selecione **"Web Service"**.
3. Selecione a opção **"Build and deploy from a Git repository"** e clique em **Next**.
4. Conecte sua conta do GitHub e escolha o repositório `futmanager`.
5. Preencha as configurações do serviço:
   - **Name:** `futmanager-api` (ou o nome de sua preferência)
   - **Region:** `Oregon (US West)` ou `Frankfurt`
   - **Branch:** `main`
   - **Root Directory:** `backend` *(MUITO IMPORTANTE: aponte para a pasta backend)*
   - **Runtime:** `Docker` *(o Render detectará automaticamente o `backend/Dockerfile`)*
   - **Instance Type:** Selecione `Free` ($0/month).
6. Role a página até **Environment Variables** e clique em **Add Environment Variable** para adicionar as seguintes variáveis:

| Chave (Key) | Valor (Value) |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db.xxxxxxxxxxxxxxxxxxxx.supabase.co:5432/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `sua_senha_do_supabase` |
| `CORS_ALLOWED_ORIGINS` | `*` |
| `TIMEZONE` | `America/Sao_Paulo` |
| `AUTH_MODE` | `dev` |
| `STORAGE_TYPE` | `local` |

7. Clique em **"Create Web Service"**.
8. O Render iniciará a compilação do Maven e a inicialização do container. Acompanhe os logs até ver:
   ```text
   Tomcat started on port 10000 (http) with context path '/api'
   Started FutmanagerApplication in ... seconds
   ```
9. No topo da página do Render, copie a URL pública HTTPS gerada (exemplo: `https://futmanager-api.onrender.com`).
10. Teste no navegador acessando a documentação:
    `https://futmanager-api.onrender.com/api/swagger-ui/index.html`

> ⚠️ **Nota sobre o plano Free do Render:** No plano gratuito, se a API ficar sem receber chamadas por 15 minutos, ela entra em suspensão para economizar recursos. Na primeira chamada após a suspensão, o primeiro carregamento pode levar cerca de 40 a 50 segundos para despertar. Depois disso, responde instantaneamente.

---

## Passo 4: Deploy do Frontend no Vercel (2 minutos)

1. Acesse [vercel.com](https://vercel.com) e faça login com seu GitHub.
2. No painel, clique em **"Add New..."** > **"Project"**.
3. Selecione o repositório `futmanager` e clique em **"Import"**.
4. Na tela de configuração:
   - **Project Name:** `futmanager`
   - **Framework Preset:** `Vite` (geralmente detectado automaticamente)
   - **Root Directory:** Clique em **Edit** e selecione a pasta `frontend`.
5. Abra a seção **Environment Variables** e adicione:
   - **Name:** `VITE_API_URL`
   - **Value:** `https://futmanager-api.onrender.com` *(a URL gerada no Passo 3)*
6. Clique no botão azul **"Deploy"**.
7. Em cerca de 40 segundos, a Vercel concluirá o build e exibirá uma tela de comemoração com a URL pública oficial (exemplo: `https://futmanager.vercel.app`).

---

## Passo 5: Teste do Sistema em Produção Gratuita

Abra a URL da Vercel no celular ou no computador:

1. **Acesso com 1 Clique (Demonstração):**
   - Clique em **"Admin (Craques)"** para entrar na escolinha de demonstração já preenchida.
   - Clique em **"Admin (Leões da Colina)"** para validar o isolamento com painel zerado.
2. **Onboarding de uma Nova Escolinha Real:**
   - Clique em **"✨ Cadastrar Nova Escolinha (SaaS)"**.
   - Cadastre uma escolinha de teste real e comece a cadastrar turmas e alunos.
3. **Chamada no Celular:**
   - Faça login como professor, acerte as presenças na tela responsiva e clique em **Finalizar Chamada**.
4. **Relatórios e Planilhas:**
   - Acesse **Relatórios** e baixe os PDFs e planilhas XLSX com carimbo da escolinha.

---

## 💡 Dicas de Produção para o Futuro (Quando começar a vender)

Quando tiver clientes pagantes:
1. **Render Individual ($7/mês):** Faça o upgrade do serviço Web no Render para o plano **Starter ($7/mo)** para evitar que a API entre em suspensão após 15 minutos ocioso.
2. **Domínio Próprio:** No Vercel, você pode adicionar seu domínio personalizado gratuitamente (ex.: `app.suaescolinha.com.br`) com certificado SSL automático.
3. **Fotos no Supabase Storage:** Altere `STORAGE_TYPE` para `supabase` nas variáveis de ambiente do Render e adicione `SUPABASE_ANON_KEY` para salvar fotos direto em bucket seguro na nuvem.
