package vn.iostar.doan.activity; // Đảm bảo package này đúng

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
// Thêm các import cần thiết
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RatingBar; // Đảm bảo đã import
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group; // Import Group
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections; // Import Collections
import java.util.List;
import java.util.Locale;

import me.relex.circleindicator.CircleIndicator3;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.iostar.doan.R; // Đảm bảo R được import đúng
import vn.iostar.doan.api.ApiService;
// Giả sử bạn có các adapter và model này:
import vn.iostar.doan.adapter.ImagesViewPager2Adapter;
import vn.iostar.doan.adapter.ProductAdapter; // Import ProductAdapter
import vn.iostar.doan.adapter.CommentAdapter; // Import CommentAdapter
import vn.iostar.doan.model.Cart;
import vn.iostar.doan.model.Comment; // Import Comment model
import vn.iostar.doan.model.Product; // Import Product model
import vn.iostar.doan.modelRequest.CartActionRequest;
// import vn.iostar.doan.model.Category; // Import Category nếu cần thiết trong Product model


public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";

    // --- Views ---
    private ViewPager2 viewPagerProductImages;
    private CircleIndicator3 indicator;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView textViewError;
    private NestedScrollView nestedScrollViewContent;
    private Group groupProductContent; // Group để quản lý visibility
    private TextView textViewProductName;
    private RatingBar ratingBarAverage; // Giữ nguyên tên biến
    private TextView textViewRatingInfo; // Giữ nguyên tên biến
    private TextView textViewProductPrice;
    private TextView textViewProductQuantity;
    private TextView textViewProductCategory;
    private TextView textViewProductDescription;
    private TextView textViewRelatedTitle;
    private RecyclerView recyclerViewRelatedProducts;
    private TextView textViewCommentsTitle;
    private Button buttonViewAllComments;
    private RecyclerView recyclerViewComments;
    private FloatingActionButton fabAddToCart;


    // --- Adapters ---
    private ImagesViewPager2Adapter imagesAdapter;
    private ProductAdapter relatedProductAdapter;
    private CommentAdapter commentAdapter;

    // --- API Service ---
    private ApiService apiService;

    // --- Data ---
    private long currentProductId = -1;
    private long userId = -1;
    private Product currentProduct; // Biến để lưu trữ sản phẩm hiện tại (Quan trọng cho việc truyền dữ liệu)
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        authToken = getIntent().getStringExtra("token");
        AnhXa(); // Ánh xạ View và khởi tạo Adapter
        apiService = ApiService.apiService; // Khởi tạo ApiService
        setupToolbar(); // Cài đặt Toolbar

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        if (intent != null) {
            currentProductId = intent.getLongExtra("productId", -1L);
            userId = intent.getLongExtra("userId", -1L);
            Log.d(TAG, "Received from Intent - Product ID: " + currentProductId + ", User ID: " + userId);

            // Nếu có Product ID hợp lệ thì fetch dữ liệu
            if (currentProductId != -1) {
                fetchProductDetails(currentProductId);
            } else {
                showError("Lỗi: Mã sản phẩm không hợp lệ.");
                Log.e(TAG, "Invalid Product ID received from Intent.");
                // Có thể finish() activity ở đây nếu product ID là bắt buộc
                // finish();
            }
        } else {
            // Trường hợp Intent null
            showError("Lỗi: Không thể nhận dữ liệu chi tiết sản phẩm.");
            Log.e(TAG, "Intent is null in onCreate.");
            // finish(); // Có thể đóng activity nếu không có intent
        }

        // --- Setup Listeners ---
        fabAddToCart.setOnClickListener(v -> handleAddToCartClick());
        buttonViewAllComments.setOnClickListener(v -> openProductRatingActivity());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(""); // Title sẽ được cập nhật sau
        }
    }

    // Xử lý sự kiện khi nút back trên toolbar được nhấn
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Hành động quay lại mặc định
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Ánh xạ các View từ layout XML và khởi tạo Adapters
    private void AnhXa() {
        viewPagerProductImages = findViewById(R.id.viewPagerProductImages);
        indicator = findViewById(R.id.indicator);
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        textViewError = findViewById(R.id.textViewError);
        nestedScrollViewContent = findViewById(R.id.nestedScrollView);
        groupProductContent = findViewById(R.id.groupProductContent);

        textViewProductName = findViewById(R.id.textViewProductName);
        ratingBarAverage = findViewById(R.id.ratingBarAverage); // Giữ nguyên tên biến
        textViewRatingInfo = findViewById(R.id.textViewRatingInfo); // Giữ nguyên tên biến
        textViewProductPrice = findViewById(R.id.textViewProductPrice);
        textViewProductQuantity = findViewById(R.id.textViewProductQuantity);
        textViewProductCategory = findViewById(R.id.textViewProductCategory);
        textViewProductDescription = findViewById(R.id.textViewProductDescription);

        textViewRelatedTitle = findViewById(R.id.textViewRelatedTitle);
        recyclerViewRelatedProducts = findViewById(R.id.recyclerViewRelatedProducts);
        textViewCommentsTitle = findViewById(R.id.textViewCommentsTitle);
        buttonViewAllComments = findViewById(R.id.buttonViewAllComments);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        fabAddToCart = findViewById(R.id.fabAddToCart);

        // --- Setup RecyclerView Related Products ---
        recyclerViewRelatedProducts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        // Đảm bảo constructor ProductAdapter phù hợp
        relatedProductAdapter = new ProductAdapter(this, new ArrayList<>());
        recyclerViewRelatedProducts.setAdapter(relatedProductAdapter);
        recyclerViewRelatedProducts.setHasFixedSize(true);

        // --- Setup RecyclerView Comments ---
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        // Khởi tạo CommentAdapter chỉ với Context
        commentAdapter = new CommentAdapter(this); // Sử dụng constructor chỉ nhận Context
        recyclerViewComments.setAdapter(commentAdapter);
        recyclerViewComments.setHasFixedSize(false); // Comment có thể có chiều cao khác nhau
        recyclerViewComments.setNestedScrollingEnabled(false); // Quan trọng khi trong NestedScrollView
    }

    // Gọi API để lấy chi tiết sản phẩm
    private void fetchProductDetails(long productId) {
        showLoading(true);
        showError(null); // Ẩn lỗi cũ

        if (apiService == null) {
            Log.e(TAG, "ApiService is not initialized!");
            showError("Lỗi cấu hình ứng dụng.");
            showLoading(false);
            return;
        }

        // Đảm bảo hàm getProductDetails tồn tại và trả về Product đầy đủ
        Call<Product> call = apiService.getProductDetails(productId);
        call.enqueue(new Callback<Product>() {
            @Override
            public void onResponse(@NonNull Call<Product> call, @NonNull Response<Product> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    // <<=== LƯU SẢN PHẨM HIỆN TẠI ===>>
                    currentProduct = response.body(); // Gán vào biến thành viên
                    Log.d(TAG, "API call successful. Product: " + currentProduct.getName());
                    displayProductDetails(currentProduct); // Hiển thị dữ liệu
                    showContent(true); // Hiển thị nội dung chính
                } else {
                    // Xử lý lỗi từ server
                    currentProduct = null; // Reset biến khi lỗi
                    String errorMsg = "Không thể tải dữ liệu sản phẩm. Mã lỗi: " + response.code();
                    if (response.errorBody() != null) {
                        try { errorMsg += " - " + response.errorBody().string(); } catch (Exception e) { Log.e(TAG, "Error parsing error body", e); }
                    } else { errorMsg += " - " + response.message(); }
                    Log.e(TAG, "API call failed: " + errorMsg);
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Product> call, @NonNull Throwable t) {
                // Xử lý lỗi mạng hoặc lỗi khác
                currentProduct = null; // Reset biến khi lỗi
                showLoading(false);
                Log.e(TAG, "API call failed due to network or execution error: ", t);
                showError("Lỗi kết nối mạng hoặc máy chủ.");
            }
        });
    }

    // Hiển thị dữ liệu Product lên các View
    private void displayProductDetails(Product product) {
        if (product == null) {
            Log.e(TAG, "Cannot display details for a null product.");
            showError("Lỗi: Dữ liệu sản phẩm không hợp lệ.");
            return;
        }

        // --- Hiển thị thông tin cơ bản ---
        textViewProductName.setText(product.getName());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(product.getName());
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        textViewProductPrice.setText(currencyFormatter.format(product.getPrice()));
        textViewProductQuantity.setText("Còn lại: " + product.getQuantity()); // Giả sử có getQuantity()

        // Giả sử có getCategory() trả về Category có getName()
        if (product.getCategory() != null && product.getCategory().getName() != null) {
            textViewProductCategory.setText("Danh mục: " + product.getCategory().getName());
            textViewProductCategory.setVisibility(View.VISIBLE);
        } else {
            textViewProductCategory.setVisibility(View.GONE);
        }

        textViewProductDescription.setText(product.getDescription()); // Giả sử có getDescription()

        // ===> TÍNH TOÁN VÀ HIỂN THỊ RATING TỪ COMMENTS <===
        List<Comment> commentsForRating = product.getComments(); // Lấy danh sách comments
        int ratingCount = 0;
        float averageRating = 0f;

        if (commentsForRating != null && !commentsForRating.isEmpty()) {
            ratingCount = commentsForRating.size();
            int totalRating = 0;
            for (Comment comment : commentsForRating) {
                totalRating += comment.getRating();
            }
            averageRating = (float) totalRating / ratingCount;

            ratingBarAverage.setRating(averageRating);
            textViewRatingInfo.setText(String.format(Locale.US, "%.1f (%d đánh giá)", averageRating, ratingCount));
            ratingBarAverage.setVisibility(View.VISIBLE);
            textViewRatingInfo.setVisibility(View.VISIBLE);
            Log.d(TAG, "Calculated Rating: Avg=" + averageRating + ", Count=" + ratingCount);

        } else {
            ratingCount = 0;
            averageRating = 0f;
            ratingBarAverage.setVisibility(View.GONE);
            textViewRatingInfo.setText("(Chưa có đánh giá)");
            textViewRatingInfo.setVisibility(View.VISIBLE);
            Log.d(TAG, "No comments found to calculate rating.");
        }
        // ===> KẾT THÚC TÍNH TOÁN VÀ HIỂN THỊ RATING <===


        // --- Hiển thị ảnh sản phẩm ---
        // Giả sử có getImageUrls() trả về List<String>
        List<String> imageUrls = product.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            imagesAdapter = new ImagesViewPager2Adapter(imageUrls);
            viewPagerProductImages.setAdapter(imagesAdapter);
            indicator.setViewPager(viewPagerProductImages);
            indicator.setVisibility(imageUrls.size() > 1 ? View.VISIBLE : View.GONE);
            viewPagerProductImages.setVisibility(View.VISIBLE);
        } else {
            viewPagerProductImages.setVisibility(View.GONE);
            indicator.setVisibility(View.GONE);
            Log.w(TAG, "No images found for product ID: " + product.getProductId());
            // Có thể set ảnh mặc định cho viewPagerProductImages nếu muốn
            // viewPagerProductImages.setAdapter(new ImagesViewPager2Adapter(Collections.singletonList("drawable://" + R.drawable.placeholder))); // Ví dụ
        }

        // --- Hiển thị Sản phẩm liên quan ---
        // Giả sử có getRelatedProducts() trả về List<Product>
        List<Product> related = product.getRelatedProducts();
        if (related != null && !related.isEmpty()) {
            textViewRelatedTitle.setVisibility(View.VISIBLE);
            recyclerViewRelatedProducts.setVisibility(View.VISIBLE);
            // Giả sử ProductAdapter có hàm updateList
            relatedProductAdapter.updateList(related);
        } else {
            textViewRelatedTitle.setVisibility(View.GONE);
            recyclerViewRelatedProducts.setVisibility(View.GONE);
        }

        // --- Hiển thị Bình luận (CHỈ 1 BÌNH LUẬN ĐẦU TIÊN) ---
        // Sử dụng lại biến commentsForRating đã lấy ở trên
        if (commentsForRating != null && !commentsForRating.isEmpty()) {
            textViewCommentsTitle.setVisibility(View.VISIBLE);
            buttonViewAllComments.setVisibility(View.VISIBLE); // Hiện nút "Xem tất cả"
            recyclerViewComments.setVisibility(View.VISIBLE);

            // Tạo list chỉ chứa comment đầu tiên
            List<Comment> firstCommentList = Collections.singletonList(commentsForRating.get(0));
            // Sử dụng setComments để cập nhật adapter
            commentAdapter.setComments(firstCommentList);

        } else {
            // Không có bình luận
            textViewCommentsTitle.setVisibility(View.GONE);
            buttonViewAllComments.setVisibility(View.GONE); // Ẩn nút "Xem tất cả"
            recyclerViewComments.setVisibility(View.GONE);
            // Xóa comment cũ trong adapter
            commentAdapter.setComments(new ArrayList<>());
        }
    }

    // Hàm xử lý khi nhấn nút Add to Cart
    private void handleAddToCartClick() {
        if (currentProductId != -1) {
            Toast.makeText(this, "Thêm sản phẩm " + currentProductId + " vào giỏ", Toast.LENGTH_SHORT).show();
            int quantityToAdd = 1;
            addProductToCart(currentProductId, quantityToAdd);
        } else {
            Log.e(TAG, "Add to Cart Clicked: Invalid Product ID.");
            Toast.makeText(this, "Lỗi: Không thể thêm sản phẩm không hợp lệ.", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm xử lý khi nhấn nút "Xem tất cả" bình luận
    private void openProductRatingActivity() {
        // Kiểm tra xem có ID sản phẩm và đối tượng sản phẩm hợp lệ không
        if (currentProductId != -1 && currentProduct != null) {
            Log.d(TAG, "Opening ProductRatingActivity for Product ID: " + currentProductId);
            Intent intent = new Intent(this, ProductRatingActivity.class);

            // ===> TRUYỀN DỮ LIỆU SANG ProductRatingActivity <===
            intent.putExtra("product_id", currentProductId); // ID là bắt buộc


            // Truyền các thông tin cơ bản của sản phẩm
            intent.putExtra("product_name", currentProduct.getName());
            List<String> imgs = currentProduct.getImageUrls();
            if (imgs != null && !imgs.isEmpty()) {
                intent.putExtra("product_image_url", imgs.get(0)); // Gửi ảnh đầu tiên
            }

            // Tính toán và truyền rating hiện tại
            List<Comment> comments = currentProduct.getComments();
            int currentRatingCount = 0;
            float currentAverageRating = 0f;
            if (comments != null && !comments.isEmpty()) {
                currentRatingCount = comments.size();
                int totalRating = 0;
                for (Comment c : comments) {
                    totalRating += c.getRating();
                }
                currentAverageRating = (float) totalRating / currentRatingCount;
            }
            intent.putExtra("product_average_rating", currentAverageRating);
            intent.putExtra("product_rating_count", currentRatingCount);

            // Truyền User ID nếu có
            if(userId != -1) {
                intent.putExtra("user_id", userId);
            }

            startActivity(intent);
        } else {
            // Xử lý trường hợp không thể mở (chưa load xong hoặc lỗi)
            Log.e(TAG, "Cannot open ratings. Product ID invalid or product data not loaded.");
            Toast.makeText(this, "Lỗi: Không thể xem đánh giá lúc này. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
        }
    }


    // Hiển thị/ẩn ProgressBar loading và nội dung/lỗi tương ứng
    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (isLoading) {
            showContent(false); // Ẩn nội dung khi đang tải
            showError(null);    // Ẩn lỗi cũ
        }
        // Việc hiển thị lại nội dung sẽ do onResponse xử lý
    }

    // Hiển thị/ẩn thông báo lỗi
    private void showError(String message) {
        if (textViewError != null) {
            if (message != null) {
                textViewError.setText(message);
                textViewError.setVisibility(View.VISIBLE);
                showContent(false); // Ẩn nội dung khi có lỗi
            } else {
                textViewError.setVisibility(View.GONE);
            }
        }
    }

    // Hiển thị/ẩn nội dung chính (sử dụng Group) và FAB
    private void showContent(boolean show) {
        // Đảm bảo các view đã được ánh xạ trước khi thay đổi visibility
        if (groupProductContent != null) {
            groupProductContent.setVisibility(show ? View.VISIBLE : View.GONE);
        } else {
            Log.e(TAG, "groupProductContent is null in showContent!");
        }
        if (fabAddToCart != null) {
            fabAddToCart.setVisibility(show ? View.VISIBLE : View.GONE);
        } else {
            Log.e(TAG, "fabAddToCart is null in showContent!");
        }
        fabAddToCart.setVisibility(show ? View.VISIBLE : View.GONE);


    }

    private void addProductToCart(long productId, int quantity) {
        if (apiService == null) {
            Log.e(TAG, "ApiService is not initialized!");
            Toast.makeText(this, "Lỗi dịch vụ, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true); // Hiện loading nếu muốn

        CartActionRequest request = new CartActionRequest(productId, quantity);
        String headerToken = "Bearer " + authToken;

        apiService.addToCart(headerToken, request)
                .enqueue(new Callback<Cart>() {
                    @Override
                    public void onResponse(@NonNull Call<Cart> call, @NonNull Response<Cart> response) {
                        showLoading(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(ProductDetailActivity.this, "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "Product added to cart successfully.");
                        } else {
                            String errorMsg = "Lỗi khi thêm vào giỏ hàng.";
                            try {
                                if (response.errorBody() != null) {
                                    errorMsg = response.errorBody().string();
                                    if (response.code() == 400) {
                                        Toast.makeText(ProductDetailActivity.this, "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                                    } else if (response.code() == 401 || response.code() == 403) {
                                        Toast.makeText(ProductDetailActivity.this, "Phiên đăng nhập hết hạn.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ProductDetailActivity.this, "Lỗi máy chủ: " + response.code(), Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(ProductDetailActivity.this, errorMsg + " (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                                }
                                Log.e(TAG, "Failed to add to cart: " + response.code() + " - " + errorMsg);

                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error body", e);
                                Toast.makeText(ProductDetailActivity.this, "Lỗi không xác định khi thêm vào giỏ.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Cart> call, @NonNull Throwable t) {
                        showLoading(false); // Ẩn loading
                        Log.e(TAG, "API call failed (addToCart): ", t);
                        Toast.makeText(ProductDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}