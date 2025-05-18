package vn.iostar.doan.model;

import java.time.LocalDateTime;
import java.util.Date;

public class Comment {
    private long commentId;
    private String image;
    private String content;
    private int rating;
    // private LocalDateTime createdAt; // Thay thế dòng này
    private Date createdAt; // Bằng dòng này
    private String fullname;
    private String avatar; // Thêm trường avatar nếu API trả về

    public Comment() {
    }

    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}