package com.biger.client.ws.react.domain.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@JsonPropertyOrder(value = {"method", "params", "id"})
@Data
@Builder
public class ExchangeRequest<T> {

    private String method;

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    private T params;

    private long id;
}
