package com.biger.client.httpops.httpurlconnection;

import com.biger.client.httpops.HttpOps;
import com.biger.client.httpops.HttpOpsBuilder;
import com.biger.client.httpops.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class HttpUrlConnectionOpsBuilder implements HttpOpsBuilder {

    static final Logger logger = LoggerFactory.getLogger(HttpUrlConnectionOpsBuilder.class);

    Duration connectionTimeout = Duration.ofSeconds(5L);

    @Override
    public HttpOpsBuilder executor(Executor e) {
        logger.warn("ignoring executor because async requests are not supported using HTTPURLConnection");
        return this;
    }

    @Override
    public HttpOpsBuilder connectionTimeout(Duration to) {
        connectionTimeout = Objects.requireNonNull(to);
        return this;
    }

    @Override
    public HttpOps build() {
        int connectTimeout = (int) this.connectionTimeout.getNano() / 1000;

        return new HttpOps() {
            @Override
            public CompletableFuture<String> sendAsync(URI uri, String accessToken, Cipher c, String method, String queryString, long expiry, String body, Duration timeout) {

                CompletableFuture<String> f = new CompletableFuture<>();

                HttpURLConnection conn;

                try {
                    URI withQuery = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), queryString, uri.getFragment());
                    conn = (HttpURLConnection) withQuery.toURL().openConnection();
                } catch (IOException | URISyntaxException e) {
                    f.completeExceptionally(e);
                    return f;
                }
                conn.setConnectTimeout(connectTimeout);
                conn.setReadTimeout(10000);

                try {
                    conn.setRequestMethod(method);
                } catch (ProtocolException e) {
                    f.completeExceptionally(e);
                    return f;
                }

                if (accessToken != null) {
                    conn.setRequestProperty("BIGER-REQUEST-EXPIRY", expiry + "");
                    conn.setRequestProperty("BIGER-ACCESS-TOKEN", accessToken);
                    conn.setRequestProperty("BIGER-REQUEST-HASH", Utils.requestHash(c, method, queryString, expiry, body));
                }

                if (body != null) {
                    conn.setRequestProperty("content-type", "application/json");
                    conn.setDoOutput(true);
                    OutputStream os = null;
                    try {
                        os = conn.getOutputStream();
                        os.write(body.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    } catch (IOException e) {
                        f.completeExceptionally(e);
                        return f;
                    }
                }

                int respCode;
                try {
                    respCode = conn.getResponseCode();
                } catch (IOException e) {
                    f.completeExceptionally(e);
                    return f;
                }
                if (respCode != 200) {
                    f.completeExceptionally(new RuntimeException("non 200 response"));
                    return f;
                }
                try (InputStream is = conn.getInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] b = new byte[128];
                    int read;
                    while ((read = is.read(b)) != -1) {
                        baos.write(b, 0, read);
                    }
                    f.complete(new String(baos.toByteArray(), StandardCharsets.UTF_8));
                    return f;
                } catch (IOException e) {
                    f.completeExceptionally(e);
                    return f;
                }
            }
        };
    }
}
