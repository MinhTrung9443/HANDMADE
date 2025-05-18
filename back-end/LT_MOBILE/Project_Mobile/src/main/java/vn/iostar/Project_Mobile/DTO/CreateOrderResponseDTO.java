package vn.iostar.Project_Mobile.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.iostar.Project_Mobile.entity.Order; // Import Order entity

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponseDTO {
    private Order order; // Thông tin đơn hàng đã tạo
    private String paymentUrl; // URL thanh toán (sẽ là null nếu không phải VNPAY)
}