@echo off
chcp 65001 >nul
title FutManager - Subir para o GitHub
cls
echo ======================================================================
echo           ⚽ FUTMANAGER SAAS - PUBLICAR NO GITHUB (PASSO 1)
echo ======================================================================
echo.
echo Este script vai enviar o codigo do seu computador para o seu GitHub.
echo.
echo 1. Certifique-se de que ja criou um repositorio VAZIO no seu GitHub.
echo    (Exemplo: https://github.com/seu-usuario/futmanager.git)
echo.
echo ----------------------------------------------------------------------
set /p REPO_URL="Cole a URL do seu repositorio no GitHub: "

if "%REPO_URL%"=="" (
    echo.
    echo [AVISO] Nenhuma URL informada. Tente novamente!
    echo.
    pause
    exit /b
)

echo.
echo [*] Conectando ao seu repositorio remoto...
wsl sh -c "cd /mnt/c/Workspace/futmanager && git remote remove origin 2>/dev/null; git remote add origin '%REPO_URL%' && git push -u origin main"

echo.
echo ======================================================================
echo Se apareceu a mensagem 'branch main set up to track origin/main',
echo seu codigo ja esta disponivel na nuvem do GitHub!
echo Agora voce pode prosseguir com o Render e Vercel conforme o manual.
echo ======================================================================
echo.
pause
