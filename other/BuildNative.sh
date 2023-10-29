#!/bin/bash
set -e

JDK_INCLUDE=$1/include
FLAGS="-O3 -Wall"

mkdir -p build

echo Compiling native tool with $JDK_INCLUDE

case "$OSTYPE" in
  *darwin*)
    gcc -c -fPIC $FLAGS -I$JDK_INCLUDE -I$JDK_INCLUDE/darwin -o /tmp/NativeTools.o other/NativeTools.c
    gcc -dynamiclib -o src/native_tools.dylib /tmp/NativeTools.o -lc
    strip src/native_tools.dylib
    ;;
  *msys* | *win*)
    gcc -c $FLAGS -I$JDK_INCLUDE -I$JDK_INCLUDE/win32 -o /tmp/NativeTools$ARCH.o other/NativeTools.c
    gcc -shared -Wl,--kill-at -o src/native_tools$ARCH.dll /tmp/NativeTools$ARCH.o
    strip src/native_tools$ARCH.dll
    ;;
  *)
    gcc -c -fPIC $FLAGS -I$JDK_INCLUDE -I$JDK_INCLUDE/linux -o /tmp/NativeTools.o other/NativeTools.c
    gcc -shared -fPIC -o src/native_tools.so /tmp/NativeTools.o -lc
    strip src/native_tools.so
esac
