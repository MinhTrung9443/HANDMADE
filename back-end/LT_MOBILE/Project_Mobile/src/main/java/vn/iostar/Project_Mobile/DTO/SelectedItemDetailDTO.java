package vn.iostar.Project_Mobile.DTO;
import lombok.*;
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class SelectedItemDetailDTO {
    private Long cartItemId;
    private Integer quantity;
    private ProductInfoDTO product;
}