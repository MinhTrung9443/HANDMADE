package vn.iostar.Project_Mobile.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.repository.IUserRepository;
import vn.iostar.Project_Mobile.service.impl.UserServiceImpl;
import vn.iostar.Project_Mobile.util.Type;

@SpringBootTest
public class UserServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceTest.class);

    @Mock
    private IUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Khởi tạo user mẫu cho các test case
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setFullName("Test User");
        testUser.setActive(false);
        testUser.setType(Type.Regular);

        System.out.println("Test user initialized with email: " + testUser.getEmail());
        logger.info("Test setup complete. Test user created with ID: {}", testUser.getUserId());
    }

    @Test
    @DisplayName("Test tìm user theo email - tìm thấy")
    void testFindByEmail_WhenUserExists() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        logger.info("Starting test: findByEmail - user exists");

        // Act
        System.out.println("Executing findByEmail with: test@example.com");
        Optional<User> result = userService.findByEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        assertEquals("Test User", result.get().getFullName());

        System.out.println("Test findByEmail_WhenUserExists completed successfully");
        logger.info("User found successfully with email: {}", result.get().getEmail());
    }

    @Test
    @DisplayName("Test tìm user theo email - không tìm thấy")
    void testFindByEmail_WhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        logger.info("Starting test: findByEmail - user does not exist");

        // Act
        System.out.println("Searching for non-existent email: nonexistent@example.com");
        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(result.isPresent());

        System.out.println("Test completed: User not found as expected");
        logger.info("Verified that user with email 'nonexistent@example.com' was not found");
    }

    @Test
    @DisplayName("Test kiểm tra email tồn tại")
    void testEmailExists() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        logger.info("Starting email existence test");

        // Act & Assert
        System.out.println("Checking if test@example.com exists...");
        boolean existingEmailResult = userService.emailExists("test@example.com");
        System.out.println("Result: " + existingEmailResult);

        System.out.println("Checking if new@example.com exists...");
        boolean newEmailResult = userService.emailExists("new@example.com");
        System.out.println("Result: " + newEmailResult);

        assertTrue(existingEmailResult);
        assertFalse(newEmailResult);

        logger.info("Email existence test complete: existing email={}, new email={}",
                existingEmailResult, newEmailResult);
    }

    @Test
    @DisplayName("Test kích hoạt tài khoản")
    void testSaveActive() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        logger.info("Starting account activation test");

        // Act
        System.out.println("Activating account for: test@example.com");
        userService.saveActive("test@example.com");

        // Assert
        assertTrue(testUser.isActive());
        verify(userRepository, times(1)).save(testUser);

        System.out.println("Account activated successfully: " + testUser.isActive());
        logger.info("Account activation successful for email: {}", testUser.getEmail());
    }

    @Test
    @DisplayName("Test lưu OTP cho user")
    void testSaveOtp() {
        // Arrange
        String otp = "123456";
        LocalDateTime before = LocalDateTime.now();
        logger.info("Starting OTP save test with code: {}", otp);

        // Act
        System.out.println("Saving OTP code: " + otp + " for user: " + testUser.getEmail());
        userService.saveOtp(testUser, otp);

        // Assert
        assertEquals(otp, testUser.getOtpCode());
        assertNotNull(testUser.getOtpExpiration());
        assertTrue(testUser.getOtpExpiration().isAfter(before));
        verify(userRepository, times(1)).save(testUser);

        System.out.println("OTP saved with expiration: " + testUser.getOtpExpiration());
        logger.info("OTP successfully saved with expiration: {}", testUser.getOtpExpiration());
    }

    @Test
    @DisplayName("Test xác thực OTP đúng và chưa hết hạn")
    void testVerifyOtpRegister_Valid() {
        // Arrange
        testUser.setOtpCode("123456");
        testUser.setOtpExpiration(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        boolean result = userService.verifyOtpRegister("test@example.com", "123456");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Test xác thực OTP sai")
    void testVerifyOtpRegister_InvalidOtp() {
        // Arrange
        testUser.setOtpCode("123456");
        testUser.setOtpExpiration(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        boolean result = userService.verifyOtpRegister("test@example.com", "000000");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Test xác thực OTP hết hạn")
    void testVerifyOtpRegister_ExpiredOtp() {
        // Arrange
        testUser.setOtpCode("123456");
        testUser.setOtpExpiration(LocalDateTime.now().minusMinutes(10)); // OTP hết hạn

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        boolean result = userService.verifyOtpRegister("test@example.com", "123456");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Test reset password thành công")
    void testResetPassword_Success() {
        // Arrange
        String newPassword = "newPassword";
        String encodedPassword = "encodedNewPassword";

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        // Act
        boolean result = userService.resetPassword("test@example.com", newPassword);

        // Assert
        assertTrue(result);
        assertEquals(encodedPassword, testUser.getPassword());
        assertNull(testUser.getOtpCode());
        assertNull(testUser.getOtpExpiration());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Test reset password khi không tìm thấy user")
    void testResetPassword_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        boolean result = userService.resetPassword("nonexistent@example.com", "newPassword");

        // Assert
        assertFalse(result);
        verify(passwordEncoder, times(0)).encode(anyString());
        verify(userRepository, times(0)).save(any(User.class));
    }
}
