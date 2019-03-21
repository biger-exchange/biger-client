package com.biger.client.util;

import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public interface RSAKeyPairGenerator {
    static void generateKeyPair(OutputStream privateKey, OutputStream publicKey) throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair p = g.generateKeyPair();
        privateKey.write(p.getPrivate().getEncoded());
        privateKey.flush();
        publicKey.write(p.getPublic().getEncoded());
        publicKey.flush();
    }
}
