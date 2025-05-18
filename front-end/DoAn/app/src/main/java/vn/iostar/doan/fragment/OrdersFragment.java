package vn.iostar.doan.fragment; // <<<< ĐẢM BẢO ĐÚNG PACKAGE CỦA BẠN

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Import các model và activity cần thiết
import java.util.ArrayList;
import java.util.List;

import vn.iostar.doan.R; // <<<< Đảm bảo R đúng
import vn.iostar.doan.activity.OrderDetailActivity;
import vn.iostar.doan.adapter.OrderAdapter; // <<<< Import OrderAdapter
import vn.iostar.doan.model.Order;
import vn.iostar.doan.model.OrderLine; // Nếu bạn dùng trong logic giả lập
import vn.iostar.doan.model.OrderStatus; // Enum trạng thái
import vn.iostar.doan.model.Product;
import vn.iostar.doan.model.Product2;   // Nếu bạn dùng trong logic giả lập

// Các import cho API call (OkHttp hoặc Retrofit) nếu bạn triển khai ở đây
// Ví dụ:
// import com.google.gson.Gson;
// import com.google.gson.GsonBuilder;
// import okhttp3.OkHttpClient;
// import retrofit2.Call;
// import retrofit2.Callback;
// import retrofit2.Response;
// import vn.iostar.doan.api.ApiService;
// import vn.iostar.doan.service.RetrofitClient; // Nếu bạn có class này

public class OrdersFragment extends Fragment implements OrderAdapter.OrderInteractionListener {

    private static final String TAG = "OrdersFragment";
    private static final String ARG_STATUS = "status_arg";
    private static final String ARG_USER_ID = "user_id_arg";
    public static final String ACTION_SELECT_PRODUCT_FOR_REVIEW = "ACTION_SELECT_PRODUCT_FOR_REVIEW";

    private String currentTabStatus;
    private Long currentUserId;

    private RecyclerView recyclerViewOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList = new ArrayList<>();
    private ProgressBar progressBarOrders;
    private TextView tvNoOrdersMessage;
    private TextView tvErrorMessageOrders;

    public static OrdersFragment newInstance(String status, Long userId) {
        OrdersFragment fragment = new OrdersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentTabStatus = getArguments().getString(ARG_STATUS);
            currentUserId = getArguments().getLong(ARG_USER_ID);
            Log.d(TAG, "Fragment created for Status: " + currentTabStatus + ", UserID: " + currentUserId);
        } else {
            Log.e(TAG, "OrdersFragment created without required arguments!");
            currentTabStatus = "UNKNOWN";
            currentUserId = -1L;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false); // << Đảm bảo bạn có R.layout.fragment_orders

        recyclerViewOrders = view.findViewById(R.id.recycler_view_orders);
        progressBarOrders = view.findViewById(R.id.progress_bar_orders);
        tvNoOrdersMessage = view.findViewById(R.id.tv_no_orders_message);
        tvErrorMessageOrders = view.findViewById(R.id.tv_error_message_orders);

        setupRecyclerView();

        if (currentUserId != null && currentUserId > 0 && currentTabStatus != null && !currentTabStatus.equals("UNKNOWN")) {
            fetchOrders();
        } else {
            showErrorMessage("Không thể tải đơn hàng do thiếu thông tin người dùng hoặc trạng thái.");
            Log.e(TAG, "Cannot fetch orders. UserID: " + currentUserId + ", Status: " + currentTabStatus);
        }

