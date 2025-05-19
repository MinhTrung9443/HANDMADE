package vn.iostar.Project_Mobile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // Sử dụng profile test
public class ApplicationTests {

    @Test
    @DisplayName("Kiểm tra toàn bộ ứng dụng khởi động thành công")
    void contextLoads() {
        // Đây là kiểm tra mặc định để đảm bảo rằng ứng dụng Spring Boot khởi động thành công
        // và tất cả các bean được cấu hình đúng
    }
}
