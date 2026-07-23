@echo off
chcp 65001 >nul
title 智学工坊 - 重置演示数据
echo 此操作会删除本作品目录中的数据库、学习记录和上传资料。
set /p confirm=请输入 RESET 确认：
if /i not "%confirm%"=="RESET" (
  echo 已取消。
  pause
  exit /b 0
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "& '%~dp0scripts\stop.ps1'"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "& '%~dp0scripts\install.ps1' -Force"
echo 重置完成，请重新启动智学工坊。
pause
