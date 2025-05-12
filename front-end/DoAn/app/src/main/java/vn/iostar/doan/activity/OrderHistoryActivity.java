package vn.iostar.doan.activity;

import static android.content.ContentValues.TAG;

import android.content.Intent; // <<< THÊM IMPORT INTENT
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import vn.iostar.doan.R;
import vn.iostar.doan.adapter.OrdersPagerAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects; // <<< THÊM IMPORT OBJECTS (Tùy chọn, để tránh warning)

public class OrderHistoryActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private OrdersPagerAdapter pagerAdapter;
    private Toolbar toolbar;

    private Long currentUserId = 1L; // <-- THAY ĐỔI: Lấy userId thực tế

    private final List<String> orderStatuses = Arrays.asList(
            "WAITING",
            "REVIEWED",
            "SHIPPING",
            "RECEIVED",
            "ERROR"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);
        currentUserId = getIntent().getLongExtra("userId", currentUserId); // *** Dùng đúng key "user_id" ***

        // Kiểm tra xem userId có hợp lệ không
        if (currentUserId <= 0) {
            Log.e(TAG, "Invalid User ID received: " + currentUserId);
            Toast.makeText(this, "Error: Could not load user history.", Toast.LENGTH_LONG).show();
            finish(); // Đóng Activity nếu không có ID hợp lệ
            return;
        }

        toolbar = findViewById(R.id.toolbar); // Đảm bảo ID toolbar đúng trong layout
        setSupportActionBar(toolbar);

        // --- KÍCH HOẠT VÀ CẤU HÌNH ACTIONBAR/TOOLBAR ---
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Theo dõi trạng thái đơn hàng"); // Đặt tiêu đề
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // <<< HIỂN THỊ NÚT BACK
            getSupportActionBar().setDisplayShowHomeEnabled(true); // <<< Đảm bảo icon được hiển thị (tùy theme)
        }
        // -------------------------------------------------

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        // Lấy userId thực tế nếu cần
        // currentUserId = SharedPreferencesUtils.getLong(this, "userId", 1L); // Ví dụ

        pagerAdapter = new OrdersPagerAdapter(this, currentUserId, orderStatuses);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(pagerAdapter.getPageTitle(position));
        }).attach();
    }

    // --- XỬ LÝ SỰ KIỆN NHẤN NÚT BACK TRÊN TOOLBAR ---
    @Override
    public boolean onSupportNavigateUp() {
        // Tạo Intent để quay về HomeActivity
        Intent intent = new Intent(OrderHistoryActivity.this, HomeActivity.class);
        // Các cờ này giúp đảm bảo nếu HomeActivity đã tồn tại thì dùng lại nó,
        // và xóa các Activity khác phía trên nó trong stack (bao gồm cả OrderHistoryActivity)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish(); // Đóng Activity hiện tại sau khi đã điều hướng
        return true; // Trả về true để báo rằng sự kiện đã được xử lý
    }
    // ----------------------------------------------------

    // (Tùy chọn) Bạn cũng có thể muốn xử lý nút Back vật lý tương tự
    // @Override
    // public void onBackPressed() {
    //     // Thay vì gọi super.onBackPressed();
    //     Intent intent = new Intent(OrderHistoryActivity.this, HomeActivity.class);
    //     intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    //     startActivity(intent);
    //     finish();
    // }
}