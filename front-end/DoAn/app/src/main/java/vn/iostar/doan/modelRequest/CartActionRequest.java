package vn.iostar.doan.modelRequest;


public class CartActionRequest {
    private Long productId;
    private int quantity;

    public CartActionRequest(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    // Getters (Setters không cần thiết nếu chỉ dùng để gửi đi)
    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}