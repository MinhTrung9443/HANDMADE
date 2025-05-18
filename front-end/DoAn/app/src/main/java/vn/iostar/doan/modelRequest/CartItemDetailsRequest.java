package vn.iostar.doan.modelRequest;

import java.util.List;

public class CartItemDetailsRequest {
    private List<Long> cartItemIds;

    public CartItemDetailsRequest(List<Long> cartItemIds) {
        this.cartItemIds = cartItemIds;
    }

    // Getter (Setter không cần thiết nếu chỉ dùng để gửi đi)
    public List<Long> getCartItemIds() {
        return cartItemIds;
    }
}