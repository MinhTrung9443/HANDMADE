package vn.iostar.doan.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;
public class Order {

    @SerializedName("orderId")
    private Long orderId; // Dùng Long (kiểu đối tượng) để có thể là null

    @SerializedName("totalPrice")
    private Double totalPrice; // Dùng Double

    @SerializedName("reviewed")
    private boolean reviewed;

    @SerializedName("orderDate")
    private Date orderDate; // Nhận là String

    @SerializedName("predictReceiveDate")
    private Date predictReceiveDate;

    @SerializedName("itemsSubtotal")
    private Double itemsSubtotal;

    @SerializedName("paymentMethod")
    private PaymentMethod paymentMethod;

    @SerializedName("shippingFee") // Hoặc "shipping_fee"
    private double shippingFee;

    @SerializedName("status")
    private OrderStatus status;
    @SerializedName("discountAmount") // Hoặc "discount_amount"
    private double discountAmount;

    @SerializedName("voucherCode") // Hoặc "voucher_code"
    private String voucherCode;
    
    @SerializedName("shippingAddress")
    private String shippingAddress;

    @SerializedName("user")
    private User user; // Tham chiếu đến lớp User

    @SerializedName("orderLines")
    private List<OrderLine> orderLines;

    public Order() {
    }

    public Order(Long orderId, Double totalPrice, Date orderDate, Date predictReceiveDate, Double itemsSubtotal, PaymentMethod paymentMethod, OrderStatus status, String shippingAddress, User user, List<OrderLine> orderLines) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.orderDate = orderDate;
        this.predictReceiveDate = predictReceiveDate;
        this.itemsSubtotal = itemsSubtotal;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.shippingAddress = shippingAddress;
        this.user = user;
        this.orderLines = orderLines;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    public double getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(double shippingFee) {
        this.shippingFee = shippingFee;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public Date getPredictReceiveDate() {
        return predictReceiveDate;
    }

    public void setPredictReceiveDate(Date predictReceiveDate) {
        this.predictReceiveDate = predictReceiveDate;
    }

    public Double getItemsSubtotal() {
        return itemsSubtotal;
    }

    public void setItemsSubtotal(Double itemsSubtotal) {
        this.itemsSubtotal = itemsSubtotal;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<OrderLine> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<OrderLine> orderLines) {
        this.orderLines = orderLines;
    }
}
