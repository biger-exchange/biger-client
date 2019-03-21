package com.biger.client.httpops;

import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;

public interface HttpOpsBuilder {
    HttpOpsBuilder executor(Executor e);
    HttpOpsBuilder connectionTimeout(Duration to);
    HttpOps build();

    static HttpOpsBuilder newBuilder() {
        return ServiceLoader.load(HttpOpsBuilder.class).iterator().next();
    }
}
