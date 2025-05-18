package vn.iostar.Project_Mobile.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
public class AddressDTO {

    private long addressId; // ID của địa chỉ, cần để client xác định khi muốn sửa/xóa

    private String recipientName; // Tên người nhận

    private String recipientPhone; // SĐT người nhận

    private String streetAddress; // Số nhà, tên đường...

    private String ward; // Phường/Xã

    private String district; // Quận/Huyện

    private String city; // Tỉnh/Thành phố

    private String country; // Quốc gia

    private boolean isDefault; // Có phải địa chỉ mặc định không?

    private String goongPlaceId; // (Tùy chọn) Place ID từ Goong nếu bạn lưu lại
    

}