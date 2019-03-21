package com.biger.client.examples;

import com.biger.client.util.RSAKeyPairGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;

public class GenerateKeyPair {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        // get home directory for output
        String home = System.getProperty("user.home");
        System.out.println("Going to generate key pair at " + Paths.get(home, "privateKey") + " and " + Paths.get(home, "publicKey"));
        RSAKeyPairGenerator.generateKeyPair(
                Files.newOutputStream(Paths.get(home, "privateKey"), StandardOpenOption.CREATE_NEW),
                Files.newOutputStream(Paths.get(home, "publicKey"), StandardOpenOption.CREATE_NEW)
        );
        System.out.println("Generated key pair at " + Paths.get(home, "privateKey") + " and " + Paths.get(home, "publicKey"));
    }
}
