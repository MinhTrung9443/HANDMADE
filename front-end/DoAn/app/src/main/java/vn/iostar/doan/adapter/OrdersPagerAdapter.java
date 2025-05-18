package vn.iostar.doan.adapter; // Thay đổi package

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity; // Quan trọng: dùng FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.List;
import vn.iostar.doan.fragment.OrderListFragment;

public class OrdersPagerAdapter extends FragmentStateAdapter {

    private final List<String> orderStatuses; // Danh sách các trạng thái (String)
    private final Long userId;

    // Constructor nhận Activity, userId và danh sách trạng thái
    public OrdersPagerAdapter(@NonNull FragmentActivity fragmentActivity, Long userId, List<String> statuses) {
        super(fragmentActivity);
        this.userId = userId;
        this.orderStatuses = statuses;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Lấy trạng thái tương ứng với vị trí tab
        String status = orderStatuses.get(position);
        // Tạo một OrderListFragment mới với userId và trạng thái đó
        return OrderListFragment.newInstance(userId, status);
    }

    @Override
    public int getItemCount() {
        // Số lượng tab bằng số lượng trạng thái
        return orderStatuses.size();
    }

    // Phương thức helper để lấy tiêu đề cho tab (sẽ dùng trong Activity)
    public String getPageTitle(int position) {
        // Có thể tùy chỉnh tên hiển thị ở đây
        // Ví dụ: Chuyển "PROCESSING" thành "Đang xử lý"
        String status = orderStatuses.get(position);
        switch (status.toUpperCase()) {
            case "WAITING": return "Đang chờ duyệt";
            case "SHIPPING": return "Đang vận chuyển";
            case "RECEIVED": return "Đã nhận hàng";
            case "REVIEWED": return "Đã đánh giá";
            case "CANCELLED": return "Đã hủy";
            case "ERROR": return "Lỗi";
            default: return status; // Trả về tên gốc nếu không khớp
        }
    }
}