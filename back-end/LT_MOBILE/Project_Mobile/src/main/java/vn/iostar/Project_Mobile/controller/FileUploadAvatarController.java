package vn.iostar.Project_Mobile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource; // Interface chung cho resource
import org.springframework.core.io.UrlResource; // Implementation cụ thể cho file URL
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller; // Dùng @Controller vì chủ yếu phục vụ resource
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody; // Cần để trả về body trực tiếp khi dùng @Controller
import vn.iostar.Project_Mobile.config.StorageProperties; // Import class cấu hình

import jakarta.servlet.http.HttpServletRequest; // Import để lấy mime type (cách khác)
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files; 
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/api") // Đường dẫn gốc cho các API phục vụ file (khớp với .path("/api") khi tạo URL)
public class FileUploadAvatarController {

    private final Path fileStorageLocation; // Nơi lưu trữ file (đọc từ cấu hình)

    @Autowired // Constructor Injection
    public FileUploadAvatarController(StorageProperties storageProperties) {
        // Lấy đường dẫn gốc đã được chuẩn hóa từ cấu hình
        this.fileStorageLocation = Paths.get(storageProperties.getLocation()).toAbsolutePath().normalize();
        System.out.println("File serving configured for directory: " + this.fileStorageLocation);
    }

    // Endpoint để truy cập file qua URL: GET /api/files/{filename:.+}
    // {filename:.+} cho phép tên file chứa dấu chấm (.)
    @GetMapping("/files/{filename:.+}") // Đường dẫn này khớp với .path("/api/files/") trong service
    @ResponseBody // Quan trọng: Trả về nội dung Resource trực tiếp, không qua view resolver
    public ResponseEntity<Resource> serveFile(@PathVariable String filename, HttpServletRequest request) {
        try {
            // 1. Tạo đường dẫn đầy đủ đến file được yêu cầu
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();

            // 2. Tạo đối tượng Resource từ đường dẫn file
            Resource resource = new UrlResource(filePath.toUri());

            // 3. Kiểm tra xem Resource có tồn tại và đọc được không
            if (resource.exists() && resource.isReadable()) {

                // 4. Xác định kiểu nội dung (MIME type) của file
                String contentType = null;
                try {
                    // Cách 1: Dùng Files.probeContentType (Khuyến nghị)
                    contentType = Files.probeContentType(filePath);
                    // Cách 2: Dùng HttpServletRequest (Cách cũ hơn)
                    // contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
                } catch (IOException ex) {
                    System.err.println("Could not determine file type for: " + filename);
                    // Không cần ném lỗi ở đây, sẽ dùng fallback
                }

                // Fallback nếu không xác định được MIME type
                if(contentType == null) {
                    contentType = "application/octet-stream"; // Kiểu mặc định cho dữ liệu nhị phân
                }
                System.out.println("Serving file: " + filename + " with Content-Type: " + contentType);

                // 5. Trả về ResponseEntity chứa Resource
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        // Optional: Thêm header Content-Disposition nếu muốn gợi ý tên file khi tải về
                        // Header này thường không cần thiết khi hiển thị ảnh trực tiếp
                        // .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"") // inline: gợi ý hiển thị trực tiếp
                        // .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"") // attachment: gợi ý tải về
                        .body(resource); // Body là nội dung file
            } else {
                // 4. Trả về lỗi 404 Not Found nếu file không tồn tại hoặc không đọc được
                System.err.println("File not found or not readable: " + filename + " at path: " + filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException ex) {
            // Lỗi xảy ra nếu đường dẫn file không hợp lệ để tạo URI (hiếm gặp với đường dẫn đã chuẩn hóa)
             System.err.println("Malformed URL for file path: " + filename);
            return ResponseEntity.badRequest().build(); // Trả về lỗi 400 Bad Request
        } catch (Exception ex) {
             // Bắt các lỗi không mong muốn khác
            System.err.println("Error serving file " + filename + ": " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Trả về lỗi 500
        }
    }
}