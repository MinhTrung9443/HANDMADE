package vn.iostar.Project_Mobile; // Hoặc package gốc của bạn

// Import các hàm assert
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test; // JUnit 5
// import org.slf4j.Logger; // Không cần Logger nữa
// import org.slf4j.LoggerFactory; // Không cần Logger nữa
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest // Tải context của Spring Boot để lấy Bean
public class TestPassword {

    // private static final Logger logger = LoggerFactory.getLogger(TestPassword.class); // Bỏ Logger

    @Autowired // Tiêm PasswordEncoder Bean
    private PasswordEncoder passwordEncoder;

    @Test
    void testBcryptHashing() {
        String plainPassword = "123";
        // Sử dụng System.out.println thay vì logger.info
        System.out.println("--- Bắt đầu Test Băm Mật Khẩu ---");

        // Băm mật khẩu
        String hashedPassword = passwordEncoder.encode(plainPassword);

        System.out.println("Mật khẩu gốc (Plain Text): '" + plainPassword + "'"); // In ra bằng println
        System.out.println("Mật khẩu đã băm (Hashed): '" + hashedPassword + "'"); // In ra bằng println

        // Các hàm assert vẫn giữ nguyên để kiểm tra tính đúng đắn của test
        assertNotNull(hashedPassword);
        assertFalse(hashedPassword.isEmpty());
        assertTrue(passwordEncoder.matches(plainPassword, hashedPassword),
                   "Mật khẩu gốc phải khớp với hash của chính nó");

        System.out.println("Kiểm tra khớp (matches): " + passwordEncoder.matches(plainPassword, hashedPassword)); // In kết quả matches

        System.out.println("--- Kết thúc Test Băm Mật Khẩu ---");
    }
}