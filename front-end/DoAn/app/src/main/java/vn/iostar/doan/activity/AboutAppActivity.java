package vn.iostar.doan.activity; // Đảm bảo package này đúng với cấu trúc project của bạn

import android.os.Bundle;
import android.view.View; // Import View
import android.widget.ImageButton; // Import ImageButton

import androidx.appcompat.app.AppCompatActivity;

import vn.iostar.doan.R; // Đảm bảo R class được import đúng

public class AboutAppActivity extends AppCompatActivity {

    private ImageButton btnBack; // Khai báo biến cho nút back

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aboutapp); // Đặt layout cho Activity

        // Ánh xạ các View từ layout
        btnBack = findViewById(R.id.btn_back);

        // Thiết lập listener cho nút quay lại
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Khi nút back được click, đóng Activity hiện tại
                finish();
                // Hoặc có thể dùng onBackPressed(); tùy vào cách bạn muốn xử lý navigation
                // onBackPressed();
            }
        });

        // Các View khác (TextViews) trong layout chỉ cần hiển thị
        // nên không cần ánh xạ hoặc xử lý sự kiện ở đây trừ khi bạn muốn thay đổi nội dung động.
    }

    // Nếu bạn muốn override hành vi của nút back cứng của thiết bị (tùy chọn)
    // @Override
    // public void onBackPressed() {
    //     super.onBackPressed(); // Quay lại màn hình trước đó
    // }
}