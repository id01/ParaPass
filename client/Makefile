INDIR=src/main/cpp
LIBDIR=src/main/cpp/libs

all: build build/crypto.o build/libCrypto.so

build:
	mkdir build

build/crypto.o: build $(INDIR)/crypto.cpp $(INDIR)/crypto.h
	g++ -O3 $(INDIR)/crypto.cpp -c -fPIC -lcryptopp -lpthread -o build/crypto.o -Wall

build/libCrypto.so: build $(INDIR)/cryptoJava.cpp $(INDIR)/crypto.h build/crypto.o $(LIBDIR)/argon2/libargon2.a $(LIBDIR)/scrypt-jane/scrypt-jane.o
	g++ -O2 $(INDIR)/cryptoJava.cpp build/crypto.o $(LIBDIR)/argon2/libargon2.a $(LIBDIR)/scrypt-jane/scrypt-jane.o \
		-shared -fPIC -lcryptopp -lpthread -o build/libCrypto.so \
		-I/usr/lib/jvm/java-8-openjdk/include -I/usr/lib/jvm/java-8-openjdk/include/linux -Wall

.PHONY: test
test:
	g++ $(INDIR)/crypto.cpp $(LIBDIR)/argon2/libargon2.a $(LIBDIR)/scrypt-jane/scrypt-jane.o -o build/a.out -lcryptopp -lpthread -DTEST_CPP