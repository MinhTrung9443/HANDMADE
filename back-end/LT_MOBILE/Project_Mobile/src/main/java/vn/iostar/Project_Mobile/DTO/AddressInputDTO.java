package vn.iostar.Project_Mobile.DTO;

import jakarta.validation.constraints.NotBlank; // Thêm validation nếu cần
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressInputDTO {

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận quá dài")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(min = 10, max = 15, message = "Số điện thoại không hợp lệ")
    private String recipientPhone;

    @NotBlank(message = "Địa chỉ chi tiết (số nhà, đường) không được để trống")
    @Size(max = 255, message = "Địa chỉ chi tiết quá dài")
    private String streetAddress;

    @NotBlank(message = "Phường/Xã không được để trống")
    @Size(max = 100, message = "Tên Phường/Xã quá dài")
    private String ward;

    @NotBlank(message = "Quận/Huyện không được để trống")
    @Size(max = 100, message = "Tên Quận/Huyện quá dài")
    private String district;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    @Size(max = 100, message = "Tên Tỉnh/Thành phố quá dài")
    private String city;

    @Size(max = 100, message = "Tên Quốc gia quá dài")
    private String country; // Có thể là optional hoặc mặc định ở backend

    @NotNull(message = "Vui lòng chỉ định đây có phải địa chỉ mặc định không")
    private Boolean isDefault = false; // Nên có giá trị mặc định là false

    private String goongPlaceId; 

}