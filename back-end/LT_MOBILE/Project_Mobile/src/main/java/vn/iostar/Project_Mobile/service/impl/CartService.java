package vn.iostar.Project_Mobile.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iostar.Project_Mobile.DTO.ProductInfoDTO;
import vn.iostar.Project_Mobile.DTO.SelectedItemDetailDTO;
import vn.iostar.Project_Mobile.entity.Cart;
import vn.iostar.Project_Mobile.entity.CartItem;
import vn.iostar.Project_Mobile.entity.Product;
import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.repository.*;
        import vn.iostar.Project_Mobile.service.ICartService;

@Service
public class CartService implements ICartService{
    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private CartItemRepository itemRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private IUserRepository userRepo; // Keep IUserRepository

    private User getUserById(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    @Override
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        User user = getUserById(userId);
        Optional<Cart> cartOpt = cartRepo.findByUser_UserId(userId); 

        Cart cart;
        if (cartOpt.isPresent()) { 
            cart = cartOpt.get(); 
            logger.debug("Found existing cart ID: {} for user ID: {}", cart.getCartId(), userId);
        } else {
            logger.info("Creating new cart for user ID: {}", userId);
            cart = new Cart();
            cart.setUser(user);
            cart = cartRepo.save(cart);
            logger.info("New cart created with ID: {}", cart.getCartId());
        }
        return cart; 
    }

    @Override
    @Transactional
    public Cart addToCart(Long userId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(userId); // userId is now validated from token
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        // 🔒 Stock check logic remains the same
        int stock = product.getQuantity();
        Optional<CartItem> optional = itemRepo.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), productId);
        int existingQty = optional.map(CartItem::getQuantity).orElse(0);

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        if (existingQty + quantity > stock) {
            throw new RuntimeException("Số lượng yêu cầu (" + (existingQty + quantity) + ") vượt quá số lượng sản phẩm trong kho (" + stock + "). Bạn đã có " + existingQty + " trong giỏ.");
        }

        CartItem item;
        if (optional.isPresent()) {
            item = optional.get();
            item.setQuantity(existingQty + quantity);
        } else {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
        }

        itemRepo.save(item);
        return cartRepo.findById(cart.getCartId())
                .orElseThrow(() -> new RuntimeException("Cart not found after adding item, ID: " + cart.getCartId())); // Should not happen normally
    }


    @Override
    @Transactional
    public Cart updateCartItem(Long userId, Long productId, int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive. To remove, use the remove endpoint.");
        }

        Cart cart = getOrCreateCart(userId); // userId validated from token
        CartItem item = itemRepo.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), productId)
                .orElseThrow(() -> new RuntimeException("Item with Product ID " + productId + " not found in cart for user ID " + userId));

        Product product = item.getProduct();
        if (newQuantity > product.getQuantity()) {
            throw new RuntimeException("Số lượng yêu cầu (" + newQuantity + ") vượt quá số lượng sản phẩm trong kho (" + product.getQuantity() + ").");
        }

        item.setQuantity(newQuantity);
        itemRepo.save(item);
        return cartRepo.findById(cart.getCartId())
                .orElseThrow(() -> new RuntimeException("Cart not found after updating item, ID: " + cart.getCartId()));
    }

    @Override
    @Transactional
    public void removeCartItem(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId); // userId validated from token
        CartItem item = itemRepo.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), productId)
                .orElseThrow(() -> new RuntimeException("Item with Product ID " + productId + " not found in cart for user ID " + userId + ". Cannot remove.")); // More specific error
        itemRepo.delete(item);
    }

    @Override
    @Transactional(readOnly = true) // Good practice for read operations
    public List<CartItem> getCartItems(Long userId) {
    	Optional<Cart> cartOpt = cartRepo.findByUser_UserId(userId); 
    	return cartOpt.map(cart -> { // Nếu cart tồn tại (isPresent)
            logger.debug("Fetching all items for cart ID: {}", cart.getCartId());
            return itemRepo.findByCart_CartId(cart.getCartId());
        })
        .orElseGet(() -> { // Nếu cartOpt rỗng (isEmpty)
            logger.debug("Cart not found for user ID: {}, returning empty list.", userId);
            return new ArrayList<>(); // Trả về list rỗng
        });
    }
    
    
    @Override
    @Transactional(readOnly = true) 
    public List<SelectedItemDetailDTO> getDetailsForSelectedItems(User user, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            logger.warn("User {} requested details for an empty or null list of cart item IDs.", user.getUserId());
            return new ArrayList<>(); // Trả về list rỗng nếu không có ID nào được yêu cầu
        }

        logger.debug("Fetching details for cart items {} for user {}", cartItemIds, user.getUserId());

        // 1. Tìm giỏ hàng của user (Dùng Optional)
        Optional<Cart> userCartOpt = cartRepo.findByUser_UserId(user.getUserId());
        if (userCartOpt.isEmpty()) {
            logger.warn("Cart not found for user ID: {} when fetching selected item details.", user.getUserId());
            return new ArrayList<>(); 
        }
        Cart userCart = userCartOpt.get();

        List<CartItem> validCartItems = itemRepo.findByCartItemIdInAndCart_CartId(cartItemIds, userCart.getCartId());

        if (validCartItems.size() != cartItemIds.size()) {
            List<Long> foundIds = validCartItems.stream().map(CartItem::getCartItemId).collect(Collectors.toList());
            List<Long> missingOrInvalidIds = cartItemIds.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toList());
            logger.warn("Could not find all requested cart items for user {}. Missing/Invalid IDs: {}", user.getUserId(), missingOrInvalidIds);
            // Quyết định: Ném lỗi hay chỉ trả về những cái tìm thấy? Ở đây ta chỉ trả về cái tìm thấy.
        }

        // 4. Map từ Entity sang DTO Response
        List<SelectedItemDetailDTO> results = validCartItems.stream()
                .map(item -> {
                    ProductInfoDTO productInfo = null;
                    Product product = item.getProduct();
                    if (product != null) {
                        productInfo = new ProductInfoDTO(
                                product.getProductId(),
                                product.getImage(), 
                                product.getName(),
                                product.getPrice() 
                        );
                    } else {
                        logger.error("CartItem ID {} is associated with a null Product!", item.getCartItemId());
                        // Xử lý trường hợp product bị null nếu có thể xảy ra
                    }
                    return new SelectedItemDetailDTO(
                            item.getCartItemId(),
                            item.getQuantity(),
                            productInfo // productInfo có thể là null nếu product bị null
                    );
                })
                .collect(Collectors.toList());

        logger.debug("Returning {} detailed items for user {}", results.size(), user.getUserId());
        return results;
    }
}




