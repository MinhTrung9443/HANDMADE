package vn.iostar.doan.activity; // <<<< THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; // <<<< THÊM IMPORT
import androidx.activity.result.contract.ActivityResultContracts; // <<<< THÊM IMPORT
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import vn.iostar.doan.R;
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.model.Order;
import vn.iostar.doan.model.OrderLine;
import vn.iostar.doan.model.OrderStatus;
import vn.iostar.doan.model.Product;


public class OrderDetailActivity extends AppCompatActivity {

    private static final String TAG = "OrderDetailActivity";

    public static final String EXTRA_ORDER_ID = "ORDER_ID_KEY";
    public static final String EXTRA_ACTION_MODE = "ACTION_MODE_KEY";
    public static final String EXTRA_USER_ID_FOR_REVIEW = "USER_ID_FOR_REVIEW_KEY";
    public static final String ACTION_MODE_SELECT_PRODUCT_FOR_REVIEW = "SELECT_PRODUCT_FOR_REVIEW_ACTION";

    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView tvErrorMessage;
    private ScrollView scrollViewContent;
    private TextView tvDetailOrderId, tvDetailOrderStatus, tvDetailOrderDate, tvDetailPredictDate, tvDetailPaymentMethod;
    private TextView tvDetailShippingAddress;
    private LinearLayout llOrderDetailItems;
    private TextView tvDetailItemsSubtotal, tvDetailShippingFee, tvDetailDiscountAmount, tvDetailTotalPrice;
    private RelativeLayout layoutDiscount;
    private TextView tvDetailDiscountLabel;

    private NumberFormat currencyFormatter;
    private SimpleDateFormat displayDateFormat;

    private long orderId = -1;
    private String currentActionMode = null;
    private Long userIdForReview = -1L;
    private Order currentOrderData;

