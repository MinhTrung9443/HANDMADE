package vn.iostar.Project_Mobile.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception được ném ra khi một người dùng không được tìm thấy trong hệ thống.
 * Mặc định sẽ trả về mã lỗi HTTP 404 (NOT_FOUND) nếu không được xử lý bởi
 * một @ExceptionHandler cụ thể.
 */
@ResponseStatus(HttpStatus.NOT_FOUND) // Chú thích này giúp Spring MVC tự động trả về mã lỗi HTTP phù hợp
public class UserNotFoundException extends RuntimeException {

    /**
     * Constructor với một thông báo lỗi cụ thể.
     * @param message Thông báo mô tả chi tiết lỗi.
     */
    public UserNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor với thông báo lỗi và nguyên nhân gốc (cause).
     * @param message Thông báo mô tả chi tiết lỗi.
     * @param cause Nguyên nhân gốc của exception.
     */
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}