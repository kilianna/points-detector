@echo off

set JDK_PATH=C:\Programy\Fiji.app\jdk\bin
set FIJI_PATH=C:\Programy\Fiji.app

if not exist %JDK_PATH%\javac.exe echo Cannot find Java compiler. Install JDK 8 and provide path to its 'bin' directory in the '%0' file.
if not exist %FIJI_PATH%\jars\ij-1.*.jar echo Cannot find ImageJ jar file. Install Fiji and provide path to it in the '%0' file.
cd /d %~dp0
mkdir build 2> NUL > NUL
mkdir dist 2> NUL > NUL
%JDK_PATH%\javac -encoding utf8 -cp %FIJI_PATH%\jars\ij-1.*.jar -d build Points_Detector.java
cd build
%JDK_PATH%\jar cvf ..\dist\Points_Detector.jar ..\plugins.config Points_Detector*.class
cd ..
