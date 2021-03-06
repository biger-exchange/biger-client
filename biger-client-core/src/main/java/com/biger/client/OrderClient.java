package com.biger.client;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface OrderClient {

    CompletableFuture<OrderInfo> query(String orderId);
    CompletableFuture<OrderInfo> createLimitOrder(String symbol, boolean isBuy, BigDecimal unitPrice, BigDecimal qty);
    CompletableFuture<Void> cancel(String orderId);

    class OrderInfo {
        public String orderId;
        public String side;
        public String symbol;
        public String orderType;
        public String orderState;
        public BigDecimal price;
        public BigDecimal orderQty;
        public BigDecimal filledQty;
        public BigDecimal totalPrice;
        public BigDecimal fee;
        public long userId;
        public int tradesCount;
        public long createTime;
        public String rejectReason;

        @Override
        public String toString() {
            return "OrderInfo{" +
                    "orderId='" + orderId + '\'' +
                    ", side='" + side + '\'' +
                    ", symbol='" + symbol + '\'' +
                    ", orderType='" + orderType + '\'' +
                    ", orderState='" + orderState + '\'' +
                    ", price='" + price + '\'' +
                    ", orderQty='" + orderQty + '\'' +
                    ", filledQty='" + filledQty + '\'' +
                    ", totalPrice='" + totalPrice + '\'' +
                    ", fee='" + fee + '\'' +
                    ", userId=" + userId +
                    ", tradesCount=" + tradesCount +
                    ", createTime=" + createTime +
                    ", rejectReason='" + rejectReason + '\'' +
                    '}';
        }
    }

    enum ErrorCode {
        ORDER_DATA_NOT_FOUND(5101),
        ORDER_USER_MISMATCH(5165),
        ORDER_ALREADY_CANCELLED(5161),
        ORDER_CANCEL_WRONG_STATE(5166),
        ORDER_CANCEL_FAILED(5163);

        public final int code;

        ErrorCode(int i) {
            code = i;
        }

        public static Optional<ErrorCode> forCode(int c) {
            return Arrays.stream(ErrorCode.values()).filter(ec->ec.code == c).findFirst();
        }
    }
}
