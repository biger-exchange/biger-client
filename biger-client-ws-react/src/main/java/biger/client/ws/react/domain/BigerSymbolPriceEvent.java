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

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"symbol", "price"})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BigerSymbolPriceEvent implements SymbolEvent {

    private String symbol;

    private BigDecimal price;

    public static BigerSymbolPriceEvent parse(List<String> args) {
        return BigerSymbolPriceEvent.builder()
                .symbol(args.get(0))
                .price(new BigDecimal(args.get(1)))
                .build();
    }
}
