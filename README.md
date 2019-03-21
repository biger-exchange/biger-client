# biger-client
java client for transactions on BIGER exchange.
Requires application of an API access token - details at https://github.com/biger-exchange/biger-api-doc

## maven repo
the binary artifacts are released at maven repo https://biger-exchange.github.io/biger-client/
```
group：     com.biger
artifactId: biger-client
version:    1.0
```
to use in your maven pom, 
```
<repository>
  <id>biger</id>
  <name>biger</name>
  <url>https://biger-exchange.github.io/biger-client/</url>
</repository>

<dependency>
    <groupId>com.biger</groupId>
    <artifactId>biger-client</artifactId>
    <version>1.0</version>
</dependency>
```

## immediate roadmap plan
* java 8 support
* websocket market data
* websocket order change subscription
* protocol version compatibility check
* configurability of 
  - connect, req timeout
  - req expiry allowance
  - clock
* consider bigdecimal instead of string for fields like price, qty
* consider enums for well known field values - but also consider case where new field value possibilities are added in future
* consider sync client for people who dont care for async (or just advise them to call .get on future)

## examples
[examples](src/main/java/com/biger/client/example)
