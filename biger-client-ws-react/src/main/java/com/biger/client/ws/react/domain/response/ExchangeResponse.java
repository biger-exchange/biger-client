package com.biger.client.ws.react.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {"id":null,"method":"state.update","params":["BTCUSDT",{"close":"3720.55","deal":"3179815.94242754","high":"3812.08","last":"3720.55","low":"3660.03","open":"3733.34","period":86400,"volume":"852.550245"}]}
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ExchangeResponse<T> {

    private ExchangeError error;

    private Object result;

    private Long id;

    private String method;

    private T params;

}
