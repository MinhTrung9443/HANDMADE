package vn.iostar.Project_Mobile.controller.Product;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.iostar.Project_Mobile.DTO.CartItemDetailsRequest;
import vn.iostar.Project_Mobile.DTO.CartItemRequest;
import vn.iostar.Project_Mobile.DTO.SelectedItemDetailDTO;
import vn.iostar.Project_Mobile.entity.Cart;
import vn.iostar.Project_Mobile.entity.CartItem;
import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.service.ICartService;
import vn.iostar.Project_Mobile.service.impl.CartService;
import vn.iostar.Project_Mobile.service.impl.UserServiceImpl;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    @Autowired
    private ICartService cartService;

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    
    @Autowired
    private UserServiceImpl userService;
    private Optional<User> getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authHeader.replace("Bearer ", "").trim();
        return userService.findByToken(token);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestHeader("Authorization") String authHeader,
                                       @RequestBody CartItemRequest request) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Bearer Token.");
        }
        User currentUser = userOpt.get();

        try {
            Cart cart = cartService.addToCart(currentUser.getUserId(), request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(cart); // Returning the whole cart object
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while adding to cart.");
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateCartItem(@RequestHeader("Authorization") String authHeader,
                                            @RequestBody CartItemRequest request) { // Use the same DTO
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Bearer Token.");
        }
        User currentUser = userOpt.get();

        try {
            Cart cart = cartService.updateCartItem(currentUser.getUserId(), request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(cart); // Return updated cart
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Log the exception e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating the cart item.");
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeCartItem(@RequestHeader("Authorization") String authHeader,
                                            @RequestParam Long productId) { // Only productId is needed now
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Bearer Token.");
        }
        User currentUser = userOpt.get();

        try {
            cartService.removeCartItem(currentUser.getUserId(), productId);
            return ResponseEntity.ok("Xóa sản phẩm khỏi giỏ hàng thành công");
        } catch (RuntimeException e) {
            // e.g., Item not found exception
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Log the exception e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while removing the cart item.");
        }
    }

    // Changed endpoint from "/{userId}" to "/items" or just "/"
    @GetMapping("/items") // Or just @GetMapping("/")
    public ResponseEntity<?> getCartItems(@RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Bearer Token.");
        }
        User currentUser = userOpt.get();

        try {
            List<CartItem> items = cartService.getCartItems(currentUser.getUserId());
            return ResponseEntity.ok(items); // Return list of items
        } catch (Exception e) {
            // Log the exception e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while retrieving cart items.");
        }
    }
    
    @PostMapping("/items/details")
    public ResponseEntity<?> getSelectedCartItemDetails(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CartItemDetailsRequest request) {

        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            logger.warn("Unauthorized attempt to get cart item details: Invalid token or header.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token không hợp lệ hoặc bị thiếu."));
        }
        User currentUser = userOpt.get();
        logger.info("User '{}' requesting details for cartItemIds: {}", currentUser.getUserId(), request.getCartItemIds());

        try {
            List<SelectedItemDetailDTO> itemDetails = cartService.getDetailsForSelectedItems(currentUser, request.getCartItemIds());
            logger.debug("Successfully fetched {} item details for user '{}'", itemDetails.size(), currentUser.getUserId());
            return ResponseEntity.ok(itemDetails); // Trả về danh sách DTO chi tiết

        } catch (NoSuchElementException e) {
            logger.warn("Not Found error while getting cart item details for user '{}': {}", currentUser.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error getting cart item details for user '{}'", currentUser.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống khi lấy chi tiết giỏ hàng."));
        }
    }
}
