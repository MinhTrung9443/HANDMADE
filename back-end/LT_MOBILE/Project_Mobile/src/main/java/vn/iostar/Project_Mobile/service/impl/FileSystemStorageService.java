package vn.iostar.Project_Mobile.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // Sử dụng StringUtils của Spring
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import vn.iostar.Project_Mobile.config.StorageProperties;
import vn.iostar.Project_Mobile.service.IFileStorageService; // Import interface

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service 
public class FileSystemStorageService implements IFileStorageService { 

    private final Path rootLocation;

    @Autowired 
    public FileSystemStorageService(StorageProperties properties) {
        if (properties.getLocation() == null || properties.getLocation().trim().isEmpty()) {
            throw new RuntimeException("Storage location must be configured in application properties (storage.location).");
        }
        this.rootLocation = Paths.get(properties.getLocation().trim());

        try {
            Files.createDirectories(this.rootLocation);
            System.out.println("Storage directory initialized at: " + this.rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location: " + properties.getLocation(), e);
        }
    }

    @Override
    public String storeFile(MultipartFile file) throws IOException { // Khai báo throws IOException như trong interface
        if (file == null || file.isEmpty()) {
            return null; 
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename.contains("..")) {
            throw new IOException("Cannot store file with relative path outside current directory: " + originalFilename);
        }

        // Tạo tên file duy nhất (UUID + đuôi file gốc)
        String extension = Optional.ofNullable(originalFilename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(originalFilename.lastIndexOf(".") + 1))
                .map(String::toLowerCase)
                .orElse(""); // Nếu không có đuôi file

        String filename = UUID.randomUUID().toString() + "." + extension;

        Path destinationFile = this.rootLocation.resolve(filename).normalize().toAbsolutePath();

        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            throw new IOException("Cannot store file outside configured directory.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING); // Ghi đè nếu file đã tồn tại (hiếm khi xảy ra với UUID)
        }

        // Tạo và trả về URL đầy đủ để truy cập file
        // URL này giả định bạn có một Controller (ví dụ: FileUploadController)
        // lắng nghe tại "/api/files/{filename}"
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath() // Lấy base URL (http://host:port)
                .path("/api/files/") // Đường dẫn của endpoint phục vụ file (CẦN KHỚP VỚI FileUploadController)
                .path(filename)     // Tên file đã lưu
                .toUriString();      // Build thành chuỗi URL

        System.out.println("Stored file " + originalFilename + " as " + filename + " accessible at: " + fileDownloadUri);
        return fileDownloadUri;
    }
}