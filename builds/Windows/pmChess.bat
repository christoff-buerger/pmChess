@echo off
setlocal

rem Clear any user set ERRORLEVEL variable:
set "ERRORLEVEL="

rem Configure absolute path used as anchor for any further file references:
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

rem Execute pmChess:
set "PMCHESS_ERROR=0"
if "%*"=="" (
	"%SCRIPT_DIR%\pmChess.exe" --debug
) else (
	"%SCRIPT_DIR%\pmChess.exe" %*
)
if ERRORLEVEL 1 (
	set "PMCHESS_ERROR=1"
) else if not ERRORLEVEL 0 (
	set "PMCHESS_ERROR=1"
)
pause

rem Return exit code:
endlocal
exit /b %PMCHESS_ERROR%
