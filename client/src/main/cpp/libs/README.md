This is the libs folder. It contains some dependencies for the C++ Python module compilation.  
Please download [argon2](https://github.com/P-H-C/phc-winner-argon2), [scrypt-jane](https://github.com/floodyberry/scrypt-jane), and
[tweetnacl](https://tweetnacl.cr.yp.to/software.html) from their respective locations, then compile them all.
Scrypt-jane should be compiled with the parameters `-DSCRYPT_SALSA -DSCRYPT_SHA256` (standard Scrypt).  
Also, you will need libpthread, libpython3.6m, and libcrypto++ all downloaded.  