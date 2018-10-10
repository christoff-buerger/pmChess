@rem This program and the accompanying materials are made available under the
@rem terms of the MIT license (X11 license) which accompanies this distribution.
@rem	
@rem Author: Christoff Bürger

@rem Initialize tools needed for the native Windows build.
@rem IMPORTANT: The 'ANT_HOME' path of Apache Ant must be set and '%ANT_HOME%\bin' added to the
@rem 	'PATH' environment variable.

@echo off

rem Enable advanced batch file commands:
verify argument_to_enforce_error 2>nul
setlocal EnableExtensions
if ERRORLEVEL 1 (
	echo=SCRIPT ABORTED: Command extensions not supported.
	exit /b 1
)
verify argument_to_enforce_error 2>nul
setlocal EnableDelayedExpansion
if ERRORLEVEL 1 (
	endlocal rem Undo "setlocal EnableExtensions"
	echo=SCRIPT ABORTED: Delayed expansion not supported.
	exit /b 1
)

rem Configure "exit" to terminate subroutines and entire script:
if not "%SelfWrapped%"=="%~0" (
	set "SelfWrapped=%~0"
	%ComSpec% /s /c ""%~0" %*"
	endlocal rem Undo "setlocal EnableExtensions"
	endlocal rem Undo "setlocal EnableDelayedExpansion"
	goto :eof
)

rem Initialize Java:
call where javac >nul 2>nul
if ERRORLEVEL 1 (
	for /d %%j in ( "C:\Program Files\Java\jdk-11.*" "C:\Program Files\Java\jdk-10.*" ) do (
		set "JAVA_HOME=%%~fj"
		set "PATH=!JAVA_HOME!\bin;!PATH!"
		goto :JAVA_INITIALIZED
	)
	set "EMESSAGE=no Java installation found"
	call :ERROR
)
:JAVA_INITIALIZED

rem Initialize Microsoft build environment:
call where msbuild >nul 2>nul
if ERRORLEVEL 1 (
	for %%v in ( "Enterprise" "Professional" "Community" "BuildTools" ) do (
		if exist "C:\Program Files (x86)\Microsoft Visual Studio\2017\%%v\VC\Auxiliary\Build\vcvarsall.bat" (
			call "C:\Program Files (x86)\Microsoft Visual Studio\2017\%%v\VC\Auxiliary\Build\vcvarsall.bat" x86
			if ERRORLEVEL 1 ( rem Not working: 'vcvarsall.bat' never returns error code.
				set "EMESSAGE=Microsoft build environment initialization failed (Microsoft Visual Studio v15.0)"
				call :ERROR
			)
			goto :VISUAL_STUDIO_INITIALIZED
		)
	)
	for %%v in ( "14.0" "12.0" "11.0" "10.0" ) do (
		if exist "C:\Program Files (x86)\Microsoft Visual Studio %%v\VC\vcvarsall.bat" (
			call "C:\Program Files (x86)\Microsoft Visual Studio %%v\VC\vcvarsall.bat" x86
			if ERRORLEVEL 1 (
				set "EMESSAGE=Microsoft build environment initialization failed (Microsoft Visual Studio v%%v)"
				call :ERROR
			)
			goto :VISUAL_STUDIO_INITIALIZED
		)
	)
	set "EMESSAGE=no Microsoft Visual Studio installation found"
	call :ERROR
)
:VISUAL_STUDIO_INITIALIZED

rem Start terminal for interactive use within the initialized environment:
cmd /k
exit 0

:ERROR
echo=
echo=ERROR: %EMESSAGE%.
echo=
exit 1