        return view;
    }

    private void setupRecyclerView() {
        if (getContext() == null) {
            Log.e(TAG, "Context is null during setupRecyclerView");
            return;
        }
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        // KHỞI TẠO ADAPTER VỚI CONSTRUCTOR ĐÚNG, TRUYỀN THÊM currentTabStatus
        orderAdapter = new OrderAdapter(getContext(), this, currentTabStatus);
        recyclerViewOrders.setAdapter(orderAdapter);
    }

    private void fetchOrders() {
        Log.d(TAG, "Fetching orders for User: " + currentUserId + ", Status: " + currentTabStatus);
        showLoading(true);
        tvErrorMessageOrders.setVisibility(View.GONE);
        tvNoOrdersMessage.setVisibility(View.GONE);

        // ======================================================================
        // === THAY THẾ PHẦN NÀY BẰNG LOGIC GỌI API THỰC TẾ CỦA BẠN ===
        // (Ví dụ đã comment ở các phản hồi trước)
        // ======================================================================

        // --- PHẦN GIẢ LẬP DỮ LIỆU (XÓA KHI CÓ API THẬT) ---
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;
            showLoading(false);
            List<Order> dummyOrders = new ArrayList<>();
            // Tạo dữ liệu giả lập dựa trên currentTabStatus
            if (currentTabStatus.equalsIgnoreCase("WAITING")) {
                Order order1 = new Order(); order1.setOrderId(101L); order1.setStatus(OrderStatus.WAITING);
                Product p1 = new Product(); p1.setName("Sản phẩm Chờ 1");
                OrderLine ol1 = new OrderLine(); ol1.setProduct(p1); ol1.setQuantity(1);
                order1.setOrderLines(List.of(ol1));
                dummyOrders.add(order1);
            } else if (currentTabStatus.equalsIgnoreCase("REVIEWED")) {
                Order order2 = new Order(); order2.setOrderId(202L); order2.setStatus(OrderStatus.REVIEWED);
                Product p2 = new Product(); p2.setName("Sản phẩm Đã Duyệt 2");
                OrderLine ol2 = new OrderLine(); ol2.setProduct(p2); ol2.setQuantity(2);
                order2.setOrderLines(List.of(ol2));
                dummyOrders.add(order2);
            } else if (currentTabStatus.equalsIgnoreCase("SHIPPING")) {
                Order order3 = new Order(); order3.setOrderId(303L); order3.setStatus(OrderStatus.SHIPPING);
                Product p3 = new Product(); p3.setName("Sản phẩm Đang Giao 3");
                OrderLine ol3 = new OrderLine(); ol3.setProduct(p3); ol3.setQuantity(1);
                order3.setOrderLines(List.of(ol3));
                dummyOrders.add(order3);
            } else if (currentTabStatus.equalsIgnoreCase("RECEIVED")) {
                Order order4 = new Order(); order4.setOrderId(404L); order4.setStatus(OrderStatus.RECEIVED); order4.setReviewed(false);
                Product p4_1 = new Product(); p4_1.setProductId(1L); p4_1.setName("Sản phẩm Đã Nhận A (chưa đánh giá)");
                Product p4_2 = new Product(); p4_2.setProductId(2L); p4_2.setName("Sản phẩm Đã Nhận B (chưa đánh giá)");
                OrderLine ol4_1 = new OrderLine(); ol4_1.setProduct(p4_1); ol4_1.setQuantity(1);
                OrderLine ol4_2 = new OrderLine(); ol4_2.setProduct(p4_2); ol4_2.setQuantity(2);
                order4.setOrderLines(List.of(ol4_1, ol4_2));
                dummyOrders.add(order4);

                Order order5 = new Order(); order5.setOrderId(505L); order5.setStatus(OrderStatus.RECEIVED); order5.setReviewed(true);
                Product p5 = new Product(); p5.setProductId(3L); p5.setName("Sản phẩm Đã Nhận C (đã đánh giá)");
                OrderLine ol5 = new OrderLine(); ol5.setProduct(p5); ol5.setQuantity(3);
                order5.setOrderLines(List.of(ol5));
                dummyOrders.add(order5);
            } else if (currentTabStatus.equalsIgnoreCase("ERROR")) {
                Order order6 = new Order(); order6.setOrderId(606L); order6.setStatus(OrderStatus.ERROR);
                Product p6 = new Product(); p6.setName("Sản phẩm Lỗi/Hủy 6");
                OrderLine ol6 = new OrderLine(); ol6.setProduct(p6); ol6.setQuantity(1);
                order6.setOrderLines(List.of(ol6));
                dummyOrders.add(order6);
            }

            orderAdapter.setOrders(dummyOrders);
            if (dummyOrders.isEmpty()) {
                showNoOrdersMessage(true);
            } else {
                showNoOrdersMessage(false);
                recyclerViewOrders.setVisibility(View.VISIBLE);
            }
        }, 1000);
        // --- KẾT THÚC PHẦN GIẢ LẬP ---
    }

    private void showLoading(boolean isLoading) {
        if (progressBarOrders != null) {
            progressBarOrders.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void showNoOrdersMessage(boolean show) {
        if (tvNoOrdersMessage != null) {
            tvNoOrdersMessage.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewOrders != null) {
            recyclerViewOrders.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (tvErrorMessageOrders != null && show) {
            tvErrorMessageOrders.setVisibility(View.GONE);
        }
    }

    private void showErrorMessage(String message) {
        if (tvErrorMessageOrders != null) {
            tvErrorMessageOrders.setText(message);
            tvErrorMessageOrders.setVisibility(View.VISIBLE);
        }
        if (recyclerViewOrders != null) {
            recyclerViewOrders.setVisibility(View.GONE);
        }
        if (tvNoOrdersMessage != null) {
            tvNoOrdersMessage.setVisibility(View.GONE);
        }
    }

    // === IMPLEMENT CÁC PHƯƠNG THỨC CỦA OrderAdapter.OrderInteractionListener ===
    @Override
    public void onCancelOrderClicked(Order order) {
        if (!isAdded() || getContext() == null) return;
        Log.d(TAG, "Cancel clicked for order: " + order.getOrderId() + " in tab: " + currentTabStatus);
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận hủy đơn")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #" + order.getOrderId() + "?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    Toast.makeText(getContext(), "Đang xử lý hủy đơn #" + order.getOrderId(), Toast.LENGTH_SHORT).show();
                    // TODO: Gọi API hủy đơn và cập nhật lại danh sách (fetchOrders())
                })
                .setNegativeButton("Không", null)
                .show();
    }

    @Override
    public void onReviewOrderClicked(Order order) {
        if (!isAdded() || getContext() == null) return;
        Log.d(TAG, "Review clicked for order: " + order.getOrderId() + " in tab: " + currentTabStatus);

        Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getOrderId());
        intent.putExtra("ACTION_MODE", ACTION_SELECT_PRODUCT_FOR_REVIEW);
        if (currentUserId != null && currentUserId > 0) {
            intent.putExtra("USER_ID_FOR_REVIEW", currentUserId);
        } else {
            Log.e(TAG, "User ID is invalid when navigating for review. Cannot proceed.");
            Toast.makeText(getContext(), "Lỗi: Không thể xác định người dùng để đánh giá.", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
    }

    @Override
    public void onRepurchaseOrderClicked(Order order) {
        if (!isAdded() || getContext() == null) return;
        Log.d(TAG, "Repurchase clicked for order: " + order.getOrderId() + " in tab: " + currentTabStatus);
        Toast.makeText(getContext(), "Mua lại đơn hàng #" + order.getOrderId(), Toast.LENGTH_SHORT).show();
        // TODO: Implement logic thêm sản phẩm vào giỏ hàng
    }

    @Override
    public void onViewDetailsClicked(Order order) {
        if (!isAdded() || getContext() == null) return;
        Log.d(TAG, "View Details clicked for order: " + order.getOrderId() + " in tab: " + currentTabStatus);
        Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getOrderId());
        startActivity(intent);
    }
}