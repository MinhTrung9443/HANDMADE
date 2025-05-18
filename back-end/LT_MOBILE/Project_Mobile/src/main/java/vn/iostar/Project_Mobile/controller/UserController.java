package vn.iostar.Project_Mobile.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.iostar.Project_Mobile.DTO.AddressDTO;
import vn.iostar.Project_Mobile.DTO.ForgotPasswordRequest;
import vn.iostar.Project_Mobile.DTO.LoginRequest;
import vn.iostar.Project_Mobile.DTO.UserUpdateDTO;
import vn.iostar.Project_Mobile.entity.Address;
import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.repository.AddressRepository;
import vn.iostar.Project_Mobile.service.IEmailService;
import vn.iostar.Project_Mobile.service.IUserService;
import vn.iostar.Project_Mobile.service.impl.AddressService;

@RestController
@RequestMapping("/api/auth")
public class UserController {

	@Autowired
	private AddressRepository addressRepository;
	@Autowired // Inject AddressService
    private AddressService addressService;

	 private final IUserService userService;
	    private final IEmailService emailService;
	    private final PasswordEncoder passwordEncoder;
	    @Autowired // Thêm AddressService vào constructor injection
	    public UserController(IUserService userService, IEmailService emailService, PasswordEncoder passwordEncoder, AddressService addressService) {
	        this.userService = userService;
	        this.emailService = emailService;
	        this.passwordEncoder = passwordEncoder;
            this.addressService = addressService; // Gán AddressService
	    }

	    @PostMapping("/login")
	    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
	        Optional<User> userOpt = userService.findByEmail(loginRequest.getEmail());

	        if (!userOpt.isPresent()) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                    .body("Email không tồn tại trong hệ thống!");
	        }
	        
	        User user = userOpt.get();
	       
	        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                    .body("Mật khẩu không chính xác!");
	        }
	        

	        String token = UUID.randomUUID().toString();

	        // ✅ Lưu token đó vào DB (thêm cột token trong bảng users)
	        user.setToken(token);
	        userService.save(user);
	        
	        // Log dữ liệu nếu bạn muốn kiểm tra
	        System.out.println("User từ database: " + user);

	        // Trả về dữ liệu người dùng
	        Map<String, Object> response = new HashMap<>();
	        response.put("message", "Đăng nhập thành công.");
	        response.put("email", user.getEmail());
	        response.put("fullName", user.getFullName());
	        response.put("userType", user.getType());
	        response.put("token", user.getToken());
	        // hoặc role gì đó nếu có
	        // response.put("password", user.getPassword()); // ❌ Không nên gửi password

	        return ResponseEntity.ok(response);
	    }
	    // Lấy thông tin của user để hiển thị
	    @GetMapping("/info")
	    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {
	        String token = authHeader.replace("Bearer ", "").trim();

	        Optional<User> userOpt = userService.findByToken(token);
	        if (!userOpt.isPresent()) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ.");
	        }

	        User user = userOpt.get();
	        List<AddressDTO> userAddresses = addressService.getAddressesByUserId(user.getUserId());

	        Map<String, Object> response = new HashMap<>();
			response.put("userId", user.getUserId());
	        response.put("email", user.getEmail());
	        response.put("fullName", user.getFullName());
	        response.put("phone", user.getPhone());
	        response.put("avatar", user.getAvatar());
 
            response.put("addresses", userAddresses);
	        return ResponseEntity.ok(response);
	    }

	    //update thông tin
	    @PutMapping("/update")
	    public ResponseEntity<?> updateUser( 
	            @RequestHeader("Authorization") String authHeader,
	            @RequestBody UserUpdateDTO updateRequest 
	    ) {
	        String token = authHeader.replace("Bearer ", "").trim();
	        Optional<User> userOpt = userService.findByToken(token);

	        if (!userOpt.isPresent()) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ.");
	        }

	        User user = userOpt.get();

	        user.setFullName(updateRequest.getFullName());
	        user.setPhone(updateRequest.getPhone());
	        user.setAvatar(updateRequest.getAvatar()); 
	        User updatedUser = userService.save(user); // Lưu và lấy đối tượng đã cập nhật

	        // Trả về đối tượng User đã cập nhật thay vì chuỗi String
	        return ResponseEntity.ok(updatedUser);
	    }

	    
	    @PostMapping("/forgot-password")
	    public ResponseEntity<User> forgotPassword(@RequestBody ForgotPasswordRequest request) {
	        Optional<User> userOpt = userService.findByEmail(request.getEmail());
	        if (!userOpt.isPresent()) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // If user not found, return empty response
	        }

	        // Generate OTP
	        String otp = emailService.generateOTP();

	        // Send OTP to the user's email
	        emailService.sendOtp(request.getEmail(), otp);

	        // Save OTP and expiration time to the database
	        userService.saveOtp(userOpt.get(), otp);

	        return ResponseEntity.ok(userOpt.get()); // Return the user object
	    }

	  
	    @PostMapping("/reset-password")
	    public ResponseEntity<User> resetPassword(@RequestBody ForgotPasswordRequest resetRequest) {
	        String email = resetRequest.getEmail();
	        String newPassword = resetRequest.getNewPassword(); // Đây là mật khẩu plain text
	        String confirmPassword = resetRequest.getConfirmPassword();

	        if (!newPassword.equals(confirmPassword)) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
	        }

	        // String encodedPassword = passwordEncoder.encode(newPassword); // << BỎ DÒNG NÀY HOẶC COMMENT LẠI

	        // Cập nhật mật khẩu: truyền mật khẩu plain text (newPassword) cho service
	        boolean isReset = userService.resetPassword(email, newPassword); // << THAY ĐỔI Ở ĐÂY
	        if (isReset) {
	            Optional<User> userOpt = userService.findByEmail(email);
	            return userOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
	        } else {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
	        }
	    }


	    @PostMapping("/verifyOtpForgotPassword")
	    public ResponseEntity<User> verifyOtpForgotPassword(@RequestBody ForgotPasswordRequest otpRequest) {
	        String email = otpRequest.getEmail();
	        String otp = otpRequest.getOtp();

	        // Kiểm tra OTP từ userService
	        boolean isOtpValid = userService.verifyOtpForgotPassword(email, otp);

	        if (isOtpValid) {
	            Optional<User> userOpt = userService.findByEmail(email);
	            return userOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)); // Return user if OTP is valid
	        } else {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Return empty response if OTP is invalid or expired
	        }
	    }


}
