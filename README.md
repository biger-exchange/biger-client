# biger-client
java client for transactions on BIGER exchange.
Requires application of an API access token - details at https://github.com/biger-exchange/biger-api-doc

## maven repo
the binary artifacts are released at maven repo https://biger-exchange.github.io/biger-client/
[gradle build.xml example for java 11](biger-client-examples/build.gradle)
[gradle build.xml example for java 8](biger-client-examples/build.gradle.java8)

## examples
[example project](biger-client-examples)

## java 11 vs java 8
We offer users the option of java 8 or java 11 compatibility.
We recommend the java 11 compatible version as long as you can use it, as it uses jdk11+ HttpClient to execute Http requests instead of HttpURLConnection. One difference is that HttpClient supports NIO and async requests, where many concurrent http requests can share the same thread or threads(we allow executor configurability). However, HttpURLConnection means sync http requests, and one thread for every concurrent request.

## immediate roadmap plan
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


