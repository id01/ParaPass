INDIR=src/main/cpp
LIBDIR=src/main/cpp/libs

CXX=g++

all: build build/crypto.o build/libCrypto.so src/main/resources/lib/libCrypto.so

build:
	mkdir build

build/crypto.o: build $(INDIR)/crypto.cpp $(INDIR)/crypto.h
	$(CXX) -O3 $(INDIR)/crypto.cpp -c -fPIC -lcryptopp -lpthread -o build/crypto.o -Wall

build/libCrypto.so: build $(INDIR)/cryptoJava.cpp $(INDIR)/crypto.h build/crypto.o $(LIBDIR)/argon2/libargon2.a $(LIBDIR)/scrypt-jane/scrypt-jane.o $(LIBDIR)/tweetnacl/tweetnacl.o
	$(CXX) -O2 $(INDIR)/cryptoJava.cpp build/crypto.o $(LIBDIR)/argon2/libargon2.a $(LIBDIR)/scrypt-jane/scrypt-jane.o \
		$(LIBDIR)/tweetnacl/tweetnacl.o -shared -fPIC -lcryptopp -lpthread -o build/libCrypto.so \
		-I/usr/lib/jvm/java-8-openjdk/include -I/usr/lib/jvm/java-8-openjdk/include/linux -Wall

src/main/resources/lib/libCrypto.so: build/libCrypto.so
	cp build/libCrypto.so src/main/resources/lib/libCrypto.so

.PHONY: test
test:
	$(CXX) $(INDIR)/crypto.cpp $(LIBDIR)/argon2/libargon2.a $(LIBDIR)/scrypt-jane/scrypt-jane.o $(LIBDIR)/tweetnacl/tweetnacl.o -o build/a.out -lcryptopp -lpthread -DTEST_CPP -g
