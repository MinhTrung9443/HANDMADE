package vn.iostar.doan.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View; // Import View cho showLoading
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull; // Import cho @NonNull
import androidx.appcompat.app.AppCompatActivity;
//import androidx.browser.customtabs.CustomTabsIntent; // Không dùng ở đây, có thể bỏ
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.iostar.doan.R;
import vn.iostar.doan.adapter.CheckoutItemsAdapter;
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.databinding.ActivityOrderBinding;
import vn.iostar.doan.model.Address;
import vn.iostar.doan.model.Order;
import vn.iostar.doan.model.OrderStatus; // *** IMPORT OrderStatus ***
import vn.iostar.doan.model.PaymentMethod;
import vn.iostar.doan.model.SelectedItemDetail;
import vn.iostar.doan.modelRequest.CartItemDetailsRequest;
import vn.iostar.doan.modelRequest.CreateOrderRequest;
import vn.iostar.doan.modelResponse.CreateOrderResponseDTO;

public class OrderActivity extends AppCompatActivity {

    private ActivityOrderBinding binding;
    private ApiService apiService;
    private String authToken;
    private ArrayList<Long> selectedItemIds;
    private CheckoutItemsAdapter checkoutItemsAdapter;
    private Address defaultAddress; // Lưu địa chỉ mặc định
    private List<SelectedItemDetail> checkoutItems;
    private PaymentMethod selectedPaymentMethodEnum = PaymentMethod.COD;
    private Long pendingVnpayOrderId = null; // Biến lưu ID đơn VNPAY đang chờ thanh toán

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    public static final String EXTRA_ORDER_ID = "ORDER_ID";

    private int apiCallsCompleted = 0;
    private final int TOTAL_API_CALLS = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiService.apiService; // Lấy instance của ApiService
        if (!receiveIntentData()) {
            return;
        }

        setupToolbar();
        setupRecyclerView();
        setupPaymentMethods();
        setupPlaceOrderButton();

        if (savedInstanceState != null) {
            pendingVnpayOrderId = savedInstanceState.getLong("pendingVnpayOrderId", -1L);
            if (pendingVnpayOrderId == -1L) {
                pendingVnpayOrderId = null; // Đặt về null nếu không tìm thấy ID hợp lệ
            }
            Log.d("OrderActivity", "Restored pendingVnpayOrderId: " + pendingVnpayOrderId);
        }

