package com.biger.client.examples;

import com.biger.client.BigerClient;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Fill in the section denoted by ***INPUT PARAMETERS*** with appropriate values before proceeding
 * place private key file named "privateKey" in user home directory foe this to work
 */
public class OrderCancel {
    public static void main(String[] args) throws Exception {

        // ***INPUT PARAMETERS*** {
        String accessToken = "test2";
        byte[] privateKey = Files.readAllBytes(Paths.get(
                System.getProperty("user.home"),
                "privateKey"
        ));
        String orderId = "9d09bfa7-9356-42bd-968f-f19459e2c6ab";
        // ***INPUT PARAMETERS*** }

        BigerClient c = BigerClient.builder()
                .accessToken(accessToken)
                .privateKey(privateKey)
                .url("https://pub-api.biger.in")
                .build();

        c.orders().cancel(orderId)
                .join();

    }
}
