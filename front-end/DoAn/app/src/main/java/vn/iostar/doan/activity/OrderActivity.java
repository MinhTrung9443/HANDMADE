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

    // Định dạng tiền tệ
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    // Biến cờ để kiểm tra xem cả 2 API fetch dữ liệu đã gọi xong chưa
    private int apiCallsCompleted = 0;
    private final int TOTAL_API_CALLS = 2; // Số lượng API cần gọi khi load dữ liệu (địa chỉ và chi tiết sản phẩm)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiService.apiService; // Lấy instance của ApiService

        // Nhận và kiểm tra dữ liệu Intent
        if (!receiveIntentData()) {
            return; // Thoát Activity nếu thiếu dữ liệu quan trọng
        }

        setupToolbar();
        setupRecyclerView();
        setupPaymentMethods(); // Setup lựa chọn thanh toán
        setupPlaceOrderButton();
        setupAddressClickListener(); // Thêm listener để có thể đổi địa chỉ

        // Khôi phục trạng thái nếu Activity bị tắt và tạo lại trong quá trình chuyển hướng
        if (savedInstanceState != null) {
            pendingVnpayOrderId = savedInstanceState.getLong("pendingVnpayOrderId", -1L);
            if (pendingVnpayOrderId == -1L) {
                pendingVnpayOrderId = null; // Đặt về null nếu không tìm thấy ID hợp lệ
            }
            Log.d("OrderActivity", "Restored pendingVnpayOrderId: " + pendingVnpayOrderId);
        }

        // Bắt đầu tải dữ liệu checkout ban đầu
        fetchCheckoutData();
    }

    // *** PHƯƠNG THỨC QUAN TRỌNG ĐƯỢC GỌI KHI ACTIVITY ĐƯỢC HIỂN THỊ LẠI (bao gồm cả sau khi quay về từ VNPAY) ***
    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra xem chúng ta có đang chờ kết quả thanh toán VNPAY nào không
        if (pendingVnpayOrderId != null) {
            Log.i("PlaceOrder", "Activity resumed. Checking status for pending VNPAY order: " + pendingVnpayOrderId);
            showLoading(true); // Hiển thị loading
            binding.btnPlaceOrder.setEnabled(false); // Tắt nút

            String headerAuth = "Bearer " + authToken;

            // Gọi API lấy chi tiết đơn hàng
            apiService.getOrderDetails(headerAuth, pendingVnpayOrderId).enqueue(new Callback<Order>() {
                @Override
                public void onResponse(Call<Order> call, Response<Order> response) {
                    showLoading(false); // Ẩn loading
                    if (response.isSuccessful() && response.body() != null) {
                        Order order = response.body();
                        Log.i("PlaceOrder", "Checked status for order " + pendingVnpayOrderId + ": " + (order != null && order.getStatus() != null ? order.getStatus().name() : "N/A"));

                        if (order != null && order.getStatus() != null) {
                            // *** SỬA LOGIC KIỂM TRA TRẠNG THÁI DỰA TRÊN ENUM ĐỒNG BỘ ***
                            if (order.getStatus() == OrderStatus.REVIEWED || order.getStatus() == OrderStatus.SHIPPING || order.getStatus() == OrderStatus.DELIVERED) { // Các trạng thái thành công sau thanh toán
                                Toast.makeText(OrderActivity.this, "Thanh toán VNPAY thành công! Mã đơn: " + order.getOrderId(), Toast.LENGTH_LONG).show();
                                Intent successIntent = new Intent(OrderActivity.this, HomeActivity.class); // Hoặc OrderHistoryActivity
                                successIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(successIntent);
                                finish();
                            } else if (order.getStatus() == OrderStatus.CANCELLED ) { // Trạng thái thất bại/hủy
                                Toast.makeText(OrderActivity.this, "Thanh toán VNPAY thất bại hoặc đã bị hủy cho đơn " + order.getOrderId() + ". Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                                binding.btnPlaceOrder.setEnabled(true);
                                pendingVnpayOrderId = null; // Reset state
                            } else if (order.getStatus() == OrderStatus.PENDING) { // Vẫn chờ xác nhận (IPN chưa tới?)
                                Toast.makeText(OrderActivity.this, "Trạng thái đơn hàng VNPAY chưa được xác nhận. Vui lòng kiểm tra lại sau.", Toast.LENGTH_LONG).show();
                                binding.btnPlaceOrder.setEnabled(true);
                                pendingVnpayOrderId = null; // Reset state
                            } else if (order.getStatus() == OrderStatus.WAITING) { // Trạng thái Waiting (Ban đầu cho COD)
                                // Nếu Backend chuyển PendingPayment -> Waiting khi thành công VNPAY,
                                // thì Mobile coi Waiting là thành công (không khuyến khích, nên dùng Processing).
                                Toast.makeText(OrderActivity.this, "Đơn hàng VNPAY đã được xác nhận. Mã đơn: " + order.getOrderId(), Toast.LENGTH_LONG).show();
                                Intent successIntent = new Intent(OrderActivity.this, HomeActivity.class); // Hoặc OrderHistoryActivity
                                successIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(successIntent);
                                finish();

                            }
                            else { // Trạng thái khác không mong muốn
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

    // *** PHƯƠNG THỨC ĐƯỢC GỌI KHI ACTIVITY LƯU TRẠNG THÁI (trước khi bị hệ điều hành kill) ***
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingVnpayOrderId != null) {
            outState.putLong("pendingVnpayOrderId", pendingVnpayOrderId);
            Log.d("OrderActivity", "Saving pendingVnpayOrderId: " + pendingVnpayOrderId);
        }
    }

    // *** PHƯƠNG THỨC TRỢ GIÚP ĐỂ MỞ URL THANH TOÁN ***
    private void openPaymentUrl(String url) {
        try {
            // Có thể dùng Custom Tabs để trải nghiệm tốt hơn nếu không mở app VNPAY
            // CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            // builder.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary)); // Đặt màu toolbar
            // CustomTabsIntent customTabsIntent = builder.build();
            // customTabsIntent.launchUrl(this, Uri.parse(url));

            // Hoặc mở bằng trình duyệt mặc định
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);

        } catch (Exception e) {
            Log.e("PlaceOrder", "Failed to open payment URL: " + url, e);
            Toast.makeText(this, "Không thể mở liên kết thanh toán: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // Xử lý lỗi: ẩn loading, bật lại nút, reset trạng thái chờ VNPAY
            showLoading(false);
            binding.btnPlaceOrder.setEnabled(true);
            pendingVnpayOrderId = null; // Reset pending ID vì không redirect được
        }
    }


    // Các phương thức khác của Activity (giữ nguyên vị trí):

    private boolean receiveIntentData() {
        // ... (giữ nguyên code nhận intent) ...
        Intent intent = getIntent();
        // Đảm bảo key "authToken" đúng như bạn gửi từ Activity trước
        authToken = intent.getStringExtra("authToken"); // <-- Kiểm tra key này

        if (intent.hasExtra("SELECTED_CART_ITEM_IDS")) { // <-- Kiểm tra key này
            try {
                // Sử dụng getSerializableExtra
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
        // ... (giữ nguyên code setup toolbar) ...
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void fetchAndSetDefaultAddress(String headerAuth) {
        // ... (giữ nguyên code fetch và hiển thị địa chỉ) ...
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
                            if (addr != null && addr.isDefaultAddress()) { // <-- Đảm bảo isDefaultAddress() đúng
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

    // Hàm displayAddress (giữ nguyên hoặc cải thiện)
    private void displayAddress(Address address) {
        // ... (giữ nguyên code hiển thị địa chỉ lên TextViews) ...
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
        if (address.getWard() != null && !address.getWard().isEmpty()) {
            if (fullAddressBuilder.length() > 0) fullAddressBuilder.append(", ");
            fullAddressBuilder.append(address.getWard());
        }
        if (address.getDistrict() != null && !address.getDistrict().isEmpty()) {
            if (fullAddressBuilder.length() > 0) fullAddressBuilder.append(", ");
            fullAddressBuilder.append(address.getDistrict());
        }
        if (address.getCity() != null && !address.getCity().isEmpty()) {
            if (fullAddressBuilder.length() > 0) fullAddressBuilder.append(", ");
            fullAddressBuilder.append(address.getCity());
        }
        // Bỏ qua country nếu không cần hiển thị

        binding.tvDeliveryAddress.setText(fullAddressBuilder.length() > 0 ? fullAddressBuilder.toString() : "Địa chỉ không đầy đủ");
    }
    private void setupRecyclerView() {
        // ... (giữ nguyên code setup RecyclerView) ...
        checkoutItems = new ArrayList<>();
        checkoutItemsAdapter = new CheckoutItemsAdapter(checkoutItems);
        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderItems.setAdapter(checkoutItemsAdapter);
        binding.rvOrderItems.setNestedScrollingEnabled(false);
    }

    private void setupPaymentMethods() {
        // ... (giữ nguyên code setup RadioGroup) ...
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

        // Set initial enum value based on the checked button
        if (binding.rbCOD.isChecked()) {
            selectedPaymentMethodEnum = PaymentMethod.COD;
        } else if (binding.rbVNPAY.isChecked()) {
            selectedPaymentMethodEnum = PaymentMethod.VNPAY;
        }
    }

    private void setupAddressClickListener() {
        binding.cardDeliveryAddress.setOnClickListener(v -> {
            // TODO: Mở màn hình chọn/thêm địa chỉ (nếu có)
            // Ví dụ: startActivityForResult(new Intent(this, AddressSelectionActivity.class), REQUEST_CODE_SELECT_ADDRESS);
            Toast.makeText(this, "Chức năng chọn địa chỉ chưa được cài đặt.", Toast.LENGTH_SHORT).show();
        });
    }


    private void fetchCheckoutData() {
        showLoading(true);
        apiCallsCompleted = 0; // Reset counter
        String headerAuth = "Bearer " + authToken;

        fetchAndSetDefaultAddress(headerAuth); // Tăng apiCallsCompleted trong callback
        fetchSelectedItemDetails(headerAuth); // Tăng apiCallsCompleted trong callback
    }

    private void fetchSelectedItemDetails(String headerAuth) {
        // ... (giữ nguyên code fetch item details) ...
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

    // synchronized đảm bảo chỉ có 1 thread truy cập method này tại 1 thời điểm
    private synchronized void checkAndHideLoading() {
        apiCallsCompleted++;
        Log.d("OrderActivity", "API calls completed: " + apiCallsCompleted + "/" + TOTAL_API_CALLS);
        if (apiCallsCompleted >= TOTAL_API_CALLS) {
            showLoading(false);
            // Không reset apiCallsCompleted ở đây, trừ khi bạn có logic load lại dữ liệu nhiều lần
            // apiCallsCompleted = 0;
        }
    }

    private int calculateTotalQuantity(List<SelectedItemDetail> items) {
        // ... (giữ nguyên code tính tổng số lượng) ...
        if (items == null) return 0;
        int total = 0;
        for (SelectedItemDetail item : items) {
            total += item.getQuantity();
        }
        return total;
    }


    private void updateTotals() {
        // ... (giữ nguyên code update totals) ...
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
        // ... (giữ nguyên code tính phí ship ví dụ) ...
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
                // showLoading(false); // KHÔNG ẩn loading ngay nếu là VNPAY redirect
                // binding.btnPlaceOrder.setEnabled(true); // KHÔNG bật lại nút ngay nếu là VNPAY redirect

                if (response.isSuccessful() && response.code() == 201 && response.body() != null) {
                    CreateOrderResponseDTO responseDto = response.body();
                    Order createdOrder = responseDto.getOrder();
                    String paymentUrl = responseDto.getPaymentUrl();

                    if (createdOrder != null) {
                        Log.i("PlaceOrder", "Order creation API successful! Order ID: " + createdOrder.getOrderId() + ", Status: " + (createdOrder.getStatus() != null ? createdOrder.getStatus().name() : "N/A"));

                        if (selectedPaymentMethodEnum == PaymentMethod.VNPAY && paymentUrl != null && !paymentUrl.isEmpty()) {
                            Log.i("PlaceOrder", "VNPAY payment required. Redirecting to URL: " + paymentUrl);
                            Toast.makeText(OrderActivity.this, "Đang chuyển hướng đến VNPAY...", Toast.LENGTH_SHORT).show();

                            // *** LƯU Order ID trước khi chuyển hướng ***
                            pendingVnpayOrderId = createdOrder.getOrderId();
                            Log.d("PlaceOrder", "Saved pendingVnpayOrderId: " + pendingVnpayOrderId);

                            // Giữ trạng thái loading (hoặc hiển thị UI chờ thanh toán) và tắt nút
                            // showLoading(true); // Đã gọi ở đầu phương thức
                            // binding.btnPlaceOrder.setEnabled(false); // Đã gọi ở đầu phương thức

                            openPaymentUrl(paymentUrl);

                            // KHÔNG finish() Activity ngay. Giữ Activity này lại để xử lý lúc quay lại trong onResume.

                        } else { // COD hoặc Phương thức khác (hoặc VNPAY URL lỗi)
                            Log.i("PlaceOrder", "Order placed with " + selectedPaymentMethodEnum.name() + " (or VNPAY URL missing/failed). Navigating.");

                            // Ẩn loading và bật lại nút cho các trường hợp KHÔNG redirect VNPAY
                            showLoading(false);
                            binding.btnPlaceOrder.setEnabled(true);

                            if (selectedPaymentMethodEnum == PaymentMethod.VNPAY && (paymentUrl == null || paymentUrl.isEmpty())) {
                                // Trường hợp đặc biệt: Chọn VNPAY nhưng tạo URL lỗi.
                                Toast.makeText(OrderActivity.this, "Đặt hàng VNPAY tạm thời không khả dụng. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                                // Đơn hàng vẫn ở trạng thái PendingPayment nhưng không có URL. Người dùng không thanh toán được.
                                // Để người dùng ở lại màn hình này hoặc tự quay về giỏ hàng.
                                pendingVnpayOrderId = null; // Reset vì không redirect
                                // Không finish() ngay
                            } else {
                                // COD thành công hoặc các phương thức không cần redirect khác
                                Toast.makeText(OrderActivity.this, "Đặt hàng thành công! Mã đơn: " + createdOrder.getOrderId(), Toast.LENGTH_LONG).show();
                                // Chuyển hướng cho COD
                                Intent successIntent = new Intent(OrderActivity.this, HomeActivity.class); // Hoặc OrderHistoryActivity
                                successIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(successIntent);
                                finish(); // Đóng Activity này cho COD
                            }
                        }
                    } else { // API trả về 201 nhưng Order object bị null - lỗi logic backend?
                        showLoading(false);
                        binding.btnPlaceOrder.setEnabled(true);
                        Log.e("PlaceOrderError", "API Success (201) but Order object is null in response DTO.");
                        Toast.makeText(OrderActivity.this, "Lỗi nội bộ: Không nhận được chi tiết đơn hàng.", Toast.LENGTH_LONG).show();
                        pendingVnpayOrderId = null; // Reset
                        // Tùy chọn: ở lại màn hình hoặc chuyển hướng cẩn thận
                    }

                } else { // API gọi thất bại (lỗi 4xx, 5xx)
                    showLoading(false);
                    binding.btnPlaceOrder.setEnabled(true);
                    Log.e("PlaceOrderError", "API Error: " + response.code() + " - " + response.message());
                    handleApiError(response); // Sử dụng hàm xử lý lỗi đã có
                    pendingVnpayOrderId = null; // Reset
                }
            }

            @Override
            public void onFailure(Call<CreateOrderResponseDTO> call, Throwable t) {
                showLoading(false);
                binding.btnPlaceOrder.setEnabled(true);
                Log.e("PlaceOrderError", "Failure placing order", t);
                Toast.makeText(OrderActivity.this, "Lỗi mạng khi đặt hàng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                pendingVnpayOrderId = null; // Reset
            }
        });
    }


    // Hàm xử lý lỗi API chung
    private void handleApiError(Response<?> response) {
        // ... (giữ nguyên code handleApiError) ...
        String errorMessage = "Đặt hàng thất bại. Mã lỗi: " + response.code();
        if (response.errorBody() != null) {
            try {
                String errorBodyString = response.errorBody().string();
                Log.e("ApiErrorBody", errorBodyString); // Log lỗi chi tiết
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


    // Hàm hiển thị/ẩn loading indicator (cần có ProgressBar trong layout với id="progressBar")
    private void showLoading(boolean isLoading) {
        if (binding != null && binding.progressBar != null) {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Logic vô hiệu hóa/kích hoạt nút đặt hàng
        if (!isLoading && pendingVnpayOrderId == null) {
            binding.btnPlaceOrder.setEnabled(true);
        } else {
            binding.btnPlaceOrder.setEnabled(false);
        }
    }

}