package vn.iostar.doan.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge; // Nếu dùng SDK mới
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets; // Nếu dùng SDK mới
import androidx.core.view.ViewCompat; // Nếu dùng SDK mới
import androidx.core.view.WindowInsetsCompat; // Nếu dùng SDK mới

import vn.iostar.doan.R;

public class OrderSuccessActivity extends AppCompatActivity {

    private Button btnTrackOrder, btnBackHome;
    private TextView tvSuccessMessage;
    private String orderId; // Để lưu mã đơn hàng nếu cần cho việc theo dõi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Bật nếu bạn dùng SDK mới và muốn giao diện tràn viền
        setContentView(R.layout.activity_success_order);

        // Lấy orderId từ Intent (nếu bạn truyền nó từ OrderActivity)
        orderId = getIntent().getStringExtra("ORDER_ID");

        btnTrackOrder = findViewById(R.id.btn_track_order);
        btnBackHome = findViewById(R.id.btn_backHome);
        tvSuccessMessage = findViewById(R.id.btn_track_order); // Lấy TextView nếu muốn hiển thị mã đơn

        // (Tùy chọn) Hiển thị mã đơn hàng trên TextView
        if (orderId != null && !orderId.isEmpty()) {
            tvSuccessMessage.setText("Đặt hàng thành công!\nMã đơn: " + orderId);
        } else {
            tvSuccessMessage.setText("Đặt hàng thành công!");
        }


        // Xử lý sự kiện nút "Theo dõi đơn hàng"
        btnTrackOrder.setOnClickListener(v -> {
            // TODO: Chuyển đến màn hình lịch sử đơn hàng hoặc chi tiết đơn hàng
            // Ví dụ: Chuyển đến OrderHistoryActivity và truyền orderId nếu cần
            Intent intent = new Intent(OrderSuccessActivity.this, OrderDetailActivity.class); // Thay bằng Activity thực tế
            // intent.putExtra("FOCUS_ORDER_ID", orderId); // Truyền ID nếu màn hình lịch sử cần focus vào đơn này
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Xóa màn hình thành công khỏi stack
            startActivity(intent);
            finish(); // Đóng màn hình thành công
            // Toast.makeText(this, "Chức năng Theo dõi đơn hàng (ID: " + orderId + ") đang được phát triển", Toast.LENGTH_SHORT).show();
        });

        // Xử lý sự kiện nút "Tiếp tục mua sắm"
        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(OrderSuccessActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Xóa hết activity trung gian, về Home
            startActivity(intent);
            finish(); // Đóng màn hình thành công
        });

        // Xử lý nút Back của hệ thống (tùy chọn: có thể cho nó làm giống nút "Tiếp tục mua sắm")
        // Nếu dùng EdgeToEdge
        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        */

        // Ghi đè nút back vật lý/hệ thống (nếu muốn nó hoạt động như nút "Tiếp tục mua sắm")
        // onBackPressedDispatcher.addCallback(this, new OnBackPressedCallback(true) {
        //     @Override
        //     public void handleOnBackPressed() {
        //         // Hành động giống nút "Tiếp tục mua sắm"
        //         Intent intent = new Intent(OrderSuccessActivity.this, MainActivity.class);
        //         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        //         startActivity(intent);
        //         finish();
        //     }
        // });
    }

    // Ghi đè nút back cho API cũ hơn (nếu không dùng OnBackPressedDispatcher)
//     @Override
//     public void onBackPressed() {
//         // Hành động giống nút "Tiếp tục mua sắm"
//         Intent intent = new Intent(OrderSuccessActivity.this, MainActivity.class);
//         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//         startActivity(intent);
//         finish();
//         super.onBackPressed(); // Gọi super nếu bạn muốn hành vi mặc định ở một số trường hợp
//     }
}