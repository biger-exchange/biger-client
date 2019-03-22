package biger.client.ws.react.domain;

import biger.client.ws.react.domain.response.SymbolEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class BigerDealEvent implements SymbolEvent {

    private String symbol;

    private List<BigerDealItem> deals;

    /**
     * [{"price": "8044", "type": "buy", "time": 1520438400.361028, "amount": "2", "id": 1762},
     */
    @Data
    public static class BigerDealItem {

        private String price;

        private String type;

        private String time;

        private String amount;

        private long id;
    }

    static public BigerDealEvent parse(List<JsonNode> args, ObjectMapper objectMapper) {
        List<BigerDealItem> items = objectMapper.convertValue(args.get(1), objectMapper.getTypeFactory().constructCollectionType(List.class, BigerDealItem.class));
        return BigerDealEvent.builder()
                .symbol(objectMapper.convertValue(args.get(0), String.class))
                .deals(items)
                .build();
    }
}
