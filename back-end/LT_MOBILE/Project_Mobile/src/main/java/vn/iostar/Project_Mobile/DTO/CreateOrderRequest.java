package vn.iostar.Project_Mobile.DTO;

import lombok.Data;
import vn.iostar.Project_Mobile.util.PaymentMethod; // Import enum PaymentMethod
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
@Data
public class CreateOrderRequest {
	
	@NotEmpty(message = "Please select items to order.")
	private List<Long> cartItemIds; 
	@NotNull(message = "Please select a payment method.")
    private PaymentMethod paymentMethod;
}
