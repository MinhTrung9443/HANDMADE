package vn.iostar.Project_Mobile.DTO;


import lombok.Data;

@Data
public class CartItemRequest {
    private Long productId;
    private int quantity;
}
