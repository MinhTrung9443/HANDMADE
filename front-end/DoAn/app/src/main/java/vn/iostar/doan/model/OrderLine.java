package vn.iostar.doan.model;

import com.google.gson.annotations.SerializedName;

public class OrderLine {


    @SerializedName("orderLineId")
    private Long orderLineId;

    @SerializedName("quantity")
    private Integer quantity;

    @SerializedName("price")
    private Double price;

    private Product2 product2;

    @SerializedName("product")
    private Product product;
    public OrderLine() {
    }

    public OrderLine(Long orderLineId) {
        this.orderLineId = orderLineId;
    }
    public OrderLine(Long orderLineId, Integer quantity, Double price, Product product) {
        this.orderLineId = orderLineId;
        this.quantity = quantity;
        this.price = price;
        this.product = product;
    }

    public Long getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(Long orderLineId) {
        this.orderLineId = orderLineId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
    
}
