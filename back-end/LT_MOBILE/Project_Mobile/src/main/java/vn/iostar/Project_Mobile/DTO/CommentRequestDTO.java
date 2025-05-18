package vn.iostar.Project_Mobile.DTO; // Hoặc package DTO của bạn

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDTO {

    private String image; // Tên trường này nên khớp với JSON client gửi, hoặc client gửi "imageUrl"

    @NotBlank(message = "Content cannot be blank")
    private String content;

    @NotNull(message = "Rating cannot be null")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating; // Dùng Integer để có thể là null nếu client không gửi, rồi validate @NotNull

    // Client sẽ gửi userId và productId
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "Product ID cannot be null")
    private Long productId;

    private String avatar; // Nếu client cũng gửi avatar cho comment
    private Long orderId;
}