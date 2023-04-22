@echo off

set JDK_PATH=C:\Programs\Fiji.app\jdk\bin
set FIJI_PATH=C:\Programs\Fiji.app

if not exist %JDK_PATH%\javac.exe echo Cannot find Java compiler. Install JDK 8 and provide path to its 'bin' directory in the '%0' file.
if not exist %FIJI_PATH%\jars\ij-1.*.jar echo Cannot find ImageJ jar file. Install Fiji and provide path to it in the '%0' file.
cd /d %~dp0
mkdir build 2> NUL > NUL
mkdir dist 2> NUL > NUL
%JDK_PATH%\javac -encoding utf8 -cp %FIJI_PATH%\jars\ij-1.*.jar -d build Points_Detector.java Slices_Correction.java Slices_Div.java Colors_Move.java Utils.java    || goto error
cd build
%JDK_PATH%\jar cvf ..\dist\Points_Detector.jar ..\plugins.config *.class          || goto error
cd ..

if "%1"=="run" goto run
goto :EOF

:run
copy /y dist\Points_Detector.jar %FIJI_PATH%\plugins\
taskkill /f /fi "WINDOWTITLE eq (Fiji*"
start "" %FIJI_PATH%\ImageJ-win64.exe test.tif
goto :EOF

:error
echo Error detected
goto :EOF
