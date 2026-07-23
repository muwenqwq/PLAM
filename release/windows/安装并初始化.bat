@echo off
chcp 65001 >nul
title 智学工坊 - 安装并初始化
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install.ps1"
echo.
pause
