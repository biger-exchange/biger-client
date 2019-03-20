package com.biger.client.internal;

import com.biger.client.BigerResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

interface Utils {

    ObjectMapper m = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    static URI newUriUnchecked(String baseUrl, String path) {
        try {
            return  new URI(baseUrl + path);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("invalid url");
        }
    }

    static String requestHash(Cipher cipher, String method, String queryString, long expiry, String body) {
        byte[] pl =
                ((queryString == null ? "": queryString) +
                method +
                expiry +
                (body == null ? "" : body))
                        .getBytes(StandardCharsets.UTF_8);
        try {
            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(
                            MessageDigest.getInstance("SHA-256").digest(pl)
                    )
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new IllegalStateException("bad cipher key", e);
        }
    }



    static <T> CompletableFuture<BigerResponse<T>> req(State s, String path, String queryString, String method, String body, TypeReference<BigerResponse<T>> typeReference) {
        URI uri = Utils.newUriUnchecked(s.url, path);

        long expiry = System.currentTimeMillis() + 10000;

        HttpRequest req = s.encryptors.borrowAndApply(c-> {

            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("BIGER-REQUEST-EXPIRY", expiry + "")
                    .header("BIGER-ACCESS-TOKEN", s.accessToken)
                    .header("BIGER-REQUEST-HASH", Utils.requestHash(c, method, queryString, expiry, body))
                    .timeout(Duration.ofSeconds(5));
            if (body == null) {
                b = b.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                b = b.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .header("content-type", "application/json");
            }
            return b.build();
        });

        return s.httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(HttpResponse::body)
                .thenApply(pl->{
                    try {
                        return m.readValue(pl, typeReference);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    static String encodePath(String source) {

        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
        boolean changed = false;
        for (byte b : bytes) {
            if (b < 0) {
                b += 256;
            }
            char c = (char)b;
            if (c == '/' || c == ':' || c == '@' || '!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
                    ',' == c || ';' == c || '=' == c || '-' == c || '.' == c || '_' == c || '~' == c || Character.isAlphabetic(c) || (c >= '0' && c <='9')) {
                bos.write(b);
            }
            else {
                bos.write('%');
                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                bos.write(hex1);
                bos.write(hex2);
                changed = true;
            }
        }
        return (changed ? new String(bos.toByteArray(), StandardCharsets.UTF_8) : source);
    }
}