        fetchCheckoutData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingVnpayOrderId != null) {
            Log.i("PlaceOrder", "Activity resumed. Checking status for pending VNPAY order: " + pendingVnpayOrderId);
            showLoading(true);
            binding.btnPlaceOrder.setEnabled(false);

            String headerAuth = "Bearer " + authToken;
            apiService.getOrderDetails(headerAuth, pendingVnpayOrderId).enqueue(new Callback<Order>() {
                @Override
                public void onResponse(Call<Order> call, Response<Order> response) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        Order order = response.body();
                        Log.i("PlaceOrder", "Checked status for order " + pendingVnpayOrderId + ": " + (order != null && order.getStatus() != null ? order.getStatus().name() : "N/A"));

                        if (order != null && order.getStatus() != null) {
                            if (order.getStatus() == OrderStatus.REVIEWED || order.getStatus() == OrderStatus.SHIPPING || order.getStatus() == OrderStatus.DELIVERED) { // Các trạng thái thành công sau thanh toán
                                Toast.makeText(OrderActivity.this, "Thanh toán VNPAY thành công! Mã đơn: " + order.getOrderId(), Toast.LENGTH_LONG).show();
                                Intent successIntent = new Intent(OrderActivity.this, OrderDetailActivity.class); // Hoặc OrderHistoryActivity
                                successIntent.putExtra(EXTRA_ORDER_ID, order.getOrderId());
                                successIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(successIntent);
                                finish();
                            } else if (order.getStatus() == OrderStatus.CANCELLED ) {
                                Toast.makeText(OrderActivity.this, "Thanh toán VNPAY thất bại hoặc đã bị hủy cho đơn " + order.getOrderId() + ". Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                                binding.btnPlaceOrder.setEnabled(true);
                                pendingVnpayOrderId = null; // Reset state
                            } else if (order.getStatus() == OrderStatus.PENDING) {
                                Toast.makeText(OrderActivity.this, "Trạng thái đơn hàng VNPAY chưa được xác nhận. Vui lòng kiểm tra lại sau.", Toast.LENGTH_LONG).show();
                                binding.btnPlaceOrder.setEnabled(true);
                                pendingVnpayOrderId = null; // Reset state
                            } else if (order.getStatus() == OrderStatus.WAITING) {
                                Toast.makeText(OrderActivity.this, "Đơn hàng VNPAY đã được xác nhận. Mã đơn: " + order.getOrderId(), Toast.LENGTH_LONG).show();
                                Intent successIntent = new Intent(OrderActivity.this, HomeActivity.class); // Hoặc OrderHistoryActivity
                                successIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(successIntent);
                                finish();

                            }
                            else {
                                Toast.makeText(OrderActivity.this, "Đơn hàng có trạng thái không mong muốn: " + order.getStatus().name() + ". Vui lòng liên hệ hỗ trợ.", Toast.LENGTH_LONG).show();
                                binding.btnPlaceOrder.setEnabled(true);
                                pendingVnpayOrderId = null; // Reset
                            }
                        } else {
                            Log.e("PlaceOrderError", "Order object or status is null in status check response.");
                            Toast.makeText(OrderActivity.this, "Lỗi kiểm tra trạng thái đơn hàng.", Toast.LENGTH_SHORT).show();
                            binding.btnPlaceOrder.setEnabled(true);
                            pendingVnpayOrderId = null;
                        }

                    } else { // API check status lỗi (4xx, 5xx)
                        showLoading(false);
                        binding.btnPlaceOrder.setEnabled(true);
                        pendingVnpayOrderId = null;
                        Log.e("PlaceOrderError", "Failed to check order status after VNPAY resume: " + response.code() + " - " + response.message());
                        handleApiError(response);
                    }
                }

                @Override
                public void onFailure(Call<Order> call, Throwable t) {
                    showLoading(false);
                    binding.btnPlaceOrder.setEnabled(true);
                    pendingVnpayOrderId = null;
                    Log.e("PlaceOrderError", "Network error checking order status after VNPAY resume", t);
                    Toast.makeText(OrderActivity.this, "Lỗi mạng khi kiểm tra trạng thái đơn hàng.", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingVnpayOrderId != null) {
            outState.putLong("pendingVnpayOrderId", pendingVnpayOrderId);
            Log.d("OrderActivity", "Saving pendingVnpayOrderId: " + pendingVnpayOrderId);
        }
    }

    private void openPaymentUrl(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);

        } catch (Exception e) {
            Log.e("PlaceOrder", "Failed to open payment URL: " + url, e);
            Toast.makeText(this, "Không thể mở liên kết thanh toán: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showLoading(false);
            binding.btnPlaceOrder.setEnabled(true);
            pendingVnpayOrderId = null;
        }
    }

    private boolean receiveIntentData() {
        Intent intent = getIntent();
        authToken = intent.getStringExtra("authToken");

        if (intent.hasExtra("SELECTED_CART_ITEM_IDS")) {
            try {
                Object extra = intent.getSerializableExtra("SELECTED_CART_ITEM_IDS");
                if (extra instanceof ArrayList<?>) {
                    selectedItemIds = new ArrayList<>();
                    for (Object item : (ArrayList<?>) extra) {
                        if (item instanceof Long) {
                            selectedItemIds.add((Long) item);
                        } else if (item instanceof Integer) { // Đôi khi ArrayList<Integer> được gửi
                            selectedItemIds.add(((Integer) item).longValue());
                        }
                    }
                } else {
                    Log.e("OrderActivityError", "Intent extra SELECTED_CART_ITEM_IDS is not ArrayList.");
                }
            } catch (ClassCastException e) {
                Log.e("OrderActivityError", "Could not cast SELECTED_CART_ITEM_IDS extra", e);
            }
        }

        if (authToken == null || authToken.isEmpty()) {
            Log.e("OrderActivityError", "AuthToken is missing or empty.");
            Toast.makeText(this, "Lỗi xác thực người dùng.", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            Log.e("OrderActivityError", "Missing or empty selected item IDs from Intent.");
            Toast.makeText(this, "Không có sản phẩm nào được chọn.", Toast.LENGTH_SHORT).show();
            finish(); // Đóng activity
            return false;
        }

        Log.d("OrderActivityData", "AuthToken received: " + (authToken != null));
        Log.d("OrderActivityData", "Received item IDs: " + selectedItemIds.toString());
        return true;
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void fetchAndSetDefaultAddress(String headerAuth) {
        Log.d("OrderActivity", "Fetching user addresses...");
        apiService.getUserAddresses(headerAuth).enqueue(new Callback<List<Address>>() {
            @Override
            public void onResponse(Call<List<Address>> call, Response<List<Address>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Address> addresses = response.body();
                    Log.d("OrderActivity", "Addresses list received. Size: " + addresses.size());

                    Address foundDefault = null;
                    if (!addresses.isEmpty()) {
                        for (Address addr : addresses) {
                            if (addr != null && addr.isDefaultAddress()) {
                                foundDefault = addr;
                                Log.d("OrderActivity", "Default address found in the list. ID: " + addr.getAddressId());
                                break;
                            }
                        }
                    }

                    if (foundDefault != null) {
                        defaultAddress = foundDefault;
                        displayAddress(defaultAddress);
                        Log.d("OrderActivity", "Default address assigned and displayed.");
                    } else {
                        defaultAddress = null;
                        Log.w("OrderActivity", "Address list is not empty, but no default address found.");
                        binding.tvRecipientInfo.setText("Chưa có địa chỉ mặc định");
                        binding.tvDeliveryAddress.setText("Vui lòng chọn hoặc thêm địa chỉ giao hàng");
                        Toast.makeText(OrderActivity.this, "Vui lòng đặt một địa chỉ làm mặc định.", Toast.LENGTH_LONG).show();
                    }

                } else if (response.code() == 404) { // Có thể người dùng chưa có địa chỉ nào
                    defaultAddress = null;
                    Log.w("OrderActivity", "User has no addresses (404 response).");
                    binding.tvRecipientInfo.setText("Chưa có địa chỉ");
                    binding.tvDeliveryAddress.setText("Vui lòng thêm địa chỉ giao hàng");
                    Toast.makeText(OrderActivity.this, "Bạn chưa có địa chỉ nào. Vui lòng thêm địa chỉ.", Toast.LENGTH_LONG).show();
                }
                else {
                    defaultAddress = null;
                    Log.e("OrderActivityError", "Error fetching addresses: " + response.code() + " - " + response.message());
                    Toast.makeText(OrderActivity.this, "Lỗi tải danh sách địa chỉ.", Toast.LENGTH_SHORT).show();
                    binding.tvRecipientInfo.setText("Lỗi tải địa chỉ");
                    binding.tvDeliveryAddress.setText("Vui lòng thử lại");
                }
                checkAndHideLoading(); // Cập nhật trạng thái loading
            }

            @Override
            public void onFailure(Call<List<Address>> call, Throwable t) {
                defaultAddress = null;
                Log.e("OrderActivityError", "Failure fetching addresses", t);
                Toast.makeText(OrderActivity.this, "Lỗi mạng khi tải địa chỉ.", Toast.LENGTH_SHORT).show();
                binding.tvRecipientInfo.setText("Lỗi mạng");
                binding.tvDeliveryAddress.setText("Vui lòng kiểm tra kết nối");
                checkAndHideLoading(); // Cập nhật trạng thái loading
            }
        });
    }

    private void displayAddress(Address address) {
        if (address == null) {
            binding.tvRecipientInfo.setText("Chưa chọn địa chỉ");
            binding.tvDeliveryAddress.setText("Vui lòng chọn địa chỉ giao hàng");
            return;
        }
        String recipientInfo = (address.getFullName() != null ? address.getFullName() : "N/A") +
                " | " +
                (address.getPhone() != null ? address.getPhone() : "N/A");
        binding.tvRecipientInfo.setText(recipientInfo);

        StringBuilder fullAddressBuilder = new StringBuilder();
        if (address.getHouseNumber() != null && !address.getHouseNumber().isEmpty()) {
            fullAddressBuilder.append(address.getHouseNumber());
        }
        binding.tvDeliveryAddress.setText(fullAddressBuilder.length() > 0 ? fullAddressBuilder.toString() : "Địa chỉ không đầy đủ");
    }
    private void setupRecyclerView() {
        checkoutItems = new ArrayList<>();
        checkoutItemsAdapter = new CheckoutItemsAdapter(checkoutItems);
        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderItems.setAdapter(checkoutItemsAdapter);
        binding.rvOrderItems.setNestedScrollingEnabled(false);
    }

    private void setupPaymentMethods() {
        binding.rgPaymentMethod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbCOD) {
                    selectedPaymentMethodEnum = PaymentMethod.COD;
                    Log.d("PaymentSelection", "Selected: COD");
                } else if (checkedId == R.id.rbVNPAY) {
                    selectedPaymentMethodEnum = PaymentMethod.VNPAY;
                    Log.d("PaymentSelection", "Selected: VNPAY");
                }
            }
        });
        if (binding.rbCOD.isChecked()) {
            selectedPaymentMethodEnum = PaymentMethod.COD;
        } else if (binding.rbVNPAY.isChecked()) {
            selectedPaymentMethodEnum = PaymentMethod.VNPAY;
        }
    }


    private void fetchCheckoutData() {
        showLoading(true);
        apiCallsCompleted = 0; // Reset counter
        String headerAuth = "Bearer " + authToken;

        fetchAndSetDefaultAddress(headerAuth); // Tăng apiCallsCompleted trong callback
        fetchSelectedItemDetails(headerAuth); // Tăng apiCallsCompleted trong callback
    }

    private void fetchSelectedItemDetails(String headerAuth) {
        Log.d("OrderActivity", "Fetching item details for IDs: " + selectedItemIds.toString());
        CartItemDetailsRequest requestBody = new CartItemDetailsRequest(selectedItemIds);

        apiService.getSelectedCartItemDetails(headerAuth, requestBody).enqueue(new Callback<List<SelectedItemDetail>>() {
            @Override
            public void onResponse(Call<List<SelectedItemDetail>> call, Response<List<SelectedItemDetail>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    checkoutItems = response.body();
                    Log.d("OrderActivity", "Fetched " + checkoutItems.size() + " item details.");
                    checkoutItemsAdapter.updateData(checkoutItems);
                    binding.tvItemsTitle.setText(String.format(Locale.getDefault(), "Sản phẩm (%d)", calculateTotalQuantity(checkoutItems)));
                    updateTotals();
                } else {
                    Log.e("OrderActivityError", "Error fetching item details: " + response.code() + " - " + response.message());
                    Toast.makeText(OrderActivity.this, "Lỗi tải chi tiết sản phẩm.", Toast.LENGTH_SHORT).show();
                    checkoutItems.clear();
                    checkoutItemsAdapter.notifyDataSetChanged();
                    binding.tvItemsTitle.setText("Sản phẩm (0)");
                    updateTotals();
                }
                checkAndHideLoading(); // Kiểm tra và ẩn loading
            }

            @Override
            public void onFailure(Call<List<SelectedItemDetail>> call, Throwable t) {
                Log.e("OrderActivityError", "Failure fetching item details", t);
                Toast.makeText(OrderActivity.this, "Lỗi mạng khi tải sản phẩm.", Toast.LENGTH_SHORT).show();
                checkoutItems.clear();
                checkoutItemsAdapter.notifyDataSetChanged();
                binding.tvItemsTitle.setText("Sản phẩm (0)");
                updateTotals();
                checkAndHideLoading(); // Kiểm tra và ẩn loading
            }
        });
    }

    private synchronized void checkAndHideLoading() {
        apiCallsCompleted++;
        Log.d("OrderActivity", "API calls completed: " + apiCallsCompleted + "/" + TOTAL_API_CALLS);
        if (apiCallsCompleted >= TOTAL_API_CALLS) {
            showLoading(false);
        }
    }

    private int calculateTotalQuantity(List<SelectedItemDetail> items) {
        if (items == null) return 0;
        int total = 0;
        for (SelectedItemDetail item : items) {
            total += item.getQuantity();
        }
        return total;
    }


    private void updateTotals() {
        double subtotal = 0.0;
        if (checkoutItems != null) {
            for (SelectedItemDetail item : checkoutItems) {
                if (item.getProduct() != null && item.getProduct().getPrice() != null) {
                    try {
                        double price = ((Number) item.getProduct().getPrice()).doubleValue();
                        subtotal += price * item.getQuantity();
                    } catch (NumberFormatException | NullPointerException e) {
                        Log.e("UpdateTotalsError", "Could not parse price for product: " + (item.getProduct() != null ? item.getProduct().getProductId() : "null"), e);
                    }
                }
            }
        }

        double shippingFee = calculateShippingFee();

        double totalPrice = subtotal + shippingFee;

        binding.tvItemsSubtotal.setText(currencyFormat.format(subtotal));
        binding.tvShippingFee.setText(currencyFormat.format(shippingFee));
        binding.tvOrderTotal.setText(currencyFormat.format(totalPrice));
        binding.tvFinalTotalBottom.setText(currencyFormat.format(totalPrice));
    }

    private double calculateShippingFee() {
        if (checkoutItems == null || checkoutItems.isEmpty()) return 0.0;
        int totalQuantity = calculateTotalQuantity(checkoutItems);
        if (totalQuantity == 0) return 0.0;
        if (totalQuantity <= 3) return 15000.0;
        if (totalQuantity <= 7) return 25000.0;
        return 35000.0;
    }


    private void setupPlaceOrderButton() {
        binding.btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void placeOrder() {
        if (defaultAddress == null || defaultAddress.getAddressId() == null) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng hợp lệ.", Toast.LENGTH_SHORT).show();
            binding.scrollView.smoothScrollTo(0, binding.cardDeliveryAddress.getTop()); // Scroll đến địa chỉ
            return;
        }
        if (checkoutItems == null || checkoutItems.isEmpty()) {
            Toast.makeText(this, "Không có sản phẩm nào trong giỏ hàng để đặt.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không xác định được sản phẩm cần đặt.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        binding.btnPlaceOrder.setEnabled(false);

        CreateOrderRequest orderRequest = new CreateOrderRequest(selectedItemIds, selectedPaymentMethodEnum);
        String headerAuth = "Bearer " + authToken;

        Log.d("PlaceOrder", "Creating order with items: " + selectedItemIds + ", payment: " + selectedPaymentMethodEnum.name());

        apiService.createOrder(headerAuth, orderRequest).enqueue(new Callback<CreateOrderResponseDTO>() {
            @Override
            public void onResponse(Call<CreateOrderResponseDTO> call, Response<CreateOrderResponseDTO> response) {

                if (response.isSuccessful() && response.code() == 201 && response.body() != null) {
                    CreateOrderResponseDTO responseDto = response.body();
                    Order createdOrder = responseDto.getOrder();
                    String paymentUrl = responseDto.getPaymentUrl();

                    if (createdOrder != null) {
                        Log.i("PlaceOrder", "Order creation API successful! Order ID: " + createdOrder.getOrderId() + ", Status: " + (createdOrder.getStatus() != null ? createdOrder.getStatus().name() : "N/A"));

                        if (selectedPaymentMethodEnum == PaymentMethod.VNPAY && paymentUrl != null && !paymentUrl.isEmpty()) {
                            Log.i("PlaceOrder", "VNPAY payment required. Redirecting to URL: " + paymentUrl);
                            Toast.makeText(OrderActivity.this, "Đang chuyển hướng đến VNPAY...", Toast.LENGTH_SHORT).show();

                            pendingVnpayOrderId = createdOrder.getOrderId();
                            Log.d("PlaceOrder", "Saved pendingVnpayOrderId: " + pendingVnpayOrderId);

                            openPaymentUrl(paymentUrl);


                        } else {
                            Log.i("PlaceOrder", "Order placed with " + selectedPaymentMethodEnum.name() + " (or VNPAY URL missing/failed). Navigating.");

                            showLoading(false);
                            binding.btnPlaceOrder.setEnabled(true);

                            if (selectedPaymentMethodEnum == PaymentMethod.VNPAY && (paymentUrl == null || paymentUrl.isEmpty())) {
                                Toast.makeText(OrderActivity.this, "Đặt hàng VNPAY tạm thời không khả dụng. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                                pendingVnpayOrderId = null;
                            } else {
                                Toast.makeText(OrderActivity.this, "Đặt hàng thành công! Mã đơn: " + createdOrder.getOrderId(), Toast.LENGTH_LONG).show();
                                Intent successIntent = new Intent(OrderActivity.this, OrderDetailActivity.class);
                                successIntent.putExtra(EXTRA_ORDER_ID, createdOrder.getOrderId());
                                successIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(successIntent);
                                finish();
                            }
                        }
                    } else {
                        showLoading(false);
                        binding.btnPlaceOrder.setEnabled(true);
                        Log.e("PlaceOrderError", "API Success (201) but Order object is null in response DTO.");
                        Toast.makeText(OrderActivity.this, "Lỗi nội bộ: Không nhận được chi tiết đơn hàng.", Toast.LENGTH_LONG).show();
                        pendingVnpayOrderId = null; // Reset
                    }

                } else {
                    showLoading(false);
                    binding.btnPlaceOrder.setEnabled(true);
                    Log.e("PlaceOrderError", "API Error: " + response.code() + " - " + response.message());
                    handleApiError(response);
                    pendingVnpayOrderId = null;
                }
            }

            @Override
            public void onFailure(Call<CreateOrderResponseDTO> call, Throwable t) {
                showLoading(false);
                binding.btnPlaceOrder.setEnabled(true);
                Log.e("PlaceOrderError", "Failure placing order", t);
                Toast.makeText(OrderActivity.this, "Lỗi mạng khi đặt hàng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                pendingVnpayOrderId = null;
            }
        });
    }
    private void handleApiError(Response<?> response) {
        String errorMessage = "Đặt hàng thất bại. Mã lỗi: " + response.code();
        if (response.errorBody() != null) {
            try {
                String errorBodyString = response.errorBody().string();
                Log.e("ApiErrorBody", errorBodyString);
                Gson gson = new Gson();
                try {
                    Map<String, String> errorMap = gson.fromJson(errorBodyString, Map.class);
                    if (errorMap != null) {
                        if (errorMap.containsKey("message")) {
                            errorMessage = errorMap.get("message");
                        } else if (errorMap.containsKey("error")) {
                            errorMessage = errorMap.get("error");
                        } else {
                            errorMessage = "Lỗi máy chủ: " + errorBodyString.substring(0, Math.min(errorBodyString.length(), 150)) + "...";
                        }
                    } else {
                        errorMessage = "Lỗi máy chủ: " + errorBodyString.substring(0, Math.min(errorBodyString.length(), 150)) + "...";
                    }
                } catch (Exception parseEx) {
                    Log.e("ApiErrorBody", "Could not parse error body as JSON", parseEx);
                    errorMessage = "Lỗi máy chủ: " + errorBodyString.substring(0, Math.min(errorBodyString.length(), 150)) + "...";
                }

            } catch (IOException e) {
                Log.e("ApiErrorBody", "Error reading error body", e);
                errorMessage = "Lỗi đọc phản hồi từ máy chủ.";
            }
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
    private void showLoading(boolean isLoading) {
        if (binding != null && binding.progressBar != null) {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (!isLoading && pendingVnpayOrderId == null) {
            binding.btnPlaceOrder.setEnabled(true);
        } else {
            binding.btnPlaceOrder.setEnabled(false);
        }
    }

}