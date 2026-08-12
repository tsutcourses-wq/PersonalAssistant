@echo off
setlocal
cd /d "%~dp0"
if not exist ".github\workflows" mkdir ".github\workflows"
copy /Y "GITHUB_WORKFLOW_TEMPLATE\build-apk.yml" ".github\workflows\build-apk.yml" >nul
if errorlevel 1 (
  echo Failed to create .github workflow.
  pause
  exit /b 1
)
echo.
echo Created successfully:
echo   .github\workflows\build-apk.yml
echo.
echo Upload the project contents to your GitHub repository.
pause
