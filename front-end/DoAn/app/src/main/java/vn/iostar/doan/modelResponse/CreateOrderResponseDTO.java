package vn.iostar.doan.modelResponse;

import com.google.gson.annotations.SerializedName; // Import nếu dùng Gson (mặc định của Retrofit thường là Gson)

import vn.iostar.doan.model.Order; // *** Import lớp Order của project Android ***

public class CreateOrderResponseDTO {

    // Tên các trường phải khớp với tên trong JSON response từ backend
    // Hoặc sử dụng @SerializedName nếu tên khác nhau

    @SerializedName("order") // Đảm bảo khớp với key "order" trong JSON response
    private Order order;

    @SerializedName("paymentUrl") // Đảm bảo khớp với key "paymentUrl" trong JSON response
    private String paymentUrl;

    // Constructor (không bắt buộc nếu dùng thư viện parse JSON như Gson)
    public CreateOrderResponseDTO(Order order, String paymentUrl) {
        this.order = order;
        this.paymentUrl = paymentUrl;
    }

    // Getters (BẮT BUỘC cho thư viện parse JSON)
    public Order getOrder() {
        return order;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    // Setters (Không bắt buộc cho việc parse, nhưng có thể cần nếu bạn muốn sửa đổi đối tượng)
    public void setOrder(Order order) {
        this.order = order;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    // (Optional) toString() để debug
    @Override
    public String toString() {
        return "CreateOrderResponseDTO{" +
                "order=" + (order != null ? order.getOrderId() : "null") + // Log ID cho gọn
                ", paymentUrl='" + paymentUrl + '\'' +
                '}';
    }
}