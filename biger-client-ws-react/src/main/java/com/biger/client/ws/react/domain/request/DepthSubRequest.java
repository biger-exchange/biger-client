package com.biger.client.ws.react.domain.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonPropertyOrder(value = {"symbol", "limit", "interval"})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepthSubRequest {

    private String symbol;

    private int limit;

    private String interval;
}
