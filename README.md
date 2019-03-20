# biger-client
java client

pre-release condition for the moment, use at your own risk
-immediate roadmap plan
-java 8 support
-websocket market data
-websocket order change subscription
-protocol version compatibility check
-error handling
  - decryption error
  - expiry error
  - non 200 resp error
  - ip block
-configurability of 
  -connect, req timeout
  -req expiry allowance
  -clock
-consider bigdecimal instead of string for fields like price, qty
-consider enums for well known field values - but also consider case where new field value possibilities are added in future
-consider sync client for people who dont care for async (or just advise them to call .get on future)
