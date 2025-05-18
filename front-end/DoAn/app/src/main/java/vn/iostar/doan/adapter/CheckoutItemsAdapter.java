package vn.iostar.doan.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import vn.iostar.doan.R;
import vn.iostar.doan.model.Product;
import vn.iostar.doan.model.SelectedItemDetail;

public class CheckoutItemsAdapter extends RecyclerView.Adapter<CheckoutItemsAdapter.CheckoutItemViewHolder> {

private List<SelectedItemDetail> items;
private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
private Context context; // Thêm context để dùng Glide

// Constructor nhận danh sách items
public CheckoutItemsAdapter(List<SelectedItemDetail> items) {
    this.items = items;
}

@NonNull
@Override
public CheckoutItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    // Lưu context từ parent
    this.context = parent.getContext();
    // Inflate layout item
    View view = LayoutInflater.from(context).inflate(R.layout.item_checkout_cart, parent, false);
    return new CheckoutItemViewHolder(view);
}

@Override
public void onBindViewHolder(@NonNull CheckoutItemViewHolder holder, int position) {
    SelectedItemDetail currentItem = items.get(position);

    // Kiểm tra null trước khi truy cập
    if (currentItem != null && currentItem.getProduct() != null) {
        Product product = currentItem.getProduct();

        // Gán dữ liệu vào các view trong ViewHolder
        holder.tvProductName.setText(product.getName() != null ? product.getName() : "N/A");

        // Format và hiển thị giá đơn vị
        if (product.getPrice() != null) {
            holder.tvProductPrice.setText(currencyFormat.format(product.getPrice()));
        } else {
            holder.tvProductPrice.setText("N/A");
        }

        // Hiển thị số lượng
        holder.tvQuantity.setText("SL: " + currentItem.getQuantity());

        // Tính và hiển thị tổng tiền cho dòng item này
        if (product.getPrice() != null) {
            double itemTotal = product.getPrice() * currentItem.getQuantity();
            holder.tvItemTotal.setText(currencyFormat.format(itemTotal));
        } else {
            holder.tvItemTotal.setText("N/A");
        }

        // Load ảnh sản phẩm bằng Glide (hoặc Picasso)
        if (product.getImage() != null && !product.getImage().isEmpty()) {
            Glide.with(context)
                    .load(product.getImage())
                    .placeholder(R.drawable.ic_launcher_background) // Tạo drawable placeholder
                    .error(R.drawable.ic_launcher_background)       // Tạo drawable error
                    .into(holder.ivProductImage);
        } else {
            // Nếu không có ảnh, hiển thị ảnh mặc định
            holder.ivProductImage.setImageResource(R.drawable.ic_launcher_foreground);
        }

    } else {
        // Xử lý trường hợp item hoặc product bị null (hiển thị mặc định hoặc log lỗi)
        holder.tvProductName.setText("Lỗi sản phẩm");
        holder.tvProductPrice.setText("");
        holder.tvQuantity.setText("");
        holder.tvItemTotal.setText("");
        holder.ivProductImage.setImageResource(R.drawable.ic_launcher_background); // Ảnh lỗi
    }
}

@Override
public int getItemCount() {
    // Trả về số lượng item trong danh sách (kiểm tra null)
    return items != null ? items.size() : 0;
}

// Phương thức để cập nhật dữ liệu cho adapter
public void updateData(List<SelectedItemDetail> newItems) {
    this.items = newItems;
    notifyDataSetChanged(); // Thông báo cho RecyclerView cập nhật lại toàn bộ list
    // Để tối ưu hơn với danh sách lớn, cân nhắc dùng DiffUtil
}

// ViewHolder class: giữ tham chiếu đến các view trong item layout
public static class CheckoutItemViewHolder extends RecyclerView.ViewHolder {
    ImageView ivProductImage;
    TextView tvProductName;
    TextView tvProductPrice;
    TextView tvQuantity;
    TextView tvItemTotal;

    public CheckoutItemViewHolder(@NonNull View itemView) {
        super(itemView);
        // Ánh xạ view từ layout item_checkout_cart.xml
        ivProductImage = itemView.findViewById(R.id.iv_product_image);
        tvProductName = itemView.findViewById(R.id.tv_product_name);
        tvProductPrice = itemView.findViewById(R.id.tv_product_price);
        tvQuantity = itemView.findViewById(R.id.tv_quantity);
        tvItemTotal = itemView.findViewById(R.id.tv_item_total);
    }
}
}