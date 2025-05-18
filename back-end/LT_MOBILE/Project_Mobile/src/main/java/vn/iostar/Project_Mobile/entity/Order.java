package vn.iostar.Project_Mobile.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import vn.iostar.Project_Mobile.util.OrderStatus;
import vn.iostar.Project_Mobile.util.PaymentMethod;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "orders")
@ToString(exclude = {"user", "orderLines"})  
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id") 
    private long orderId;

    @Column(name = "items_subtotal", nullable = false) 
    private Double itemsSubtotal;   

    @Column(name = "total_price") 
    private double totalPrice;

    @Temporal(TemporalType.DATE)
    @Column(name = "order_date") 
    private Date orderDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "predict_receive_date") 
    private Date predictReceiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status") 
    private OrderStatus status;

    @Column(name = "shipping_address") 
    private String shippingAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Đảm bảo đây là tên cột foreign key đúng
    @JsonIgnoreProperties({"password", "token", "otpCode", "otpExpiration",
                           "addresses", "comments", "favorite", "cart", "orders", // Thêm "orders" nếu User có list orders
                           "hibernateLazyInitializer", "handler"})
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true) 
    @JsonManagedReference
    private List<OrderLine> orderLines;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method") 
    private PaymentMethod paymentMethod;

    @Column(name = "reviewed") 
    private Boolean reviewed = false;

}