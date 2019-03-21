package com.biger.client.httpops.httpclient;

import com.biger.client.httpops.HttpOps;
import com.biger.client.httpops.HttpOpsBuilder;
import com.biger.client.httpops.Utils;

import javax.crypto.Cipher;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class HttpClientOpsBuilder implements HttpOpsBuilder {

    Executor e;
    Duration connectionTimeout = Duration.ofSeconds(5L);

    @Override
    public HttpOpsBuilder executor(Executor e) {
        this.e = e;
        return this;
    }

    @Override
    public HttpOpsBuilder connectionTimeout(Duration to) {
        connectionTimeout = Objects.requireNonNull(to);
        return this;
    }

    @Override
    public HttpOps build() {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectionTimeout);

        if (e != null) {
            httpClientBuilder = httpClientBuilder.executor(e);
        }
        HttpClient httpClient = httpClientBuilder.build();

        return new HttpOps() {
            @Override
            public CompletableFuture<String> sendAsync(URI uri, String accessToken, Cipher c, String method, String queryString, long expiry, String body, Duration timeout) {
                HttpRequest.Builder b = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("BIGER-REQUEST-EXPIRY", expiry + "")
                        .header("BIGER-ACCESS-TOKEN", accessToken)
                        .header("BIGER-REQUEST-HASH", Utils.requestHash(c, method, queryString, expiry, body))
                        .timeout(Duration.ofSeconds(5));
                if (body == null) {
                    b = b.method(method, HttpRequest.BodyPublishers.noBody());
                } else {
                    b = b.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                            .header("content-type", "application/json");
                }
                HttpRequest req = b.build();

                return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(resp->{
                        if (resp.statusCode() != 200) {
                                throw new RuntimeException("non 200 response");
                        }
                        return resp;
                    })
                    .thenApply(HttpResponse::body);
            }
        };
    }
}