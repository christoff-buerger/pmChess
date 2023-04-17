@echo off
setlocal

rem Clear any user set ERRORLEVEL variable:
set "ERRORLEVEL="

set "SCRIP_DIR=%~dp0"
set "SCRIP_DIR=%SCRIP_DIR:~0,-1%"

set "PMCHESS_ERROR=0"
"%SCRIP_DIR%\pmChess.exe" %*
if ERRORLEVEL 1 (
	set "PMCHESS_ERROR=1"
)
pause

endlocal
exit /b %PMCHESS_ERROR%
