package vn.iostar.doan.modelRequest;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;

import vn.iostar.doan.model.Product;
import vn.iostar.doan.model.User1;

public class CommentRequest {
    private String content;
    private int rating; // Giữ lại nếu server của bạn chấp nhận và bạn muốn gửi
    private String image;  // Giữ lại nếu server chấp nhận và bạn muốn gửi (có thể là null)
    private long userId;
    private long productId;

    public CommentRequest(String content, int rating, String image, long userId, long productId) {
        this.content = content;
        this.rating = rating;
        this.image = image;
        this.userId = userId;
        this.productId = productId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    // Getters (không bắt buộc cho việc gửi đi với Gson/Moshi, nhưng có thể hữu ích)
    public String getContent() { return content; }
    public int getRating() { return rating; }
    public String getImage() { return image; }
}
