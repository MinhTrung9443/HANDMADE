package vn.iostar.Project_Mobile.DTO;

import lombok.*;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class ProductInfoDTO {
    private Long productId;
    private String image;
    private String name;
    private Double price;
}