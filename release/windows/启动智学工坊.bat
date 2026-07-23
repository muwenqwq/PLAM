@echo off
chcp 65001 >nul
title 智学工坊 - 启动
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start.ps1"
echo.
pause
