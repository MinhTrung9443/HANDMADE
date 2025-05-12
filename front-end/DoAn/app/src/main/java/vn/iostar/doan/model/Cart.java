package vn.iostar.doan.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Cart {
    @SerializedName("cartId") // Khớp với tên trường JSON trả về
    private Long cartId;

    @SerializedName("user")
    private User user; // Chỉ cần ID hoặc các thông tin cần thiết

    @SerializedName("cartItems")
    private List<CartItem> cartItems;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }
}









