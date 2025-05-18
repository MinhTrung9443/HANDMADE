package vn.iostar.doan.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Product {
    private long productId;
    //ảnh đại diện
    private String image;

    //danh sách các ảnh của product
    private List<String> images;
    @SerializedName("imageUrls")
    private List<String> imageUrls;

    private String name;
    private Double price;
    private String description;
    private int quantity;
    private Category category;
    private List<Comment> comments;
    private List<Product> relatedProducts;

    public Product(long productId, String image, List<String> imageUrls, String name, Double price, String description, int quantity, Category category, List<Comment> comments, List<Product> relatedProducts) {
        this.productId = productId;
        this.imageUrls = imageUrls;
        this.name = name;
        this.price = price;
        this.description = description;
        this.quantity = quantity;
        this.category = category;
        this.comments = comments;
        this.relatedProducts = relatedProducts;
    }
    public Product(long id) {
        this.productId = id;
    }

    public Product() {
    }

    public Product(long productId, String image, String name, Double price, String description, Category category) {
        this.productId = productId;
        this.image = image;
        this.name = name;
        this.price = price;
        this.description = description;
        this.category = category;
    }
    public List<String> getImages() {
        return images;
    }
    public void setImages(List<String> images) {
        this.images = images;
    }
    public Product(String image, String name, Double price) {

        this.image = image;
        this.name = name;
        this.price = price;

    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
    public List<String> getImageUrls() {
        return imageUrls;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Product> getRelatedProducts() {
        return relatedProducts;
    }

    public void setRelatedProducts(List<Product> relatedProducts) {
        this.relatedProducts = relatedProducts;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}

