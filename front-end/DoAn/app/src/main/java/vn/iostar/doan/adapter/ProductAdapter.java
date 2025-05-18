package vn.iostar.doan.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.text.NumberFormat;
import java.util.ArrayList; // Import ArrayList
import java.util.List;
import java.util.Locale;

import vn.iostar.doan.R;
import vn.iostar.doan.activity.ProductDetailActivity;
import vn.iostar.doan.model.Product;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private static final String TAG = "ProductAdapter"; // Thêm TAG để log
    private Context context;
    private List<Product> productList;
    private String token; // Lưu token
    private long userId;  // Lưu userId

    private NumberFormat currencyFormatter;

    // --- Constructor duy nhất nhận tất cả dữ liệu cần thiết ---
    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    }
    public ProductAdapter(Context context, List<Product> productList, String token) {
        this.context = context;
        this.productList = productList;
        this.token = token; // <<< Lưu token
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    }
    public ProductAdapter(Context context, List<Product> productList, String token, long userId) {
        this.context = context;
        // Khởi tạo productList nếu nó null để tránh lỗi ở getItemCount hoặc onBindViewHolder
        this.productList = (productList != null) ? productList : new ArrayList<>();
        this.token = token;
        this.userId = userId;
        // KHỞI TẠO currencyFormatter
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng context đã lưu để lấy LayoutInflater
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }
    public void updateList(List<Product> newList) {
        productList = newList;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        // Gọi hàm bind trong ViewHolder để gán dữ liệu
        // Truyền cả token và userId vào hàm bind
        holder.bind(product, currencyFormatter, token, userId);
    }

    @Override
    public int getItemCount() {
        // Không cần kiểm tra null ở đây nếu đã khởi tạo trong constructor
        return productList.size();
    }

    // (Tùy chọn) Phương thức để cập nhật dữ liệu cho adapter
    @SuppressLint("NotifyDataSetChanged")
    public void updateProducts(List<Product> newProductList) {
        this.productList.clear();
        if (newProductList != null) {
            this.productList.addAll(newProductList);
        }
        Log.d(TAG, "Adapter updated with " + this.productList.size() + " items.");
        notifyDataSetChanged(); // Thông báo thay đổi dữ liệu
    }


    // --- ViewHolder ---
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Đảm bảo khai báo đúng các View bạn có trong item_product.xml
        ImageView imageView;
        TextView tvProductName, tvProductPrice;
        // Bỏ các TextView khác nếu không dùng hoặc chưa ánh xạ
        // TextView tvProductDescription, tvProductCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ View từ layout item_product.xml
            imageView = itemView.findViewById(R.id.imageView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);

            // Kiểm tra null (tùy chọn, nhưng tốt)
            if (imageView == null || tvProductName == null || tvProductPrice == null) {
                Log.e(TAG, "ViewHolder: Một hoặc nhiều View không được tìm thấy trong layout item_product.xml!");
            }
        }

        // --- Phương thức Bind Data (Quan trọng) ---
        // Thêm token và userId vào tham số
        public void bind(final Product product, NumberFormat formatter, final String currentToken, final long currentUserId) {
            if (product == null) {
                // Xử lý trường hợp product null nếu cần
                Log.w(TAG, "bind called with null product.");
                return;
            }

            // Gắn dữ liệu text
            tvProductName.setText(product.getName() != null ? product.getName() : "No Name");

            // Định dạng giá tiền và gắn vào TextView
            // Sử dụng formatter từ tham số
            tvProductPrice.setText(formatter.format(product.getPrice()));

            // === SỬ DỤNG GLIDE ĐỂ TẢI ẢNH ===
            String imageUrlOrPath = product.getImage(); // Lấy đường dẫn/tên file ảnh dạng String

            // Tạo tùy chọn cho ảnh placeholder và lỗi
            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.ic_launcher_foreground) // Sử dụng lại placeholder từ file 1
                    .error(R.drawable.edit);          // Sử dụng lại ảnh lỗi từ file 1

            if (imageUrlOrPath != null && !imageUrlOrPath.isEmpty()) {
                String finalImageUrl;
                // Kiểm tra xem có phải URL đầy đủ không
                if (imageUrlOrPath.startsWith("http://") || imageUrlOrPath.startsWith("https://")) {
                    finalImageUrl = imageUrlOrPath; // Là URL đầy đủ
                } else {
                    // Nếu chỉ là tên file, ghép với Base URL ảnh
                    // *** THAY ĐỔI URL NÀY CHO ĐÚNG VỚI BACKEND CỦA BẠN ***
                    // Đây là URL ví dụ, bạn cần lấy URL chính xác từ cấu hình ứng dụng hoặc API
                    String BASE_IMAGE_URL = "http://10.0.2.2:8080/api/v1/images/";
                    finalImageUrl = BASE_IMAGE_URL + imageUrlOrPath;
                }

                // Dùng Glide để tải ảnh
                Glide.with(itemView.getContext()) // Lấy context từ itemView là tốt nhất
                        .load(finalImageUrl)      // Tải từ URL đã xử lý
                        .apply(requestOptions)    // Áp dụng placeholder/error
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Bật cache (tùy chọn)
                        .into(imageView);         // Hiển thị vào ImageView

            } else {
                // Không có đường dẫn ảnh -> hiển thị ảnh lỗi/placeholder
                 Log.d(TAG, "No image URL for product ID: " + product.getProductId() + ", setting default image.");
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_launcher_foreground) // Hoặc R.drawable.edit
                        .into(imageView);
            }

            // --- Thiết lập OnClickListener cho toàn bộ item ---
            itemView.setOnClickListener(v -> {
                Log.d(TAG, "Clicked product: " + product.getProductId());
                Intent intent = new Intent(context, ProductDetailActivity.class);
                intent.putExtra("productId", product.getProductId()); // Truyền ID sản phẩm (sử dụng key "productId")
                intent.putExtra("userId", currentUserId);           // Truyền userId
                intent.putExtra("token", currentToken);             // Truyền token
                context.startActivity(intent);
            });
            // -----------------------------------------------
        }
        // ------------------------------------
    }
}