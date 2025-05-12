package vn.iostar.doan.modelResponse;

import  com.google.gson.annotations.SerializedName;
import java.io.Serializable;

// Represents the data sent to the backend for adding/updating an address
public class AddressInputDTO implements Serializable {

    // Changed SerializedName to match backend's expected field name
    @SerializedName("recipientName")
    private String fullName; // Bạn có thể giữ tên biến là fullName hoặc đổi thành recipientName cho rõ ràng

    // Changed SerializedName to match backend's expected field name
    @SerializedName("recipientPhone")
    private String phone; // Bạn có thể giữ tên biến là phone hoặc đổi thành recipientPhone cho rõ ràng

    // Changed SerializedName to match backend's expected field name
    @SerializedName("streetAddress")
    private String houseNumber; // Bạn có thể giữ tên biến là houseNumber hoặc đổi thành streetAddress cho rõ ràng

    @SerializedName("ward")
    private String ward;

    @SerializedName("district")
    private String district;

    @SerializedName("city")
    private String city;

    @SerializedName("country")
    private String country;

    @SerializedName("isDefaultAddress")
    private boolean isDefaultAddress;

    // Constructor (giữ nguyên, truyền giá trị vào các biến nội bộ)
    public AddressInputDTO(String fullName, String phone, String houseNumber, String ward, String district, String city, String country, boolean isDefaultAddress) {
        this.fullName = fullName; // Giá trị từ EditTextFullName sẽ được gán vào biến này
        this.phone = phone; // Giá trị từ EditTextPhone sẽ được gán vào biến này
        this.houseNumber = houseNumber; // Giá trị từ EditTextHouseNumber sẽ được gán vào biến này
        this.ward = ward;
        this.district = district;
        this.city = city;
        this.country = country;
        this.isDefaultAddress = isDefaultAddress;
    }

    // Getters (giữ nguyên, chúng trả về giá trị của biến nội bộ)
    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public String getWard() {
        return ward;
    }

    public String getDistrict() {
        return district;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public boolean isDefaultAddress() {
        return isDefaultAddress;
    }
}