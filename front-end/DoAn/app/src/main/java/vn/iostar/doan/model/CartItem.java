package vn.iostar.doan.model;

import com.google.gson.annotations.SerializedName;

public class CartItem {
    @SerializedName("cartItemId") // Khớp với key "cartItemId" trong JSON
    private Long cartItemId; // Kiểu Long cho ID

    @SerializedName("quantity") // Khớp với key "quantity" trong JSON
    private int quantity; // Kiểu int cho số lượng

    @SerializedName("product") // Khớp với key "product" trong JSON
    private Product product;
    private transient boolean isSelected = false;
    public Long getCartItemId() {
        return cartItemId;
    }
    public CartItem(Long cartItemId, int quantity, Product product) {
        this.cartItemId = cartItemId;
        this.quantity = quantity;
        this.product = product;
    }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
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
    public double getProductPrice() {
        return (product != null) ? product.getPrice() : 0.0;
    }
    public double getItemTotalPrice() {
        return quantity * getProductPrice();
    }
}
