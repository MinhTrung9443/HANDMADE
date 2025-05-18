package vn.iostar.doan.fragment; // <<<< THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
// import java.util.Objects; // Bỏ import nếu không dùng trực tiếp
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import vn.iostar.doan.R;
import vn.iostar.doan.activity.OrderDetailActivity; // <<<< QUAN TRỌNG: Import để dùng hằng số
import vn.iostar.doan.activity.OrderHistoryActivity;
import vn.iostar.doan.adapter.OrderAdapter;
import vn.iostar.doan.model.Order;

public class OrderListFragment extends Fragment implements OrderAdapter.OrderInteractionListener {

    private static final String TAG = "OrderListFragment";
    // Sử dụng tên khác cho argument keys để tránh nhầm lẫn với Intent keys
    private static final String ARG_USER_ID_FRAGMENT = "user_id_arg_for_fragment";
    private static final String ARG_ORDER_STATUS_FRAGMENT = "order_status_arg_for_fragment";

    // KHÔNG CẦN ĐỊNH NGHĨA LẠI HẰNG SỐ Ở ĐÂY
    // Chúng sẽ được lấy từ OrderDetailActivity.java
    // public static final String ACTION_MODE_SELECT_PRODUCT_FOR_REVIEW = "ACTION_SELECT_PRODUCT_FOR_REVIEW";
    // public static final String EXTRA_USER_ID_FOR_REVIEW = "USER_ID_FOR_REVIEW";


    private RecyclerView recyclerViewOrders;
    private OrderAdapter orderAdapter;
    private ProgressBar progressBar;
    private TextView tvErrorMessage;
    private TextView tvNoOrders;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .create();

    private static final String BASE_URL = "http://10.0.2.2:8080/";

    private Long fragmentUserId; // Đổi tên để rõ ràng là userId của Fragment này
    private String fragmentTargetStatus;

