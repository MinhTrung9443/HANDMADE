package vn.iostar.Project_Mobile.config; // Hoặc package config của bạn

// --- THÊM CÁC IMPORT NÀY ---
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// --------------------------
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Khởi tạo Logger
    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    // Đọc lại đường dẫn upload từ application.properties
    @Value("${upload.path:/path/to/your/static/images}") // <<< Đảm bảo giá trị này đúng
    private String uploadPath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:8080")  // Ví dụ frontend của bạn
                .allowedMethods("GET", "POST", "PUT", "DELETE","PATCH");}
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối của thư mục upload
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath();
        String uploadLocation = uploadDir.toUri().toString(); // Phải là định dạng URI "file:/..."

        // Sử dụng logger đã khởi tạo
        log.info("Mapping /api/images/** to resource location: {}", uploadLocation); // Log để kiểm tra

        // Cấu hình: Bất kỳ request nào đến /api/images/**
        // sẽ tìm file trong thư mục vật lý uploadPath
        registry.addResourceHandler("/api/images/**") // <<< Đường dẫn web bạn muốn dùng để xem ảnh
                .addResourceLocations(uploadLocation); // <<< Thư mục thực tế trên ổ cứng (dạng URI)
    }
}
