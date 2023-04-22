#!/bin/bash
set -e

#TODO: Find header files

#javah NativeTools
gcc -fPIC -c -O2 -I /usr/lib/jvm/java-8-openjdk-amd64/include -I /usr/lib/jvm/java-8-openjdk-amd64/include/linux -Wall -o /tmp/NativeTools.o NativeTools.c
gcc -shared -fPIC -o build/native_tools.so /tmp/NativeTools.o -nostdlib -nolibc
strip build/native_tools.so
