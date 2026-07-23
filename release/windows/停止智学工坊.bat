@echo off
chcp 65001 >nul
title 智学工坊 - 停止
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\stop.ps1"
echo.
pause
