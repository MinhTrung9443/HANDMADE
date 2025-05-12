package vn.iostar.doan.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Address  implements Serializable{
    @SerializedName("addressId")
    private Long addressId;

    @SerializedName("recipientName")
    private String fullName;

    @SerializedName("recipientPhone")
    private String phone;

    @SerializedName("streetAddress")
    private String houseNumber; // Or streetAddress

    @SerializedName("ward")
    private String ward; // Added based on typical address structures

    @SerializedName("district")
    private String district;

    @SerializedName("city")
    private String city; // Or province

    @SerializedName("country")
    private String country;

    @SerializedName("default")
    private boolean isDefaultAddress;
    private User1 user;

    // Constructor (optional, good for creating new ones)
    public Address() {
    }

    public Address(long addressId, String houseNumber, String district, String city, String country, User1 user) {
        this.addressId = addressId;
        this.houseNumber = houseNumber;
        this.district = district;
        this.city = city;
        this.country = country;
        this.user = user;
    }

    // Getters and Setters
    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isDefaultAddress() {
        return isDefaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        isDefaultAddress = defaultAddress;
    }

    // Helper method to get a displayable address string
    public String getFullAddressString() {
        StringBuilder sb = new StringBuilder();
        if (houseNumber != null && !houseNumber.isEmpty()) sb.append(houseNumber).append(", ");
        if (ward != null && !ward.isEmpty()) sb.append(ward).append(", ");
        if (district != null && !district.isEmpty()) sb.append(district).append(", ");
        if (city != null && !city.isEmpty()) sb.append(city);
        if (country != null && !country.isEmpty() && sb.length() > 0) sb.append(", ").append(country); // Add country only if city exists

        // Remove trailing comma and space if any
        String fullAddress = sb.toString().trim();
        if (fullAddress.endsWith(",")) {
            fullAddress = fullAddress.substring(0, fullAddress.length() - 1).trim();
        }
        return fullAddress.isEmpty() ? "Địa chỉ chưa hoàn chỉnh" : fullAddress;
    }

    @Override
    public String toString() {
        return "Address{" +
                "addressId=" + addressId +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", houseNumber='" + houseNumber + '\'' +
                ", ward='" + ward + '\'' +
                ", district='" + district + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", isDefaultAddress=" + isDefaultAddress +
                '}';
    }
}
