package com.biger.client.httpops;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public interface Utils {

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

        long expiry = s.clock.millis() + s.expiryLeewayMillis;

        return s.encryptors.borrowAndApply(c->{
            return s.httpOps.sendAsync(
                    uri,
                    s.accessToken,
                    c,
                    method,
                    queryString,
                    expiry,
                    body,
                    Duration.ofSeconds(10L))
                    .thenApply(pl->{
                        BigerResponse<T> resp;
                        try {
                            resp = m.readValue(pl, typeReference);
                        } catch (IOException e) {
                            throw new BigerResponseException("error while parsing response body", e);
                        }
                        // if the request fails due to crypto failures of expiry timeout, the http status is still 200.
                        // but the code field in response body will be something else
                        if (resp.code != 200) {
                            if (resp.code == 900108) {
                                throw new BigerResponseException("expired request - check that your system clock is synchronized", 200, pl);
                            }
                            if (resp.code == 900109) {
                                throw new BigerResponseException("please check that your access token is valid and not expired, else contact biger to file a report", 200, pl);
                            }
                            throw new BigerResponseException("err code is " + resp.code, 200, pl);
                        }
                        return resp;
                    });
        });

    }

    static CompletableFuture<JsonNode> reqGeneric(State s, String path, String queryString, String method, String body) {
        URI uri = Utils.newUriUnchecked(s.url, path);

        long expiry = s.clock.millis() + s.expiryLeewayMillis;

        return s.encryptors.borrowAndApply(c-> s.httpOps.sendAsync(
                uri,
                s.accessToken,
                c,
                method,
                queryString,
                expiry,
                body,
                Duration.ofSeconds(10L))
                .thenApply(pl-> {
                    try {
                        return m.readTree(pl);
                    } catch (IOException e) {
                        throw new BigerResponseException("error while parsing response body", e);
                    }
                }));
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
