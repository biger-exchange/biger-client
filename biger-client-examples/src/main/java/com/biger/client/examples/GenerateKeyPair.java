package com.biger.client.examples;

import com.biger.client.util.RSAKeyPairGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;

public class GenerateKeyPair {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        // get home directory for output
        String home = System.getProperty("user.home");
        System.out.println("Going to generate key pair at " + Path.of(home, "privateKey") + " and " + Path.of(home, "publicKey"));
        RSAKeyPairGenerator.generateKeyPair(
                Files.newOutputStream(Path.of(home, "privateKey"), StandardOpenOption.CREATE_NEW),
                Files.newOutputStream(Path.of(home, "publicKey"), StandardOpenOption.CREATE_NEW)
        );
        System.out.println("Generated key pair at " + Path.of(home, "privateKey") + " and " + Path.of(home, "publicKey"));
    }
}