    // <<<< KHAI BÁO LAUNCHER CHO ProductRatingActivity >>>>
    private ActivityResultLauncher<Intent> productRatingLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        Intent intent = getIntent();
        orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1L);
        if (intent.hasExtra(EXTRA_ACTION_MODE)) {
            currentActionMode = intent.getStringExtra(EXTRA_ACTION_MODE);
        }
        if (intent.hasExtra(EXTRA_USER_ID_FOR_REVIEW)) {
            userIdForReview = intent.getLongExtra(EXTRA_USER_ID_FOR_REVIEW, -1L);
        }

        if (orderId == -1) {
            Log.e(TAG, "Order ID not passed correctly via Intent.");
            Toast.makeText(this, "Lỗi: Không tìm thấy mã đơn hàng.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "Received Order ID: " + orderId + ", ActionMode: " + currentActionMode + ", UserID for Review: " + userIdForReview);

        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        displayDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        // <<<< KHỞI TẠO LAUNCHER >>>>
        initProductRatingLauncher();

        setupViews();
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (ACTION_MODE_SELECT_PRODUCT_FOR_REVIEW.equals(currentActionMode)) {
                getSupportActionBar().setTitle("Chọn sản phẩm để đánh giá");
            } else {
                getSupportActionBar().setTitle("Đơn hàng #" + orderId);
            }
        }

        if (savedInstanceState == null || currentOrderData == null) {
            fetchOrderDetailWithRetrofit(orderId);
        } else {
            fetchOrderDetailWithRetrofit(orderId);
        }
    }

    // <<<< HÀM KHỞI TẠO LAUNCHER >>>>
    private void initProductRatingLauncher() {
        productRatingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Callback này được gọi khi ProductRatingActivity kết thúc
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Người dùng đã đánh giá thành công
                        Log.d(TAG, "ProductRatingActivity returned RESULT_OK.");
                        // Đặt kết quả cho OrderHistoryActivity và đóng OrderDetailActivity
                        setResult(Activity.RESULT_OK);
                        finish(); // Đóng OrderDetailActivity để OrderHistoryActivity nhận kết quả
                    } else {
                        // Người dùng hủy hoặc có lỗi từ ProductRatingActivity
                        Log.d(TAG, "ProductRatingActivity returned code: " + result.getResultCode() + ". Setting RESULT_CANCELED for OrderHistoryActivity.");
                        // Nếu ProductRatingActivity bị hủy, chúng ta cũng nên báo là CANCELED cho OrderHistoryActivity
                        setResult(Activity.RESULT_CANCELED);
                        finish(); // Đóng OrderDetailActivity
                    }
                }
        );
    }


    private void setupViews() {
        // ... (giữ nguyên)
        toolbar = findViewById(R.id.toolbar_order_detail);
        progressBar = findViewById(R.id.progress_bar_order_detail);
        tvErrorMessage = findViewById(R.id.tv_error_message_order_detail);
        scrollViewContent = findViewById(R.id.scroll_view_order_detail);
        tvDetailOrderId = findViewById(R.id.tv_detail_order_id);
        tvDetailOrderStatus = findViewById(R.id.tv_detail_order_status);
        tvDetailOrderDate = findViewById(R.id.tv_detail_order_date);
        tvDetailPredictDate = findViewById(R.id.tv_detail_predict_date);
        tvDetailPaymentMethod = findViewById(R.id.tv_detail_payment_method);
        tvDetailShippingAddress = findViewById(R.id.tv_detail_shipping_address);
        llOrderDetailItems = findViewById(R.id.ll_order_detail_items);
        tvDetailItemsSubtotal = findViewById(R.id.tv_detail_items_subtotal);
        tvDetailShippingFee = findViewById(R.id.tv_detail_shipping_fee);
        tvDetailDiscountAmount = findViewById(R.id.tv_detail_discount_amount);
        tvDetailTotalPrice = findViewById(R.id.tv_detail_total_price);
        layoutDiscount = findViewById(R.id.layout_discount);
        tvDetailDiscountLabel = findViewById(R.id.tv_detail_discount_label);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (ACTION_MODE_SELECT_PRODUCT_FOR_REVIEW.equals(currentActionMode)) {
                setResult(Activity.RESULT_CANCELED); // Khi back từ mode chọn sản phẩm, coi như hủy
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchOrderDetailWithRetrofit(long currentOrderIdToFetch) {
        // ... (giữ nguyên)
        showLoading(true);
        tvErrorMessage.setVisibility(View.GONE);
        scrollViewContent.setVisibility(View.GONE);

        Call<Order> call = ApiService.apiService.getOrderDetail(currentOrderIdToFetch);
        Log.d(TAG, "Calling API for order detail: " + call.request().url());

        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    currentOrderData = response.body();
                    Log.d(TAG, "API call successful. Order received: " + currentOrderData.getOrderId());
                    displayOrderDetails(currentOrderData);
                    scrollViewContent.setVisibility(View.VISIBLE);
                } else {
                    String errorMsg = "Lỗi " + response.code() + ": Không thể tải chi tiết đơn hàng.";
                    try {
                        if (response.errorBody() != null) {
                            String errorBodyString = response.errorBody().string();
                            if (!errorBodyString.isEmpty() && !errorBodyString.startsWith("<")) {
                                errorMsg = "Lỗi " + response.code() + ": " + errorBodyString.substring(0, Math.min(errorBodyString.length(), 100));
                            }
                            Log.e(TAG, "API call failed with code " + response.code() + ". Error Body: " + errorBodyString);
                        } else {
                            Log.e(TAG, "API call failed with code " + response.code() + ". No error body.");
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "API call failed on network/exception: ", t);
                showError("Lỗi kết nối mạng. Vui lòng thử lại.");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void displayOrderDetails(Order order) {
        // ... (phần đầu giữ nguyên) ...
        if (order == null) {
            showError("Không có dữ liệu chi tiết cho đơn hàng này.");
            return;
        }

        tvDetailOrderId.setText("Mã ĐH: #" + order.getOrderId());
        tvDetailPaymentMethod.setText("Thanh toán: " + (order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : "N/A"));
        tvDetailOrderDate.setText("Ngày đặt: " + formatDate(order.getOrderDate()));
        tvDetailPredictDate.setText("Dự kiến nhận: " + formatDate(order.getPredictReceiveDate()));
        tvDetailPredictDate.setVisibility(order.getPredictReceiveDate() != null ? View.VISIBLE : View.GONE);

        OrderStatus status = order.getStatus();
        String statusText = getStatusDisplayString(status);
        int statusColor = getStatusColor(status);
        tvDetailOrderStatus.setText(statusText);
        tvDetailOrderStatus.setTextColor(statusColor);

        tvDetailShippingAddress.setText(order.getShippingAddress() != null ? order.getShippingAddress() : "Không có thông tin địa chỉ.");

        llOrderDetailItems.removeAllViews();
        if (order.getOrderLines() != null && !order.getOrderLines().isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < order.getOrderLines().size(); i++) {
                OrderLine line = order.getOrderLines().get(i);
                Product product = line.getProduct();

                View itemView = inflater.inflate(R.layout.list_item_order_detail_product, llOrderDetailItems, false);
                ImageView ivProduct = itemView.findViewById(R.id.iv_product_image_detail);
                TextView tvName = itemView.findViewById(R.id.tv_product_name_detail);
                TextView tvQuantity = itemView.findViewById(R.id.tv_product_quantity_detail);
                TextView tvPrice = itemView.findViewById(R.id.tv_product_price_detail);
                Button btnEvaluateThisProduct = itemView.findViewById(R.id.btn_evaluate_this_product);

                if (product != null) {
                    tvName.setText(product.getName());
                    Glide.with(this)
                            .load(product.getImage())
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .into(ivProduct);

                    if (ACTION_MODE_SELECT_PRODUCT_FOR_REVIEW.equals(currentActionMode) &&
                            order.getStatus() == OrderStatus.RECEIVED &&
                            userIdForReview > 0L) {

                        btnEvaluateThisProduct.setVisibility(View.VISIBLE);
                        final long currentSelectedProductId = product.getProductId();

                        btnEvaluateThisProduct.setOnClickListener(v -> {
                            Log.d(TAG, "Evaluate this product clicked. ProductId: " + currentSelectedProductId +
                                    ", UserId: " + userIdForReview + ", OrderId: " + order.getOrderId());

                            Intent intentToProductRating = new Intent(OrderDetailActivity.this, ProductRatingActivity.class);
                            intentToProductRating.putExtra("product_id", currentSelectedProductId);
                            intentToProductRating.putExtra("user_id", userIdForReview);
                            intentToProductRating.putExtra("order_id", order.getOrderId());

                            // <<<< SỬ DỤNG productRatingLauncher ĐỂ MỞ ProductRatingActivity >>>>
                            if (productRatingLauncher != null) {
                                productRatingLauncher.launch(intentToProductRating);
                            } else {
                                Log.e(TAG, "productRatingLauncher is null! Cannot launch ProductRatingActivity for result.");
                                // Fallback (không khuyến khích vì mất luồng kết quả)
                                startActivity(intentToProductRating);
                                // Nếu fallback, phải setResult và finish ở đây để OrderHistoryActivity không bị treo chờ
                                setResult(Activity.RESULT_CANCELED); // Coi như không hoàn thành đúng luồng
                                finish();
                            }
                            // <<<< KHÔNG GỌI setResult VÀ finish Ở ĐÂY NỮA >>>>
                        });
                    } else {
                        btnEvaluateThisProduct.setVisibility(View.GONE);
                    }
                } else {
                    tvName.setText("Sản phẩm không xác định");
                    ivProduct.setImageResource(R.drawable.placeholder_image);
                    btnEvaluateThisProduct.setVisibility(View.GONE);
                }

                tvQuantity.setText("SL: x" + line.getQuantity());
                tvPrice.setText(currencyFormatter.format(line.getPrice()));
                llOrderDetailItems.addView(itemView);

                if (i < order.getOrderLines().size() - 1) {
                    // ... (code thêm divider như cũ) ...
                    View divider = new View(this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (int) getResources().getDimension(R.dimen.divider_height)
                    );
                    params.setMargins(0, (int) getResources().getDimension(R.dimen.divider_margin_vertical),
                            0, (int) getResources().getDimension(R.dimen.divider_margin_vertical));
                    divider.setLayoutParams(params);
                    divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider_color));
                    llOrderDetailItems.addView(divider);
                }
            }
        } else {
            // ... (code hiển thị "không có sản phẩm" như cũ) ...
            TextView noItemsText = new TextView(this);
            noItemsText.setText("Không có sản phẩm trong đơn hàng này.");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 16,0,16);
            noItemsText.setLayoutParams(params);
            llOrderDetailItems.addView(noItemsText);
        }

        // ... (code hiển thị tóm tắt đơn hàng như cũ) ...
        tvDetailItemsSubtotal.setText(currencyFormatter.format(order.getItemsSubtotal()));
        double shippingFee = order.getShippingFee();
        tvDetailShippingFee.setText(currencyFormatter.format(shippingFee));

        if (order.getDiscountAmount() > 0) {
            layoutDiscount.setVisibility(View.VISIBLE);
            String discountLabel = "Giảm giá";
            if (order.getVoucherCode() != null && !order.getVoucherCode().isEmpty()) {
                discountLabel += " (" + order.getVoucherCode() + ")";
            }
            tvDetailDiscountLabel.setText(discountLabel + ":");
            tvDetailDiscountAmount.setText("- " + currencyFormatter.format(order.getDiscountAmount()));
        } else {
            layoutDiscount.setVisibility(View.GONE);
        }
        tvDetailTotalPrice.setText(currencyFormatter.format(order.getTotalPrice()));
    }

    // ... (formatDate, getStatusDisplayString, getStatusColor, showLoading, showError giữ nguyên) ...
    private String formatDate(Date date) {
        if (date == null) return "N/A";
        try {
            return displayDateFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + date, e);
            return "Lỗi ngày";
        }
    }

    private String getStatusDisplayString(OrderStatus status) {
        if (status == null) return "Không xác định";
        switch (status) {
            case WAITING: return "Đang chờ xử lý";
            case REVIEWED: return "Đã xác nhận";
            case SHIPPING: return "Đang vận chuyển";
            case RECEIVED: return "Đã giao thành công";
            case CANCELLED: return "Đã hủy";
            case ERROR: return "Lỗi đơn hàng";
            default: return status.name();
        }
    }

    private int getStatusColor(OrderStatus status) {
        int colorResId = R.color.my_grey_neutral;
        if (status != null) {
            switch (status) {
                case RECEIVED: colorResId = R.color.my_green_success; break;
                case SHIPPING: colorResId = R.color.my_orange_processing; break;
                case CANCELLED:
                case ERROR:    colorResId = R.color.my_red_error; break;
                case WAITING:
                case REVIEWED: colorResId = R.color.my_blue_info; break;
            }
        }
        return ContextCompat.getColor(this, colorResId);
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (isLoading) {
            if (scrollViewContent != null) scrollViewContent.setVisibility(View.GONE);
            if (tvErrorMessage != null) tvErrorMessage.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (scrollViewContent != null) {
            scrollViewContent.setVisibility(View.GONE);
        }
        if (tvErrorMessage != null) {
            if (message != null && !message.isEmpty()) {
                tvErrorMessage.setText(message);
                tvErrorMessage.setVisibility(View.VISIBLE);
            } else {
                tvErrorMessage.setVisibility(View.GONE);
            }
        }
    }
}