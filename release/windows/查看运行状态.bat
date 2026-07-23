@echo off
chcp 65001 >nul
title 智学工坊 - 运行状态
powershell.exe -NoProfile -Command "try {(Invoke-WebRequest -UseBasicParsing http://127.0.0.1:18080/api/health).Content} catch {'后端未运行：' + $_.Exception.Message}; try {(Invoke-WebRequest -UseBasicParsing http://127.0.0.1:18000/ai/health).Content} catch {'AI 服务未运行：' + $_.Exception.Message}"
echo.
pause
