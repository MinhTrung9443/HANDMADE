package vn.iostar.doan.modelRequest;

import java.util.List;

import vn.iostar.doan.model.PaymentMethod;

public class CreateOrderRequest {
    private List<Long> cartItemIds;
    private PaymentMethod paymentMethod;

    public CreateOrderRequest(List<Long> cartItemIds, PaymentMethod paymentMethod) {
        this.cartItemIds = cartItemIds;
        this.paymentMethod = paymentMethod;
    }

    public List<Long> getCartItemIds() {
        return cartItemIds;
    }

    public void setCartItemIds(List<Long> cartItemIds) {
        this.cartItemIds = cartItemIds;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
