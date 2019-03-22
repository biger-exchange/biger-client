package biger.client.ws.react.domain;

import biger.client.ws.react.domain.response.SymbolEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ["BTCUSDT",{"close":"3972.78","deal":"5931075.58523433","high":"4138.55","last":"3972.78","low":"3762.83","open":"3762.83","period":86400,"volume":"1481.255499"}]
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder(value = {"symbol", "state"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BigerSymbolStateEvent implements SymbolEvent {

    private String symbol;

    private SymbolState state;

    @Data
    static public class SymbolState {
        private String close;

        private String deal;

        private String high;

        private String last;

        private String low;

        private String open;

        private long period;

        private String volume;
    }

    static public BigerSymbolStateEvent parse(List<JsonNode> args, ObjectMapper objectMapper) {
        return BigerSymbolStateEvent.builder()
                .symbol(objectMapper.convertValue(args.get(0), String.class))
                .state(objectMapper.convertValue(args.get(1), SymbolState.class))
                .build();

    }


}
