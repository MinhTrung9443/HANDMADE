package vn.iostar.Project_Mobile.entity;

// === THÊM IMPORT CẦN THIẾT ===
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // Có thể cần cho Product
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "order_line")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long orderLineId;

    private int quantity;
    private double price;

    // === THÊM @JsonBackReference VÀ FetchType.LAZY CHO Order ===
    @ManyToOne(fetch = FetchType.LAZY) // <<< Tải trễ Order
    @JoinColumn(name = "order_id") // Đảm bảo tên cột khớp DB
    @JsonBackReference // Phía "con", sẽ KHÔNG được serialize -> Phá vỡ vòng lặp
    private Order order;

    // === THÊM FetchType.LAZY VÀ @JsonIgnoreProperties (Tùy chọn) CHO Product ===
    @ManyToOne(fetch = FetchType.LAZY) // <<< Tải trễ Product
    @JoinColumn(name = "product_id") // Đảm bảo tên cột khớp DB
    // Bỏ qua các trường không cần hoặc có thể gây lặp từ Product
    @JsonIgnoreProperties({"orderLines", "category", /* các trường khác nếu có */ "hibernateLazyInitializer", "handler"})
    private Product product;

    }