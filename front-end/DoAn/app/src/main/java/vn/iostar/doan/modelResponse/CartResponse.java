package vn.iostar.doan.modelResponse;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import vn.iostar.doan.model.CartItem;

public class CartResponse {
    @SerializedName("items") // Tên key chứa danh sách sản phẩm trong JSON
    private List<CartItem> items;

    @SerializedName("totalPrice") // Tên key chứa tổng tiền trong JSON
    private double totalPrice;

    @SerializedName("totalItems") // Có thể có hoặc không, tùy API
    private int totalItems;

    // Getters and Setters
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
}
