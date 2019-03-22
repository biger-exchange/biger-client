package biger.client.ws.react.domain;

import biger.client.ws.react.domain.response.SymbolEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * false,{"asks":[["3507","0.016254"],["3509.08","0"],["3510.47","0.022001"],["3512.57","0"],["3513.96","0.047923"]],"bids":[["3504.99","0.017987"]]},"BTCUSDT"]
 * [false,{"asks":[["0.033263","0.533"],["0.033264","0"]],"bids":[]},"ETHBTC"]
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"full", "orderBook", "symbol"})
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BigerMarketDepthEvent implements SymbolEvent {

    private Boolean full;

    private OrderBook orderBook;

    private String symbol;

    @Data
    static public class OrderBook {

        private List<Order> asks;

        private List<Order> bids;

    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"price", "qty"})
    @Data
    static public class Order {
        private BigDecimal price;

        private BigDecimal qty;
    }

}
