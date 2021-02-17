package com.serverless.external.portsp;


import com.serverless.external.portsp.exception.CryptoException;

import java.io.InputStream;

public interface CryptoStreamFactory {
    InputStream newDecryptStream(InputStream var1) throws CryptoException;

    InputStream newEncryptStream(InputStream var1) throws CryptoException;
}