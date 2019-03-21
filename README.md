# biger-client
java client for transactions on BIGER exchange.
Requires application of an API access token - details at https://github.com/biger-exchange/biger-api-doc

## maven repo
the binary artifacts are released at maven repo https://biger-exchange.github.io/biger-client/.

[gradle build.xml example for java 11](biger-client-examples/build.gradle)

[gradle build.xml example for java 8](biger-client-examples/build.gradle.java8)

## examples
[example project](biger-client-examples)

## java 11 vs java 8
 * We offer users the option of java 8 or java 11 compatibility.
 * We recommend the java 11 compatible version as long as you can use it
 * java 11 compatible release  uses jdk11+ HttpClient to execute http requests
 * java 8 compatible release uses HttpURLConnection to execute http requests
 * HttpClient supports NIO and async requests
   - many concurrent http requests can share the same thread or threads(we allow executor configurability)
 * HttpURLConnection means synchronous http requests, and one thread for every concurrent request

## tool for generating key pair to use for application of api access token
One of the requirements to apply for api access token is for you to generate your own RSA key pair, and provide the public key to biger exchange. Our client provides such API to generate the key pair for you if you wish.

[Example to generate RSA key pair](biger-client-examples/src/main/java/com/biger/client/examples/GenerateKeyPair.java)

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


