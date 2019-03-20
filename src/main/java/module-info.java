module BigerClient {
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires java.sql;

    exports com.biger.client;
    exports com.biger.client.util;
}