package vn.iostar.doan.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.iostar.doan.R;
import vn.iostar.doan.adapter.CartAdapter;
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.model.Cart;
import vn.iostar.doan.model.CartItem;
import vn.iostar.doan.modelRequest.CartActionRequest;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartItemListener {

    private static final String TAG = "CartActivity";

    private String authToken;
    private Toolbar toolbar;
    private RecyclerView recyclerViewCart;
    private ProgressBar progressBarCart;
    private TextView textViewEmptyCart;

    private TextView textViewTotalValue;
    private CheckBox checkboxSelectAll;
    private AppCompatButton buttonCheckout;

    private CartAdapter cartAdapter;
    private List<CartItem> cartItemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Get token
        authToken = getIntent().getStringExtra("token");
        if (authToken == null || authToken.isEmpty()) {
            Log.e(TAG, "Auth token is missing!");
            Toast.makeText(this, "Lỗi xác thực. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setupViews();
        setupToolbar();

        setupRecyclerView();
        setupListeners();
        fetchCartData();
    }
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(""); // Giữ nguyên không có title trên toolbar
        }
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Hoặc finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void setupViews() {
        recyclerViewCart = findViewById(R.id.recyclerViewCartItems);
        progressBarCart = findViewById(R.id.progressBarCart);
        textViewEmptyCart = findViewById(R.id.textViewEmptyCart);
        toolbar = findViewById(R.id.toolbar);

        textViewTotalValue = findViewById(R.id.textViewTotalValue); // Giữ lại, nhưng nó sẽ hiển thị tạm tính
        checkboxSelectAll = findViewById(R.id.checkboxSelectAll);
        buttonCheckout = findViewById(R.id.buttonCheckout);

        progressBarCart.setVisibility(View.GONE);
        textViewEmptyCart.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this, cartItemList, this);
        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCart.setAdapter(cartAdapter);
    }

    private void setupListeners() {
        checkboxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Logic giữ nguyên
            boolean stateNeedsChange = false;
            for (CartItem item : cartItemList) {
                if (item.isSelected() != isChecked) {
                    stateNeedsChange = true;
                    break;
                }
            }

            if (buttonView.isPressed() || stateNeedsChange) {
                Log.d(TAG, "SelectAll triggered. IsPressed: " + buttonView.isPressed() + ", NeedsChange: " + stateNeedsChange + ", NewState: " + isChecked);
                selectAllItems(isChecked);
                cartAdapter.notifyDataSetChanged(); // Cập nhật toàn bộ adapter để hiển thị checkbox
                updateSummary(); // Cập nhật tổng tiền và button
            } else {
                Log.d(TAG, "SelectAll event skipped. IsPressed: " + buttonView.isPressed() + ", NeedsChange: " + stateNeedsChange + ", CurrentState: " + isChecked);
            }
        });

        buttonCheckout.setOnClickListener(v -> {
            List<CartItem> selectedItems = new ArrayList<>();
            for (CartItem item : cartItemList) {
                if (item.isSelected()) {
                    selectedItems.add(item);
                }
            }
            if (!selectedItems.isEmpty()) {
                ArrayList<Long> selectedItemIds = new ArrayList<>();
                for (CartItem selectedItem : selectedItems) {
                    selectedItemIds.add(selectedItem.getCartItemId());
                }
                Toast.makeText(this, "Chuyển đến thanh toán với " + selectedItems.size() + " sản phẩm", Toast.LENGTH_SHORT).show();
                Intent checkoutIntent = new Intent(this, OrderActivity.class);
                checkoutIntent.putExtra("authToken", authToken);
                checkoutIntent.putExtra("SELECTED_CART_ITEM_IDS", selectedItemIds);
                startActivity(checkoutIntent);
            } else {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchCartData() {
        showLoading(true);

        String header = "Bearer " + authToken;
        ApiService.apiService.getCartItems(header).enqueue(new Callback<List<CartItem>>() {
            @Override
            public void onResponse(Call<List<CartItem>> call, Response<List<CartItem>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    cartItemList.clear();
                    cartItemList.addAll(response.body());

                    if (cartItemList.isEmpty()) {
                        showDataView(false, "Giỏ hàng của bạn đang trống");
                    } else {
                        showDataView(true, null);
                        cartAdapter.updateCartItems(cartItemList); // Cập nhật dữ liệu cho adapter
                        selectAllItems(true); // Chọn tất cả khi tải xong
                        cartAdapter.notifyDataSetChanged(); // Cập nhật UI adapter sau khi chọn tất cả
                        updateSummary(); // Cập nhật tổng tiền và nút checkout
                        updateSelectAllCheckboxState(); // Đồng bộ checkbox Select All
                    }
                } else {
                    handleApiError(response);
                    updateSummary(); // Cập nhật lại summary (về 0) khi có lỗi
                }
            }

            @Override
            public void onFailure(Call<List<CartItem>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network failure fetching cart", t);
                showDataView(false, "Lỗi kết nối mạng. Vui lòng thử lại.");
                updateSummary(); // Cập nhật lại summary (về 0) khi có lỗi mạng
            }
        });
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        // Logic giữ nguyên
        if (newQuantity <= 0) {
            onItemRemoved(item);
            return;
        }
        Long productId = item.getProduct().getProductId();
        String header = "Bearer " + authToken;
        CartActionRequest updateRequest = new CartActionRequest(productId, newQuantity);

        ApiService.apiService.updateCartItem(header, updateRequest).enqueue(new Callback<Cart>() {
            @Override
            public void onResponse(Call<Cart> call, Response<Cart> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Quantity updated successfully via API.");
                    Toast.makeText(CartActivity.this, "Đã cập nhật số lượng.", Toast.LENGTH_SHORT).show();
                    // Tìm item trong list và cập nhật lại số lượng và trạng thái selected
                    int index = cartItemList.indexOf(item);
                    if (index != -1) {
                        CartItem currentItem = cartItemList.get(index);
                        currentItem.setQuantity(newQuantity);
                        // Không cần gọi notifyDataSetChanged() ở đây vì fetchCartData sẽ làm
                    } else {
                        Log.w(TAG, "Item quantity updated, but local item not found to update directly. Refetching.");
                    }
                    // Tải lại toàn bộ giỏ hàng để đảm bảo dữ liệu nhất quán
                    fetchCartData(); // Hoặc chỉ cập nhật item và summary nếu API trả về item đã cập nhật
                } else {
                    String errorMsg = "Cập nhật số lượng thất bại.";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " Lỗi: " + response.code() + " - " + response.errorBody().string();
                        } else {
                            errorMsg += " Lỗi: " + response.code() + " " + response.message();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body (Update Quantity)", e);
                    }
                    if (response.code() == 401) errorMsg = "Phiên đăng nhập hết hạn.";
                    else if (response.code() == 400) errorMsg = "Yêu cầu không hợp lệ.";
                    Toast.makeText(CartActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    // Có thể cần fetch lại để đồng bộ trạng thái UI về trạng thái cũ
                    fetchCartData();
                }
            }

            @Override
            public void onFailure(Call<Cart> call, Throwable t) {
                Log.e(TAG, "API Call Failed (Update Quantity): " + t.getMessage(), t);
                Toast.makeText(CartActivity.this, "Lỗi kết nối. Không thể cập nhật số lượng.", Toast.LENGTH_LONG).show();
                // Có thể cần fetch lại để đồng bộ
                fetchCartData();
            }
        });
    }


    @Override
    public void onItemRemoved(CartItem item) {
        // Logic giữ nguyên
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa sản phẩm '" + item.getProduct().getName() + "' khỏi giỏ hàng?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    Long productIdToRemove = item.getProduct().getProductId();
                    String header = "Bearer " + authToken;
                    ApiService.apiService.removeCartItem(header,productIdToRemove).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                int position = cartItemList.indexOf(item);
                                if (position != -1) {
                                    cartItemList.remove(position);
                                    cartAdapter.notifyItemRemoved(position);
                                    // Quan trọng: Thông báo cho adapter về sự thay đổi phạm vi
                                    cartAdapter.notifyItemRangeChanged(position, cartItemList.size() - position);
                                    updateSummary();
                                    updateSelectAllCheckboxState();
                                    if (cartItemList.isEmpty()) {
                                        showDataView(false, "Giỏ hàng của bạn đã trống");
                                    }
                                    Toast.makeText(CartActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                                }else {
                                    Log.w(TAG, "Item removed successfully via API, but not found in local list for UI update. Refetching.");
                                    fetchCartData(); // Tải lại nếu không tìm thấy item trong list cục bộ
                                }
                            } else {
                                String errorMsg = "Xóa sản phẩm thất bại.";
                                try {
                                    if (response.errorBody() != null) {
                                        errorMsg += " Lỗi: " + response.code() + " - " + response.errorBody().string();
                                    } else {
                                        errorMsg += " Lỗi: " + response.code() + " " + response.message();
                                    }
                                } catch (IOException e) { Log.e(TAG, "Error reading error body", e); }
                                if (response.code() == 401) errorMsg = "Phiên đăng nhập hết hạn.";
                                else if (response.code() == 400) errorMsg = "Yêu cầu không hợp lệ.";
                                Toast.makeText(CartActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.e(TAG, "API Call Failed (Remove Item): " + t.getMessage(), t);
                            Toast.makeText(CartActivity.this, "Lỗi kết nối. Không thể xóa sản phẩm.", Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    @Override
    public void onItemSelectionChanged() {
        // Logic giữ nguyên
        updateSummary();
        updateSelectAllCheckboxState(); // Sync "Select All" checkbox
    }


    private void updateSummary() {
        double subtotal = 0;
        int selectedItemCount = 0;
        boolean hasItemSelected = false;

        for (CartItem item : cartItemList) {
            if (item.isSelected()) {
                subtotal += item.getItemTotalPrice();
                selectedItemCount++;
                hasItemSelected = true;
            }
        }

        // Bỏ tính thuế và tổng cuối cùng
        // double taxes = subtotal * 0.05;
        // double total = subtotal + taxes;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Cập nhật TextView "Tạm tính" (trước đây là Total)
        textViewTotalValue.setText(currencyFormat.format(subtotal));

        // Bỏ cập nhật TextView thuế
        // textViewTaxesValue.setText("+ " + currencyFormat.format(taxes));

        // Cập nhật nút Checkout
        buttonCheckout.setText("Thanh toán (" + selectedItemCount + ")");
        buttonCheckout.setEnabled(hasItemSelected);

        // Cập nhật background nút (có thể giữ nguyên hoặc đơn giản hóa nếu muốn)
        if (hasItemSelected) {
            buttonCheckout.setBackgroundResource(R.drawable.bottom_nav_background); // Màu nền khi enable
            // buttonCheckout.setTextColor(getColor(R.color.white)); // Đặt màu chữ nếu cần
        } else {
            buttonCheckout.setBackgroundResource(R.drawable.bg); // Màu nền khi disable (vd: màu xám)
            // buttonCheckout.setTextColor(getColor(R.color.grey)); // Đặt màu chữ khi disable nếu cần
        }
    }

    // Hàm này giữ nguyên
    private void updateSelectAllCheckboxState() {
        if (cartItemList.isEmpty()) {
            if (checkboxSelectAll.isChecked()) {
                // Gỡ bỏ listener tạm thời để tránh vòng lặp khi setChecked(false)
                checkboxSelectAll.setOnCheckedChangeListener(null);
                checkboxSelectAll.setChecked(false);
                // Đặt lại listener
                setupSelectAllListener();
            }
            checkboxSelectAll.setEnabled(false); // Vô hiệu hóa khi list trống
            return;
        } else {
            checkboxSelectAll.setEnabled(true); // Kích hoạt lại khi có item
        }

        boolean allSelected = true;
        for (CartItem item : cartItemList) {
            if (!item.isSelected()) {
                allSelected = false;
                break;
            }
        }

        if (checkboxSelectAll.isChecked() != allSelected) {
            // Gỡ bỏ listener tạm thời để tránh vòng lặp
            checkboxSelectAll.setOnCheckedChangeListener(null);
            checkboxSelectAll.setChecked(allSelected);
            // Đặt lại listener
            setupSelectAllListener();
        }
    }

    // Hàm này tách ra để dễ quản lý listener của checkbox Select All
    private void setupSelectAllListener() {
        checkboxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            boolean stateNeedsChange = false;
            for (CartItem item : cartItemList) {
                if (item.isSelected() != isChecked) {
                    stateNeedsChange = true;
                    break;
                }
            }
            // Chỉ xử lý nếu checkbox được nhấn hoặc trạng thái cần thay đổi thực sự
            if (buttonView.isPressed() || stateNeedsChange) {
                selectAllItems(isChecked);
                cartAdapter.notifyDataSetChanged();
                updateSummary();
            }
        });
    }


    // Hàm này giữ nguyên
    private void selectAllItems(boolean select) {
        for (CartItem item : cartItemList) {
            item.setSelected(select);
        }
    }

    // Hàm này giữ nguyên
    private void showLoading(boolean isLoading) {
        progressBarCart.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // Không ẩn/hiện RecyclerView trực tiếp ở đây, để showDataView xử lý
        if (isLoading) {
            textViewEmptyCart.setVisibility(View.GONE);
        }
    }

    // Hàm này giữ nguyên (chỉ ẩn/hiện RecyclerView thay vì NestedScrollView)
    private void showDataView(boolean showData, String message) {
        if (showData) {
            recyclerViewCart.setVisibility(View.VISIBLE); // Hiện RecyclerView
            textViewEmptyCart.setVisibility(View.GONE);
        } else {
            recyclerViewCart.setVisibility(View.GONE); // Ẩn RecyclerView
            textViewEmptyCart.setText(message);
            textViewEmptyCart.setVisibility(View.VISIBLE);
        }
        // Summary và Button luôn hiển thị, chỉ có giá trị thay đổi
    }

    // Hàm này giữ nguyên
    private void handleApiError(Response<?> response) {
        Log.e(TAG, "API Error: " + response.code() + " - " + response.message());
        String errorMessage = "Đã xảy ra lỗi khi tải giỏ hàng.";
        if (response.errorBody() != null) {
            try {
                errorMessage = response.errorBody().string();
                if (errorMessage.length() > 2 && errorMessage.startsWith("\"") && errorMessage.endsWith("\"")) {
                    errorMessage = errorMessage.substring(1, errorMessage.length() - 1);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading error body", e);
                errorMessage = response.message() != null ? response.message() : "Lỗi không xác định";
            }
        } else if (response.message() != null && !response.message().isEmpty()) {
            errorMessage = response.message();
        }
        showDataView(false, "Lỗi " + response.code() + ": " + errorMessage);
    }
}