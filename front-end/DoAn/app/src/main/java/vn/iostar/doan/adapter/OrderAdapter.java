package vn.iostar.doan.adapter; // <<<< ĐẢM BẢO ĐÚNG PACKAGE CỦA BẠN

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.iostar.doan.R; // <<<< Đảm bảo R đúng
import vn.iostar.doan.model.Order;
import vn.iostar.doan.model.OrderLine;
import vn.iostar.doan.model.OrderStatus;
import vn.iostar.doan.model.Product;
import vn.iostar.doan.model.Product2;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private final Context context;
    private final NumberFormat currencyFormatter;
    private final OrderInteractionListener listener;
    private final SimpleDateFormat dateFormatter;
    private final String currentAdapterTabStatus; // Lưu trạng thái của tab mà adapter này đang phục vụ

    public interface OrderInteractionListener {
        void onCancelOrderClicked(Order order);
        void onReviewOrderClicked(Order order);
        void onRepurchaseOrderClicked(Order order);
        void onViewDetailsClicked(Order order);
    }

    public OrderAdapter(Context context, OrderInteractionListener listener, String tabStatus) {
        this.context = context;
        this.listener = listener;
        this.currentAdapterTabStatus = tabStatus; // Gán trạng thái tab
        this.orderList = new ArrayList<>();
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setOrders(List<Order> orders) {
        this.orderList.clear();
        if (orders != null) {
            this.orderList.addAll(orders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_order, parent, false); // Đảm bảo R.layout.list_item_order tồn tại
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.bind(order, currencyFormatter, dateFormatter, listener, currentAdapterTabStatus);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderDate, tvOrderStatus, tvPredictDate, tvTotalPrice, tvPaymentMethod;
        LinearLayout llProductLines;
        Button btnCancelOrder, btnReviewOrder, btnRepurchaseOrder, btnViewDetails;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            tvPredictDate = itemView.findViewById(R.id.tv_predict_date);
            tvTotalPrice = itemView.findViewById(R.id.tv_total_price);
            tvPaymentMethod = itemView.findViewById(R.id.tv_payment_method);
            llProductLines = itemView.findViewById(R.id.ll_product_lines);
            btnCancelOrder = itemView.findViewById(R.id.btn_cancel_order);
            btnReviewOrder = itemView.findViewById(R.id.btn_review_order);
            btnRepurchaseOrder = itemView.findViewById(R.id.btn_repurchase_order);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Order order, NumberFormat currencyFormatter, SimpleDateFormat dateFormatter, final OrderInteractionListener listener, final String currentTabStatus) {
            if (order == null) {
                Log.e("OrderViewHolder", "Order object is null in bind");
                return;
            }

            tvOrderId.setText("Mã ĐH: #" + order.getOrderId());
            tvTotalPrice.setText("Tổng tiền: " + currencyFormatter.format(order.getTotalPrice()));

            OrderStatus status = order.getStatus();
            String statusDisplayString = getStatusDisplayString(status);
            tvOrderStatus.setText("Trạng thái: " + statusDisplayString);
            int statusColor = getStatusColor(itemView.getContext(), status);
            tvOrderStatus.setTextColor(statusColor);

            boolean hideDatesAndPayment = (status == OrderStatus.ERROR) || (status == OrderStatus.RECEIVED && order.isReviewed());

            tvOrderDate.setVisibility(hideDatesAndPayment || order.getOrderDate() == null ? View.GONE : View.VISIBLE);
            if (order.getOrderDate() != null && !hideDatesAndPayment) {
                tvOrderDate.setText("Ngày đặt: " + dateFormatter.format(order.getOrderDate()));
            }

            tvPredictDate.setVisibility(hideDatesAndPayment || order.getPredictReceiveDate() == null ? View.GONE : View.VISIBLE);
            if (order.getPredictReceiveDate() != null && !hideDatesAndPayment) {
                tvPredictDate.setText("Dự kiến: " + dateFormatter.format(order.getPredictReceiveDate()));
            }

            tvPaymentMethod.setVisibility(hideDatesAndPayment || order.getPaymentMethod() == null ? View.GONE : View.VISIBLE);
            if (order.getPaymentMethod() != null && !hideDatesAndPayment) {
                tvPaymentMethod.setText("Thanh toán: " + order.getPaymentMethod().toString()); // Đảm bảo enum PaymentMethod có toString() hợp lý
            }


            llProductLines.removeAllViews();
            if (order.getOrderLines() != null && !order.getOrderLines().isEmpty()) {
                for (OrderLine line : order.getOrderLines()) {
                    Product product = line.getProduct();
                    if (product != null && product.getName() != null) { // Thêm kiểm tra product.getName()
                        TextView tvProduct = new TextView(itemView.getContext());
                        tvProduct.setText("- " + product.getName() + " (SL: " + line.getQuantity() + ")");
                        tvProduct.setTextSize(14);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 0, 0, 4);
                        tvProduct.setLayoutParams(params);
                        llProductLines.addView(tvProduct);
                    } else {
                        Log.w("OrderViewHolder", "OrderLine has null product or product name for order ID: " + order.getOrderId());
                    }
                }
            }

            // Reset visibility của tất cả các nút trước
            btnCancelOrder.setVisibility(View.GONE);
            btnReviewOrder.setVisibility(View.GONE);
            btnRepurchaseOrder.setVisibility(View.GONE);
            btnViewDetails.setVisibility(View.GONE); // Mặc định ẩn nút Xem chi tiết

            // Xử lý hiển thị nút "Xem chi tiết"
            if (!"RECEIVED".equalsIgnoreCase(currentTabStatus)) {
                // Nếu không phải tab "RECEIVED", hiển thị nút "Xem chi tiết"
                btnViewDetails.setVisibility(View.VISIBLE);
                btnViewDetails.setOnClickListener(v -> {
                    if (listener != null) listener.onViewDetailsClicked(order);
                });
            }
            // Nếu là tab "RECEIVED", nút "Xem chi tiết" sẽ vẫn là GONE (đã set ở trên)

            // Xử lý các nút khác dựa trên trạng thái của đơn hàng
            if (status != null) {
                switch (status) {
                    case WAITING:
                        btnCancelOrder.setVisibility(View.VISIBLE);
                        btnCancelOrder.setOnClickListener(v -> {
                            if (listener != null) listener.onCancelOrderClicked(order);
                        });
                        break;
                    case RECEIVED:
                        // Đối với đơn hàng "RECEIVED", nút "Xem chi tiết" đã bị ẩn nếu đang ở tab "RECEIVED"
                        // Giờ chỉ hiển thị nút "Đánh giá" hoặc "Mua lại"
                        if (order.isReviewed()) { // Đã đánh giá
                            btnRepurchaseOrder.setVisibility(View.VISIBLE);
                            btnRepurchaseOrder.setOnClickListener(v -> {
                                if (listener != null) listener.onRepurchaseOrderClicked(order);
                            });
                        } else { // Chưa đánh giá
                            btnReviewOrder.setVisibility(View.VISIBLE);
                            btnReviewOrder.setOnClickListener(v -> {
                                if (listener != null) listener.onReviewOrderClicked(order);
                            });
                        }
                        break;
                    // Các trạng thái khác (SHIPPING, ERROR, REVIEWED (nếu là trạng thái riêng))
                    // Nút "Xem chi tiết" sẽ được hiển thị nếu không phải tab "RECEIVED" (do logic ở trên)
                    // Các nút hành động khác (Hủy, Đánh giá, Mua lại) không áp dụng cho các trạng thái này.
                    case SHIPPING:
                        // Có thể vẫn muốn hiển thị nút Xem chi tiết (nếu không phải tab RECEIVED)
                        break;
                    case ERROR:
                        // Có thể vẫn muốn hiển thị nút Xem chi tiết (nếu không phải tab RECEIVED)
                        // hoặc nút Mua lại nếu logic cho phép
                        break;
                    case REVIEWED: // Nếu đây là trạng thái sau khi "Đã duyệt" đơn hàng, trước khi giao
                        // Có thể vẫn muốn hiển thị nút Xem chi tiết (nếu không phải tab RECEIVED)
                        break;

                    default:
                        // Không có hành động cụ thể cho các trạng thái khác
                        break;
                }
            }
        }

        private String getStatusDisplayString(OrderStatus status) {
            if (status == null) return "N/A";
            switch (status) {
                case WAITING: return "Đang chờ";
                case REVIEWED: return "Đã duyệt";
                case SHIPPING: return "Đang vận chuyển";
                case RECEIVED: return "Đã nhận hàng";
                case ERROR: return "Đã hủy";
                default: return status.name();
            }
        }

        private int getStatusColor(Context context, OrderStatus status) {
            if (status == null) {
                return ContextCompat.getColor(context, R.color.my_grey_neutral); // Đảm bảo có màu này
            }
            switch (status) {
                case RECEIVED: return ContextCompat.getColor(context, R.color.my_green_success); // Đảm bảo có màu này
                case SHIPPING: return ContextCompat.getColor(context, R.color.my_orange_processing); // Đảm bảo có màu này
                case ERROR:    return ContextCompat.getColor(context, R.color.my_red_error); // Đảm bảo có màu này
                default:       return ContextCompat.getColor(context, R.color.my_grey_neutral);
            }
        }
    }
}