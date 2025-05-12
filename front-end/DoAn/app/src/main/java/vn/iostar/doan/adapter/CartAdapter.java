package vn.iostar.doan.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import vn.iostar.doan.R;
import vn.iostar.doan.model.CartItem;
import vn.iostar.doan.model.Product;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final Context context;
    private List<CartItem> cartItemList;
    private final CartItemListener listener;

    public interface CartItemListener {
        void onQuantityChanged(CartItem item, int newQuantity); // Khi số lượng thay đổi
        void onItemRemoved(CartItem item); // Khi nhấn nút xóa
        void onItemSelectionChanged(); // Khi trạng thái chọn của item thay đổi (checkbox)
    }

    public CartAdapter(Context context, List<CartItem> cartItemList, CartItemListener listener) {
        this.context = context;
        this.cartItemList = cartItemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem currentItem = cartItemList.get(position);
        Product product = currentItem.getProduct();

        if (product != null) {
            holder.textViewProductName.setText(product.getName());

            // Định dạng tiền tệ Việt Nam
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            holder.textViewProductPrice.setText(currencyFormat.format(product.getPrice()));

            // Load ảnh sản phẩm bằng Glide
            Glide.with(context)
                    .load(product.getImage())
                    .placeholder(R.drawable.ic_launcher_foreground) // Ảnh chờ
                    .error(R.drawable.ic_launcher_background) // Ảnh lỗi
                    .into(holder.imageViewProduct);
        } else {
            // Xử lý trường hợp product null (dữ liệu lỗi?)
            holder.textViewProductName.setText("Sản phẩm lỗi");
            holder.textViewProductPrice.setText("N/A");
            holder.imageViewProduct.setImageResource(R.drawable.ic_launcher_foreground); // Ảnh lỗi
        }

        holder.textViewQuantity.setText(String.valueOf(currentItem.getQuantity()));

        // Xử lý Checkbox
        // Bỏ listener cũ trước khi set checked để tránh trigger vòng lặp khi cuộn
        holder.checkboxItemSelect.setOnCheckedChangeListener(null);
        holder.checkboxItemSelect.setChecked(currentItem.isSelected());
        // Đặt listener mới
        holder.checkboxItemSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentItem.setSelected(isChecked);
            if (listener != null) {
                listener.onItemSelectionChanged(); // Thông báo cho Activity rằng lựa chọn đã thay đổi
            }
        });


        // Xử lý nút giảm số lượng
        holder.buttonDecreaseQuantity.setOnClickListener(v -> {
            int currentQuantity = currentItem.getQuantity();
            if (currentQuantity > 1) { // Số lượng tối thiểu là 1
                int newQuantity = currentQuantity - 1;
                // Không cập nhật trực tiếp list ở đây, thông báo cho Activity xử lý
                if (listener != null) {
                    listener.onQuantityChanged(currentItem, newQuantity);
                }
            } else {
                // Có thể hiển thị dialog hỏi có muốn xóa không khi giảm từ 1
                // Hoặc gọi luôn listener xóa
                if (listener != null) {
                    listener.onItemRemoved(currentItem);
                }
            }
        });

        // Xử lý nút tăng số lượng
        holder.buttonIncreaseQuantity.setOnClickListener(v -> {
            int currentQuantity = currentItem.getQuantity();
            // Có thể kiểm tra với số lượng tồn kho (product.getStockQuantity()) nếu cần
            int newQuantity = currentQuantity + 1;
            if (listener != null) {
                listener.onQuantityChanged(currentItem, newQuantity);
            }
        });

        // Xử lý nút xóa item
        holder.buttonRemoveItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemRemoved(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList == null ? 0 : cartItemList.size();
    }

    public void updateCartItems(List<CartItem> newItems) {
        this.cartItemList = newItems;
        notifyDataSetChanged();
    }

    public List<CartItem> getCurrentItems() {
        return cartItemList;
    }

    // ViewHolder class
    static class CartViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkboxItemSelect;
        ImageView imageViewProduct;
        TextView textViewProductName, textViewProductPrice, textViewQuantity;
        ImageButton  buttonRemoveItem;
        Button buttonDecreaseQuantity, buttonIncreaseQuantity;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxItemSelect = itemView.findViewById(R.id.itemCheckbox);
            imageViewProduct = itemView.findViewById(R.id.img_product);
            textViewProductName = itemView.findViewById(R.id.tv_product_name);
            textViewProductPrice = itemView.findViewById(R.id.tv_product_price);
            textViewQuantity = itemView.findViewById(R.id.tv_quantity);
            buttonDecreaseQuantity = itemView.findViewById(R.id.btn_decrease);
            buttonIncreaseQuantity = itemView.findViewById(R.id.btn_increase);
            buttonRemoveItem = itemView.findViewById(R.id.buttonRemoveItem);
        }
    }
}
