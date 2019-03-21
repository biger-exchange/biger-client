package com.biger.client;

import java.math.BigDecimal;
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
        public String price;
        public String orderQty;
        public String filledQty;
        public String totalPrice;
        public String fee;
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
}
