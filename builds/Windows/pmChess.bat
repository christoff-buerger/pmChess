@echo off
setlocal

rem Clear any user set ERRORLEVEL variable:
set "ERRORLEVEL="

set "SCRIP_DIR=%~dp0"
set "SCRIP_DIR=%SCRIP_DIR:~0,-1%"

set "PMCHESS_ERROR=0"
rem start /wait "" "%SCRIP_DIR%\pmChess.exe" %* >"%SCRIP_DIR%\log.txt" 2>&1
call "%SCRIP_DIR%\pmChess.exe" %* >"%SCRIP_DIR%\log.txt" 2>&1
if ERRORLEVEL 1 (
	set "PMCHESS_ERROR=1"
)
type "%SCRIP_DIR%\log.txt"
pause

endlocal

exit /b %PMCHESS_ERROR%
