package vn.iostar.Project_Mobile.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.service.IFileStorageService;
import vn.iostar.Project_Mobile.service.IUserService;

import java.io.IOException;
import java.util.Collections; 
import java.util.Map;
import java.util.Optional; 

@RestController
@RequestMapping("/api/upload")
public class ImageUploadController {

    // Khai báo logger
    private static final Logger log = LoggerFactory.getLogger(ImageUploadController.class);

    private final IFileStorageService fileStorageService;
    private final IUserService userService; // Thêm UserService

    @Autowired // Cập nhật Constructor Injection
    public ImageUploadController(IFileStorageService fileStorageService, IUserService userService) {
        this.fileStorageService = fileStorageService;
        this.userService = userService; // Inject UserService
    }

    @PostMapping("/image")
    public ResponseEntity<Object> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Upload attempt without proper Authorization header.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body("Yêu cầu thiếu hoặc sai định dạng thông tin xác thực.");
        }

        String token = authHeader.substring(7); // Bỏ "Bearer "
        Optional<User> userOpt = userService.findByToken(token);

        if (!userOpt.isPresent()) {
            log.warn("Upload attempt with invalid or expired token: {}", token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ hoặc đã hết hạn.");
        }

        User uploadingUser = userOpt.get();
        log.info("Image upload authorized for user: {}", uploadingUser.getEmail());

        if (file == null || file.isEmpty()) {
            log.warn("User {} attempted to upload an empty file.", uploadingUser.getEmail());
            return ResponseEntity.badRequest().body("Vui lòng chọn một file để upload.");
        }

        try {
            log.info("User {} processing upload for file: {}", uploadingUser.getEmail(), file.getOriginalFilename());
            String imageUrl = fileStorageService.storeFile(file);

            if (imageUrl == null) {
                log.error("User {} encountered an error: storeFile returned null unexpectedly for file {}", uploadingUser.getEmail(), file.getOriginalFilename());
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                      .body("Không thể lưu file.");
            }

            log.info("User {} stored file {} successfully. URL: {}", uploadingUser.getEmail(), file.getOriginalFilename(), imageUrl);
            Map<String, String> response = Collections.singletonMap("imageUrl", imageUrl);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("IOException during file upload for user {} and file {}: {}", uploadingUser.getEmail(), file.getOriginalFilename(), e.getMessage(), e); // Log cả exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Upload file thất bại: Lỗi đọc/ghi file."); // Thông báo lỗi chung chung hơn cho client
        } catch (Exception e) {
             log.error("Unexpected error during file upload for user {} and file {}: {}", uploadingUser.getEmail(), file.getOriginalFilename(), e.getMessage(), e); // Log cả exception
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body("Lỗi không xác định phía server.");
        }
    }
}