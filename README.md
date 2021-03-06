# biger-client
java client for transactions on BIGER exchange.
Requires application of an API access token - details at https://github.com/biger-exchange/biger-api-doc

## maven repo
the binary artifacts are released at maven repo https://biger-exchange.github.io/biger-client/.

[gradle build.xml example for java 11](https://github.com/biger-exchange/biger-client-example/blob/master/build.gradle)

[gradle build.xml example for java 8](https://github.com/biger-exchange/biger-client-example/blob/master/build.gradle.java8)

## examples
[example project](https://github.com/biger-exchange/biger-client-example)

## java 11 vs java 8
 * We offer users the option of java 8 or java 11 compatibility.
 * We recommend the java 11 compatible version as long as you can use it
 * java 11 compatible release  uses jdk11+ HttpClient to execute http requests
 * java 8 compatible release uses HttpURLConnection to execute http requests
 * HttpClient supports NIO and async requests
   - many concurrent http requests can share the same thread or threads(we allow executor configurability)
 * HttpURLConnection means synchronous http requests, and one thread for every concurrent request
 * for our market data websocket/react client, we provide an impl based on jdk11+ HttpClient and another impl based on netty4
 * if you are not on java11+, your only option there is based on netty4, introducing another dependency to your runtime

## tool for generating key pair to use for application of api access token
One of the requirements to apply for api access token is for you to generate your own RSA key pair, and provide the public key to biger exchange. Our client provides such API to generate the key pair for you if you wish.

[Example to generate RSA key pair](https://github.com/biger-exchange/biger-client-example/blob/master/src/main/java/com/biger/client/examples/GenerateKeyPair.java)

## websocket react api for market data
This is available now in a beta state, we expect some reorganization of the code and changes to the general APIs and possibly bugs

## immediate roadmap plan
* protocol version compatibility check
* consider enums for well known field values - but also consider case where new field value possibilities are added in future
* consider sync client for people who dont care for async (or just advise them to call .get on future)