    public static OrderListFragment newInstance(Long userId, String status) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID_FRAGMENT, userId); // Sử dụng key argument đã đổi tên
        args.putString(ARG_ORDER_STATUS_FRAGMENT, status); // Sử dụng key argument đã đổi tên
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            fragmentUserId = getArguments().getLong(ARG_USER_ID_FRAGMENT); // Nhận bằng key argument đã đổi tên
            fragmentTargetStatus = getArguments().getString(ARG_ORDER_STATUS_FRAGMENT); // Nhận bằng key argument đã đổi tên
            Log.d(TAG, "Fragment created for UserID: " + fragmentUserId + ", Status: " + fragmentTargetStatus);
        } else {
            Log.e(TAG, "Fragment created without required arguments! Using defaults.");
            fragmentUserId = -1L;
            fragmentTargetStatus = "UNKNOWN";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_list, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewOrders = view.findViewById(R.id.recycler_view_orders_fragment);
        progressBar = view.findViewById(R.id.progress_bar_fragment);
        tvErrorMessage = view.findViewById(R.id.tv_error_message_fragment);
        tvNoOrders = view.findViewById(R.id.tv_no_orders_fragment);

        setupRecyclerView();

        if (fragmentUserId != null && fragmentUserId > 0 &&
                fragmentTargetStatus != null && !fragmentTargetStatus.equals("UNKNOWN")) {
            fetchOrdersWithOkHttp(fragmentUserId, fragmentTargetStatus);
        } else {
            String errorMsg = "Lỗi cấu hình Fragment: UserID=" + fragmentUserId + ", Status=" + fragmentTargetStatus;
            Log.e(TAG, errorMsg);
            showError(errorMsg);
        }
    }

    private void setupRecyclerView() {
        if (getContext() == null) {
            Log.e(TAG, "Context is null during setupRecyclerView");
            return;
        }
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new OrderAdapter(getContext(), this, fragmentTargetStatus); // Truyền fragmentTargetStatus
        recyclerViewOrders.setAdapter(orderAdapter);
    }

    private void fetchOrdersWithOkHttp(Long currentUserIdToFetch, final String statusToFilter) {
        showLoading(true);
        showError(null);
        showNoOrdersMessage(false);

        String url = BASE_URL + "api/orders/status/" + currentUserIdToFetch; // URL bạn đang dùng
        Log.d(TAG, "Fetching ALL orders for user from URL: " + url + " (will filter for status: " + statusToFilter + " on client-side)");

        Request.Builder requestBuilder = new Request.Builder().url(url);
        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "OkHttp Fetch onFailure: ", e);
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        if (tvErrorMessage.getVisibility() == View.GONE && tvNoOrders.getVisibility() == View.GONE) {
                            showError("Lỗi kết nối mạng. Vui lòng thử lại.");
                        }
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        final String errorMsg = "Lỗi Server: " + response.code();
                        String errorBodyStr = (responseBody != null) ? responseBody.string() : "";
                        Log.e(TAG, "OkHttp Fetch onResponse Error: " + errorMsg + ", Body: " + errorBodyStr);
                        if (getActivity() != null && isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                showLoading(false);
                                showError(errorMsg + ": Không thể tải dữ liệu.");
                            });
                        }
                        return;
                    }

                    if (responseBody == null) {
                        Log.e(TAG, "OkHttp Fetch onResponse Success but body is null");
                        if (getActivity() != null && isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                showLoading(false);
                                showError("Không nhận được dữ liệu từ server.");
                            });
                        }
                        return;
                    }

                    final String responseData = responseBody.string();
                    Log.d(TAG, "OkHttp Fetch onResponse Success JSON (first 500 chars): " + responseData.substring(0, Math.min(responseData.length(), 500)) + "...");

                    Type listType = new TypeToken<ArrayList<Order>>() {}.getType();
                    final List<Order> allOrders = gson.fromJson(responseData, listType);

                    if (allOrders == null) {
                        Log.e(TAG, "Parsed order list is null. JSON might be malformed or not an array: " + responseData);
                        if (getActivity() != null && isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                showLoading(false);
                                showError("Dữ liệu trả về không đúng định dạng.");
                            });
                        }
                        return;
                    }

                    List<Order> filteredOrders;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        filteredOrders = allOrders.stream()
                                .filter(order -> order.getStatus() != null &&
                                        order.getStatus().name().equalsIgnoreCase(statusToFilter))
                                .collect(Collectors.toList());
                    } else {
                        filteredOrders = new ArrayList<>();
                        for (Order orderItem : allOrders) { // Đổi tên biến để tránh trùng
                            if (orderItem.getStatus() != null &&
                                    orderItem.getStatus().name().equalsIgnoreCase(statusToFilter)) {
                                filteredOrders.add(orderItem);
                            }
                        }
                    }
                    Log.d(TAG, "Total orders fetched: " + allOrders.size() + ", Filtered orders for status '" + statusToFilter + "': " + filteredOrders.size());

                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            if (filteredOrders.isEmpty()) {
                                showNoOrdersMessage(true);
                                if (orderAdapter != null) orderAdapter.setOrders(new ArrayList<>());
                            } else {
                                if (orderAdapter != null) orderAdapter.setOrders(filteredOrders);
                                showNoOrdersMessage(false);
                            }
                            showError(null);
                        });
                    }

                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "JSON Parsing Error: ", e);
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            showError("Lỗi xử lý dữ liệu trả về (JSON).");
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException reading response body: ", e);
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            if (tvErrorMessage.getVisibility() == View.GONE) {
                                showError("Lỗi đọc dữ liệu từ server.");
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error in onResponse processing: ", e);
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            if (tvErrorMessage.getVisibility() == View.GONE) {
                                showError("Đã xảy ra lỗi không mong muốn.");
                            }
                        });
                    }
                }
            }
        });
    }

    private void callCancelOrderApi(long orderIdToCancel) {
        // ... (Giữ nguyên logic)
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached or context is null, cannot call cancel API.");
            return;
        }
        Toast.makeText(requireContext(), "Đang gửi yêu cầu hủy...", Toast.LENGTH_SHORT).show();
        showLoading(true);

        String url = BASE_URL + "api/orders/" + orderIdToCancel + "/cancel";
        Log.d(TAG, "Calling cancel API: " + url);

        RequestBody emptyBody = RequestBody.create(new byte[0], null);
        Request.Builder requestBuilder = new Request.Builder().url(url).put(emptyBody);
        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "OkHttp Cancel Order onFailure: ", e);
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(requireContext(), "Hủy đơn thất bại: Lỗi mạng", Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        try (ResponseBody responseBody = response.body()) {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "OkHttp Cancel Order Success. Code: " + response.code());
                                Toast.makeText(requireContext(), "Đã hủy đơn hàng #" + orderIdToCancel, Toast.LENGTH_SHORT).show();
                                if (fragmentUserId != null && fragmentTargetStatus != null && !fragmentTargetStatus.equals("UNKNOWN")) {
                                    fetchOrdersWithOkHttp(fragmentUserId, fragmentTargetStatus);
                                } else {
                                    showLoading(false);
                                }
                            } else {
                                String errorMessage = "Hủy đơn thất bại: Mã lỗi " + response.code();
                                String errorBodyString = null;
                                if (responseBody != null) {
                                    try { errorBodyString = responseBody.string();
                                        if (errorBodyString != null && !errorBodyString.isEmpty() && !errorBodyString.startsWith("<")) {
                                            errorMessage = "Hủy thất bại: " + errorBodyString.substring(0, Math.min(errorBodyString.length(), 100));
                                        }
                                    } catch (IOException ioException) { Log.e(TAG, "Error reading error body for cancel", ioException); }
                                }
                                Log.e(TAG, "OkHttp Cancel Order Error: " + errorMessage + (errorBodyString != null ? " (Raw Body: " + errorBodyString + ")" : ""));
                                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                                showLoading(false);
                            }
                        } catch (Exception exception) { // Catch all other exceptions
                            Log.e(TAG, "Unexpected error processing cancel response", exception);
                            Toast.makeText(requireContext(), "Lỗi không xác định khi hủy đơn.", Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    });
                }
            }
        });
    }


    private void showLoading(boolean isLoading) {
        if (!isAdded() || progressBar == null) return;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            if (tvErrorMessage != null) tvErrorMessage.setVisibility(View.GONE);
            if (tvNoOrders != null) tvNoOrders.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (!isAdded() || tvErrorMessage == null || recyclerViewOrders == null || tvNoOrders == null) return;
        if (message != null && !message.isEmpty()) {
            tvErrorMessage.setText(message);
            tvErrorMessage.setVisibility(View.VISIBLE);
            recyclerViewOrders.setVisibility(View.GONE);
            tvNoOrders.setVisibility(View.GONE);
        } else {
            tvErrorMessage.setVisibility(View.GONE);
        }
    }

    private void showNoOrdersMessage(boolean show) {
        if (!isAdded() || tvNoOrders == null || recyclerViewOrders == null || tvErrorMessage == null) return;
        tvNoOrders.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerViewOrders.setVisibility(View.GONE);
            tvErrorMessage.setVisibility(View.GONE);
        } else {
            recyclerViewOrders.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onCancelOrderClicked(Order order) {
        // ... (Giữ nguyên)
        if (!isAdded() || getContext() == null) return;
        Log.d(TAG, "Cancel button clicked for order ID: " + order.getOrderId());
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận hủy đơn")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #" + order.getOrderId() + " không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> callCancelOrderApi(order.getOrderId()))
                .setNegativeButton("Không", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    @Override
    public void onReviewOrderClicked(Order order) {
        if (!isAdded() || getContext() == null || getActivity() == null) {
            Log.e(TAG, "Fragment not attached or context/activity is null. Cannot proceed to review.");
            return;
        }

        Log.d(TAG, "Review button clicked for Order ID: " + order.getOrderId());

        Intent intentToOrderDetail = new Intent(requireContext(), OrderDetailActivity.class);

        // SỬ DỤNG HẰNG SỐ TỪ OrderDetailActivity.java ĐỂ ĐẢM BẢO NHẤT QUÁN
        intentToOrderDetail.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getOrderId());
        intentToOrderDetail.putExtra(OrderDetailActivity.EXTRA_ACTION_MODE, OrderDetailActivity.ACTION_MODE_SELECT_PRODUCT_FOR_REVIEW);

        if (fragmentUserId != null && fragmentUserId > 0) { // Sử dụng fragmentUserId đã đổi tên
            intentToOrderDetail.putExtra(OrderDetailActivity.EXTRA_USER_ID_FOR_REVIEW, fragmentUserId); // Sử dụng fragmentUserId
            Log.d(TAG, "Passing " + OrderDetailActivity.EXTRA_USER_ID_FOR_REVIEW + ": " + fragmentUserId +
                    " with Key " + OrderDetailActivity.EXTRA_ACTION_MODE + " = " + OrderDetailActivity.ACTION_MODE_SELECT_PRODUCT_FOR_REVIEW +
                    " to OrderDetailActivity for OrderID: " + order.getOrderId());
        } else {
            Log.e(TAG, "FragmentUserId (" + fragmentUserId + ") is invalid when trying to review order.");
            Toast.makeText(getContext(), "Lỗi: Không thể xác định người dùng để đánh giá.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (getActivity() instanceof OrderHistoryActivity) {
            ((OrderHistoryActivity) getActivity()).launchReviewActivity(intentToOrderDetail);
        } else {
            Log.e(TAG, "Parent activity is not OrderHistoryActivity. Launching OrderDetailActivity directly.");
            startActivity(intentToOrderDetail);
        }
    }


    @Override
    public void onRepurchaseOrderClicked(Order order) {
        // ... (Giữ nguyên)
        if (!isAdded() || getContext() == null) return;
        Log.d(TAG, "Repurchase button clicked for order ID: " + order.getOrderId());
        if (order.getOrderLines() != null && !order.getOrderLines().isEmpty()) {
            Toast.makeText(requireContext(), "Chức năng Mua lại đang được phát triển.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Không có sản phẩm trong đơn hàng này để mua lại.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewDetailsClicked(Order order) {
        // ... (Giữ nguyên)
        if (!isAdded() || getContext() == null) return;
        Log.d(TAG, "View Details button clicked for order ID: " + order.getOrderId());
        Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getOrderId());
        startActivity(intent);
    }

    public void refreshOrders() {
        Log.d(TAG, "refreshOrders called for status: " + fragmentTargetStatus); // Sử dụng fragmentTargetStatus
        if (fragmentUserId != null && fragmentUserId > 0 && // Sử dụng fragmentUserId
                fragmentTargetStatus != null && !fragmentTargetStatus.equals("UNKNOWN") && isAdded()) {
            fetchOrdersWithOkHttp(fragmentUserId, fragmentTargetStatus); // Sử dụng fragmentUserId
        }
    }
}