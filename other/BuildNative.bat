@echo off

SET TOKEN=%RANDOM%%TIME%%RANDOM%
SET TOKEN=%TOKEN::=%
SET TOKEN=%TOKEN:,=%
SET TOKEN=%TOKEN:.=%
SET TOKEN=%TOKEN: =%
SET TEMP_FILE=%TEMP%\%TOKEN%

IF NOT EXIST C:\msys64\msys2_shell.cmd GOTO install_help

:check_ucrt64
CALL C:\msys64\msys2_shell.cmd -defterm -here -no-start -ucrt64 %~dp0\BuildNative.sh _check
IF NOT ERRORLEVEL 1 GOTO success_ucrt64
ECHO WARNING!!! Cannot compile native tools for Windows x64 platform. 1>&2
CALL :install_help
GOTO check_mingw32
:success_ucrt64

CALL C:\msys64\msys2_shell.cmd -defterm -here -no-start -ucrt64 %~dp0\BuildNative.sh "%1" 64

:check_mingw32
CALL C:\msys64\msys2_shell.cmd -defterm -here -no-start -mingw32 %~dp0\BuildNative.sh _check
IF NOT ERRORLEVEL 1 GOTO success_mingw32
ECHO WARNING!!! Cannot compile native tools for Windows x86 platform. 1>&2
GOTO install_help
:success_mingw32

CALL C:\msys64\msys2_shell.cmd -defterm -here -no-start -mingw32 %~dp0\BuildNative.sh "%1" 32

goto :EOF

:install_help
ECHO Native tools compilation environment is missing some parts. 1>&2
ECHO Install Msys2 from https://www.msys2.org/ to default path   1>&2
ECHO C:\msys64 and install at least following packages in it:    1>&2
ECHO pacman -S mingw-w64-ucrt-x86_64-gcc mingw-w64-i686-gcc      1>&2
goto :EOF
