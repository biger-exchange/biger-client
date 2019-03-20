package com.biger.client.example;

import com.biger.client.BigerClient;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fill in the section denoted by ***INPUT PARAMETERS*** with appropriate values before proceeding
 * place private key file named "privateKey" in user home directory foe this to work
 */
public class GetSymbolList {
    public static void main(String[] args) throws Exception {

        // ***INPUT PARAMETERS*** {
        String accessToken = "test2";
        byte[] privateKey = Files.readAllBytes(Path.of(
                System.getProperty("user.home"),
                "privateKey"
        ));
        // ***INPUT PARAMETERS*** }

        BigerClient c = BigerClient.builder()
                .accessToken(accessToken)
                .privateKey(privateKey)
                .url("https://pub-api.biger.in")
                .build();

        c.symbols().list()
                .thenAccept(System.out::println)
                .join();

    }
}
