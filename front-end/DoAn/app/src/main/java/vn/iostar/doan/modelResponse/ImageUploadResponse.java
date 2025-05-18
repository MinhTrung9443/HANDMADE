package vn.iostar.doan.modelResponse;

import com.google.gson.annotations.SerializedName; 

public class ImageUploadResponse {


    @SerializedName("imageUrl")
    private String imageUrl; 

    public ImageUploadResponse() {
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }


    // (Optional) toString() để dễ debug
    @Override
    public String toString() {
        return "ImageUploadResponse{" +
                "imageUrl='" + imageUrl + '\'' +
                '}';
    }
}