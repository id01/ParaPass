#!/bin/sh
g++ -O2 crypto.cpp libs/argon2/libargon2.a libs/scrypt-jane/scrypt-jane.o -shared -fPIC -o crypto.so -lcryptopp -lpthread -lpython3.6m $@
