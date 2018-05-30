#!/bin/sh
g++ crypto.cpp libs/argon2/libargon2.a libs/scrypt-jane/scrypt-jane.o -lcryptopp -lpthread -DTEST_CPP $@
