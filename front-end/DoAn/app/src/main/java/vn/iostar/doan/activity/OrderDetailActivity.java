package vn.iostar.doan.activity; // <<<< THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN

import android.annotation.SuppressLint;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import vn.iostar.doan.R; // <<<< Đảm bảo R đúng
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.fragment.OrderListFragment; // Để dùng hằng ACTION_SELECT_PRODUCT_FOR_REVIEW
import vn.iostar.doan.model.Order;
import vn.iostar.doan.model.OrderLine;
import vn.iostar.doan.model.OrderStatus;
import vn.iostar.doan.model.Product;
import vn.iostar.doan.model.Product2;

public class OrderDetailActivity extends AppCompatActivity {

    private static final String TAG = "OrderDetailActivity";
    public static final String EXTRA_ORDER_ID = "ORDER_ID"; // Key để nhận Order ID

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

    private Gson gson;
    private NumberFormat currencyFormatter;
    private SimpleDateFormat displayDateFormat;

    private long orderId = -1;
    private String actionMode = null; // Lưu action: null (xem chi tiết) hoặc ACTION_SELECT_PRODUCT_FOR_REVIEW
    private Long userIdForReview = -1L; // Lưu userId nếu action là review

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1L);
        if (getIntent().hasExtra("ACTION_MODE")) {
            actionMode = getIntent().getStringExtra("ACTION_MODE");
        }
        if (getIntent().hasExtra("USER_ID_FOR_REVIEW")) {
            userIdForReview = getIntent().getLongExtra("USER_ID_FOR_REVIEW", -1L);
            Log.d(TAG, "Received USER_ID_FOR_REVIEW: " + userIdForReview);
        }

        if (orderId == -1) {
            Log.e(TAG, "Order ID not passed correctly via Intent.");
            Toast.makeText(this, "Lỗi: Không tìm thấy mã đơn hàng.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "Received Order ID: " + orderId + ", ActionMode: " + actionMode + ", UserID for Review: " + userIdForReview);

        gson = new GsonBuilder().create();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        displayDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        setupViews();
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (OrderListFragment.ACTION_SELECT_PRODUCT_FOR_REVIEW.equals(actionMode)) {
                getSupportActionBar().setTitle("Chọn sản phẩm để đánh giá");
            } else {
                getSupportActionBar().setTitle("Đơn hàng #" + orderId);
            }
        }

        fetchOrderDetailWithRetrofit(orderId);
    }

    private void setupViews() {
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
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchOrderDetailWithRetrofit(long currentOrderId) {
        showLoading(true);
        tvErrorMessage.setVisibility(View.GONE);
        scrollViewContent.setVisibility(View.GONE);

        // TODO: Add Authentication Token if API requires
        Call<Order> call = ApiService.apiService.getOrderDetail(currentOrderId);
        Log.d(TAG, "Calling API: " + call.request().url());

        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Order orderDetail = response.body();
                    Log.d(TAG, "API call successful. Order received: " + orderDetail.getOrderId());
                    displayOrderDetails(orderDetail);
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
        if (order == null) {
            showError("Không có dữ liệu chi tiết cho đơn hàng này.");
            return;
        }

        // Thông tin chung
        tvDetailOrderId.setText("Mã ĐH: #" + order.getOrderId());
        tvDetailPaymentMethod.setText("Thanh toán: " + (order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : "N/A")); // Ensure PaymentMethod has toString()
        tvDetailOrderDate.setText("Ngày đặt: " + formatDate(order.getOrderDate()));
        tvDetailPredictDate.setText("Dự kiến nhận: " + formatDate(order.getPredictReceiveDate()));
        tvDetailPredictDate.setVisibility(order.getPredictReceiveDate() != null ? View.VISIBLE : View.GONE);
        OrderStatus status = order.getStatus();
        String statusText = getStatusDisplayString(status);
        int statusColor = getStatusColor(status);
        tvDetailOrderStatus.setText(statusText);
        tvDetailOrderStatus.setTextColor(statusColor);

        // Địa chỉ
        tvDetailShippingAddress.setText(order.getShippingAddress() != null ? order.getShippingAddress() : "Không có thông tin địa chỉ.");

        // Danh sách sản phẩm
        llOrderDetailItems.removeAllViews();
        if (order.getOrderLines() != null && !order.getOrderLines().isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < order.getOrderLines().size(); i++) {
                OrderLine line = order.getOrderLines().get(i);
                Product product = line.getProduct(); // Model Product2

                View itemView = inflater.inflate(R.layout.list_item_order_detail_product, llOrderDetailItems, false);
                ImageView ivProduct = itemView.findViewById(R.id.iv_product_image_detail);
                TextView tvName = itemView.findViewById(R.id.tv_product_name_detail);
                TextView tvQuantity = itemView.findViewById(R.id.tv_product_quantity_detail);
                TextView tvPrice = itemView.findViewById(R.id.tv_product_price_detail);
                Button btnEvaluateThisProduct = itemView.findViewById(R.id.btn_evaluate_this_product);

                if (product != null) {
                    tvName.setText(product.getName());
                    // Giả định Product2 có getImageUrl() trả về URL ảnh
                    Glide.with(this)
                            .load(product.getImages())
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .into(ivProduct);

                    // Hiển thị nút "Đánh giá" nếu đúng action mode và trạng thái
                    if (OrderListFragment.ACTION_SELECT_PRODUCT_FOR_REVIEW.equals(actionMode) &&
                            order.getStatus() == OrderStatus.RECEIVED &&
                            userIdForReview > 0L) { // Kiểm tra userIdForReview hợp lệ

                        btnEvaluateThisProduct.setVisibility(View.VISIBLE);
                        // Giả định Product2 có getProductId()
                        final long currentProductId = product.getProductId();
                        btnEvaluateThisProduct.setOnClickListener(v -> {
                            Log.d(TAG, "Evaluate this product clicked. ProductId: " + currentProductId + ", UserId: " + userIdForReview);
                            Intent reviewIntent = new Intent(OrderDetailActivity.this, ProductRatingActivity.class);
                            reviewIntent.putExtra("product_id", currentProductId); // Key cho ProductRatingActivity
                            reviewIntent.putExtra("user_id", userIdForReview);    // Truyền userId
                            startActivity(reviewIntent);
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
                tvPrice.setText(currencyFormatter.format(line.getPrice())); // OrderLine cần getPrice()
                llOrderDetailItems.addView(itemView);

                if (i < order.getOrderLines().size() - 1) {
                    View divider = new View(this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (int) getResources().getDimension(R.dimen.divider_height)
                    );
                    params.setMargins(0, (int) getResources().getDimension(R.dimen.divider_margin), 0, (int) getResources().getDimension(R.dimen.divider_margin));
                    divider.setLayoutParams(params);
                    divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider_color));
                    llOrderDetailItems.addView(divider);
                }
            }
        } else {
            TextView noItemsText = new TextView(this);
            noItemsText.setText("Không có sản phẩm trong đơn hàng này.");
            llOrderDetailItems.addView(noItemsText);
        }

        // Tóm tắt đơn hàng
        tvDetailItemsSubtotal.setText(currencyFormatter.format(order.getItemsSubtotal()));
        // Tính phí ship = total - subtotal, hoặc dùng getter nếu có
        double shippingFee = order.getTotalPrice() - order.getItemsSubtotal() - order.getDiscountAmount();
        if (order.getShippingFee() > 0){ // Ưu tiên getter nếu có
            shippingFee = order.getShippingFee();
        }
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
        if (status == null) return "N/A";
        switch (status) {
            case WAITING: return "Đang chờ xác nhận";
            case REVIEWED: return "Đã xác nhận";
            case SHIPPING: return "Đang vận chuyển";
            case RECEIVED: return "Đã giao thành công";
            case ERROR: return "Đã hủy";
            default: return status.name();
        }
    }

    private int getStatusColor(OrderStatus status) {
        int colorResId = R.color.my_grey_neutral;
        if (status != null) {
            switch (status) {
                case RECEIVED: colorResId = R.color.my_green_success; break;
                case SHIPPING: colorResId = R.color.my_orange_processing; break;
                case ERROR:    colorResId = R.color.my_red_error; break;
            }
        }
        return ContextCompat.getColor(this, colorResId);
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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