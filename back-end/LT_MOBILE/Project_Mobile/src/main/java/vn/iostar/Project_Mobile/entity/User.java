package vn.iostar.Project_Mobile.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import vn.iostar.Project_Mobile.entity.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import vn.iostar.Project_Mobile.util.Type;
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "`User`")
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
  

    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String avatar;
    private String fullName;
    private String phone;
    private String token;
    private String otpCode;

    private LocalDateTime otpExpiration;
    private boolean active;
    @Enumerated(EnumType.STRING)
    private Type type;


    // Quan hệ một-Nhiều với Address
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonBackReference
    @ToString.Exclude
    private List<Address> addresses = new ArrayList<>();

    
 // Quan hệ 1-N: Một User có nhiều Comment
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Comment> comments = new ArrayList<>();

    
    
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private Cart cart;

    
}
