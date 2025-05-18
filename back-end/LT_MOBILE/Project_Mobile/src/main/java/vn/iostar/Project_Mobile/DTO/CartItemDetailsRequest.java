package vn.iostar.Project_Mobile.DTO;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
public class CartItemDetailsRequest {
	@NotEmpty(message = "Danh sách ID không được rỗng")
    private List<Long> cartItemIds;

    public CartItemDetailsRequest() {}

    public List<Long> getCartItemIds() {
        return cartItemIds;
    }

    public void setCartItemIds(List<Long> cartItemIds) {
        this.cartItemIds = cartItemIds;
    }
}
