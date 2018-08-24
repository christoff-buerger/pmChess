@rem This program and the accompanying materials are made available under the
@rem terms of the MIT license (X11 license) which accompanies this distribution.
@rem	
@rem Author: Christoff Bürger

@rem Find given executable. Echos all absolut paths where it can be found.
@rem Echos nothing and returns with an error code if not found.

@echo off

set "found=1"
for %%e in ( %PATHEXT% ) do (
	for %%i in ( %1%%e ) do (
		if not "%%~$PATH:i"=="" (
			echo=%%~$PATH:i
			set "found=0"
		)
	)
)
exit /b %found%
