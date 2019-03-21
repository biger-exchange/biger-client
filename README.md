# biger-client
java client for transactions on BIGER exchange.
Requires application of an API access token - details at https://github.com/biger-exchange/biger-api-doc


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
