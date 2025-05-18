package vn.iostar.Project_Mobile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import vn.iostar.Project_Mobile.DTO.AddressDTO; 
import vn.iostar.Project_Mobile.DTO.AddressInputDTO; 
import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.repository.IUserRepository;
import vn.iostar.Project_Mobile.service.IUserService;
import vn.iostar.Project_Mobile.service.impl.AddressService;
import jakarta.validation.Valid; 
import java.security.Principal; 
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;


    @Autowired
    private IUserService userService; 
    private Optional<User> getUserByToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authHeader.replace("Bearer ", "").trim();
        return userService.findByToken(token);
    }

    @GetMapping
    public ResponseEntity<?> getUserAddresses(@RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserByToken(authHeader);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ hoặc người dùng không tồn tại.");
        }
        User currentUser = userOpt.get();
        Long userId = currentUser.getUserId(); 

        List<AddressDTO> addresses = addressService.getAddressesByUserId(userId); // Gọi Service với userId
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<?> getAddressById(@PathVariable Long addressId,
                                            @RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserByToken(authHeader);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ hoặc người dùng không tồn tại.");
        }

        User currentUser = userOpt.get();
        Long userId = currentUser.getUserId(); 

        try {
            AddressDTO address = addressService.getAddressByIdAndUserId(addressId, userId);
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } 
    }

    @PostMapping
    public ResponseEntity<?> addAddress(@RequestHeader("Authorization") String authHeader,
                                         @Valid @RequestBody AddressInputDTO addressInputDTO) {
        Optional<User> userOpt = getUserByToken(authHeader);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ hoặc người dùng không tồn tại.");
        }
        User currentUser = userOpt.get();
        Long userId = currentUser.getUserId(); // Lấy userId từ User tìm được

        AddressDTO newAddress = addressService.addAddress(userId, addressInputDTO); // Gọi Service với userId
        System.out.println("Received AddressInputDTO with isDefault: " + addressInputDTO.getIsDefault());
        return ResponseEntity.status(HttpStatus.CREATED).body(newAddress);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<?> updateAddress(@PathVariable Long addressId,
                                           @RequestHeader("Authorization") String authHeader,
                                           @Valid @RequestBody AddressInputDTO addressInputDTO) {
        Optional<User> userOpt = getUserByToken(authHeader);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ hoặc người dùng không tồn tại.");
        }

        User currentUser = userOpt.get();
        Long userId = currentUser.getUserId(); 

        try {
            AddressDTO updatedAddress = addressService.updateAddress(addressId, userId, addressInputDTO);
            return ResponseEntity.ok(updatedAddress);
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } 
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long addressId,
                                           @RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserByToken(authHeader);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ hoặc người dùng không tồn tại.");
        }

        User currentUser = userOpt.get();
        Long userId = currentUser.getUserId(); // Lấy userId từ User tìm được

        try {
            addressService.deleteAddress(addressId, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } 
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<?> setDefaultAddress(@PathVariable Long addressId,
                                              @RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserByToken(authHeader);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ hoặc người dùng không tồn tại.");
        }

        User currentUser = userOpt.get();
        Long userId = currentUser.getUserId(); // Lấy userId từ User tìm được

        try {
            addressService.setDefaultAddress(addressId, userId);
            return ResponseEntity.ok().body("Địa chỉ đã được đặt làm mặc định."); // Có thể trả về thông báo thành công
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } 
    }
}