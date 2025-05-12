package vn.iostar.doan.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SelectedItemDetail implements Serializable {
    @SerializedName("cartItemId")
    private Long cartItemId;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("product")
    private Product product;  // Object Product chứa thông tin chi tiết (tên, giá, ảnh...)

    // Constructors (cần có constructor mặc định cho Gson)
    public SelectedItemDetail() {}

    public SelectedItemDetail(Long cartItemId, int quantity, Product product) {
        this.cartItemId = cartItemId;
        this.quantity = quantity;
        this.product = product;
    }

    // Getters and Setters
    public Long getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(Long cartItemId) {
        this.cartItemId = cartItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}