@echo off
chcp 65001 >nul
title FutManager - Sincronizar com o GitHub (WSL)
cls
echo ======================================================================
echo           ⚽ FUTMANAGER SAAS - SINCRONIZAR COM O GITHUB
echo ======================================================================
echo.
echo Repositório configurado: https://github.com/Nmartins6/futmanager.git
echo (Utilizando Git via WSL Linux)
echo.
echo Escolha uma opção:
echo [1] Enviar alterações atuais para https://github.com/Nmartins6/futmanager.git
echo [2] Trocar a URL do repositório remoto
echo [3] Sair
echo.
set /p OPCAO="Digite a opção desejada (1, 2 ou 3) [padrão: 1]: "

if "%OPCAO%"=="" set OPCAO=1
if "%OPCAO%"=="3" exit /b

if "%OPCAO%"=="2" (
    echo.
    set /p REPO_URL="Cole a nova URL do GitHub (ex: https://github.com/usuario/repo.git): "
    if "%REPO_URL%"=="" (
        echo [AVISO] Nenhuma URL informada. Cancelando...
        pause
        exit /b
    )
    echo Conectando nova URL...
    wsl sh -c "cd /mnt/c/Workspace/futmanager && git remote remove origin 2>/dev/null; git remote add origin '%REPO_URL%'"
)

echo.
echo [*] Enviando código para o GitHub via WSL...
echo.
wsl sh -c "cd /mnt/c/Workspace/futmanager && git push -u origin main"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ======================================================================
    echo  ✅ SUCESSO! SEU CÓDIGO ESTÁ ATUALIZADO NO GITHUB!
    echo ======================================================================
    echo Repositório: https://github.com/Nmartins6/futmanager
    echo.
    echo Próximos passos para o Deploy gratuito:
    echo 1. Render.com: Crie o Web Service conectando o repositório 'futmanager'.
    echo 2. Vercel.com: Crie o projeto importando 'futmanager' (pasta raiz: frontend).
    echo ======================================================================
) else (
    echo.
    echo ======================================================================
    echo  ❌ OCORREU UM ERRO AO ENVIAR PARA O GITHUB
    echo ======================================================================
    echo Verifique a mensagem de erro acima.
    echo Se foi pedido login, confirme a autenticação do GitHub no terminal.
    echo ======================================================================
)
echo.
pause
