package vn.iostar.Project_Mobile.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.iostar.Project_Mobile.DTO.ProductInfoDTO;
import vn.iostar.Project_Mobile.DTO.SelectedItemDetailDTO;
import vn.iostar.Project_Mobile.entity.Cart;
import vn.iostar.Project_Mobile.entity.CartItem;
import vn.iostar.Project_Mobile.entity.Product;
import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.repository.CartItemRepository;
import vn.iostar.Project_Mobile.repository.CartRepository;
import vn.iostar.Project_Mobile.repository.IUserRepository;
import vn.iostar.Project_Mobile.repository.ProductRepository;
import vn.iostar.Project_Mobile.service.impl.CartService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Cart testCart;
    private Product productA;
    private Product productB;
    private CartItem cartItemA_existing;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");

        testCart = new Cart();
        testCart.setCartId(10L);
        testCart.setUser(testUser);
        testCart.setCartItems(new ArrayList<>()); // Khởi tạo list rỗng

        productA = new Product();
        productA.setProductId(100L);
        productA.setName("Product A");
        productA.setPrice(50.0);
        productA.setQuantity(10); // Tồn kho

        productB = new Product();
        productB.setProductId(101L);
        productB.setName("Product B");
        productB.setPrice(75.0);
        productB.setQuantity(5);  // Tồn kho

        cartItemA_existing = new CartItem();
        cartItemA_existing.setCartItemId(200L);
        cartItemA_existing.setCart(testCart);
        cartItemA_existing.setProduct(productA);
        cartItemA_existing.setQuantity(2);
        // testCart.getCartItems().add(cartItemA_existing); // Không thêm trực tiếp ở đây, để test add mới
    }

    @Test
    @DisplayName("Get or Create Cart - Cart Exists")
    void getOrCreateCart_WhenCartExists() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));

        Cart resultCart = cartService.getOrCreateCart(testUser.getUserId());

        assertNotNull(resultCart);
        assertEquals(testCart.getCartId(), resultCart.getCartId());
        verify(cartRepository, times(1)).findByUser_UserId(testUser.getUserId());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Get or Create Cart - Cart Does Not Exist, Create New")
    void getOrCreateCart_WhenCartDoesNotExist_CreatesNew() {
        User newUser = new User(); newUser.setUserId(2L);
        Cart newCart = new Cart(); newCart.setCartId(11L); newCart.setUser(newUser);

        when(userRepository.findById(newUser.getUserId())).thenReturn(Optional.of(newUser));
        when(cartRepository.findByUser_UserId(newUser.getUserId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart); // Trả về cart đã được gán ID

        Cart resultCart = cartService.getOrCreateCart(newUser.getUserId());

        assertNotNull(resultCart);
        assertEquals(newCart.getCartId(), resultCart.getCartId());
        assertEquals(newUser.getUserId(), resultCart.getUser().getUserId());
        verify(cartRepository, times(1)).findByUser_UserId(newUser.getUserId());
        verify(cartRepository, times(1)).save(argThat(cart -> cart.getUser().getUserId().equals(newUser.getUserId())));
    }

    @Test
    @DisplayName("Add To Cart - New Product")
    void addToCart_NewProduct_Success() {
        int quantityToAdd = 3;

        // Mock getOrCreateCart (giả sử cart đã tồn tại)
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        // Mock productRepository
        when(productRepository.findById(productB.getProductId())).thenReturn(Optional.of(productB));
        // Mock cartItemRepository (sản phẩm B chưa có trong giỏ)
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(testCart.getCartId(), productB.getProductId()))
                .thenReturn(Optional.empty());
        // Mock save cart item
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem item = invocation.getArgument(0);
            item.setCartItemId(201L); // Gán ID giả
            return item;
        });
        // Mock cartRepo.findById để trả về cart sau khi item được thêm (quan trọng cho return của addToCart)
        when(cartRepository.findById(testCart.getCartId())).thenReturn(Optional.of(testCart));


        Cart updatedCart = cartService.addToCart(testUser.getUserId(), productB.getProductId(), quantityToAdd);

        assertNotNull(updatedCart);
        // Kiểm tra cartItemRepository.save được gọi với đúng thông tin
        verify(cartItemRepository, times(1)).save(argThat(item ->
                item.getProduct().getProductId()==(productB.getProductId()) &&
                item.getQuantity() == quantityToAdd &&
                item.getCart().getCartId()==(testCart.getCartId())
        ));
        // Do addToCart trả về cart, không phải list item, nên kiểm tra cart trả về là đủ
    }

    @Test
    @DisplayName("Add To Cart - Existing Product, Update Quantity")
    void addToCart_ExistingProduct_UpdatesQuantity() {
        int initialQuantity = cartItemA_existing.getQuantity(); // 2
        int quantityToAdd = 1; // Thêm 1
        int expectedQuantity = initialQuantity + quantityToAdd; // 3

        // Mock getOrCreateCart
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        // Mock productRepository
        when(productRepository.findById(productA.getProductId())).thenReturn(Optional.of(productA));
        // Mock cartItemRepository (sản phẩm A đã có trong giỏ)
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(testCart.getCartId(), productA.getProductId()))
                .thenReturn(Optional.of(cartItemA_existing));
        // Mock save cart item (sẽ được gọi để cập nhật quantity)
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItemA_existing);
        when(cartRepository.findById(testCart.getCartId())).thenReturn(Optional.of(testCart));


        Cart updatedCart = cartService.addToCart(testUser.getUserId(), productA.getProductId(), quantityToAdd);

        assertNotNull(updatedCart);
        verify(cartItemRepository, times(1)).save(cartItemA_existing);
        assertEquals(expectedQuantity, cartItemA_existing.getQuantity());
    }

    @Test
    @DisplayName("Add To Cart - Quantity Exceeds Stock")
    void addToCart_QuantityExceedsStock_ThrowsRuntimeException() {
        int quantityToAdd = productA.getQuantity() + 1; // Yêu cầu nhiều hơn tồn kho

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(productRepository.findById(productA.getProductId())).thenReturn(Optional.of(productA));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(testCart.getCartId(), productA.getProductId()))
                .thenReturn(Optional.empty()); // Giả sử thêm mới

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addToCart(testUser.getUserId(), productA.getProductId(), quantityToAdd);
        });
        assertTrue(exception.getMessage().contains("vượt quá số lượng sản phẩm trong kho"));
    }
    
    @Test
    @DisplayName("Add To Cart - Product Not Found")
    void addToCart_ProductNotFound_ThrowsRuntimeException() {
        Long nonExistentProductId = 999L;
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addToCart(testUser.getUserId(), nonExistentProductId, 1);
        });
        assertEquals("Product not found with ID: " + nonExistentProductId, exception.getMessage());
    }

    @Test
    @DisplayName("Update Cart Item - Success")
    void updateCartItem_Success() {
        int newQuantity = 3;
        // Giả sử cartItemA_existing (quantity=2) đã có trong giỏ
        testCart.getCartItems().add(cartItemA_existing); // Thêm vào để test update

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(testCart.getCartId(), productA.getProductId()))
                .thenReturn(Optional.of(cartItemA_existing));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItemA_existing);
        when(cartRepository.findById(testCart.getCartId())).thenReturn(Optional.of(testCart));


        Cart updatedCart = cartService.updateCartItem(testUser.getUserId(), productA.getProductId(), newQuantity);

        assertNotNull(updatedCart);
        verify(cartItemRepository, times(1)).save(cartItemA_existing);
        assertEquals(newQuantity, cartItemA_existing.getQuantity());
    }
    
    @Test
    @DisplayName("Update Cart Item - Quantity Exceeds Stock")
    void updateCartItem_QuantityExceedsStock_ThrowsRuntimeException() {
        int newQuantity = productA.getQuantity() + 5; // Vượt tồn kho

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(testCart.getCartId(), productA.getProductId()))
                .thenReturn(Optional.of(cartItemA_existing)); // Product A đã có trong giỏ

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.updateCartItem(testUser.getUserId(), productA.getProductId(), newQuantity);
        });
        assertTrue(exception.getMessage().contains("vượt quá số lượng sản phẩm trong kho"));
    }

    @Test
    @DisplayName("Update Cart Item - Item Not Found in Cart")
    void updateCartItem_ItemNotFoundInCart_ThrowsRuntimeException() {
        Long nonExistentProductIdInCart = productB.getProductId(); // Product B chưa có trong giỏ

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(testCart.getCartId(), nonExistentProductIdInCart))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.updateCartItem(testUser.getUserId(), nonExistentProductIdInCart, 1);
        });
        assertTrue(exception.getMessage().contains("not found in cart"));
    }
    
//    @Test
//    @DisplayName("Update Cart Item - Zero or Negative Quantity")
//    void updateCartItem_ZeroOrNegativeQuantity_ThrowsIllegalArgumentException() {
//        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
//        // Không cần mock cartRepository hay cartItemRepository vì lỗi sẽ được ném ra trước
//
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//            cartService.updateCartItem(testUser.getUserId(), productA.getProductId(), 0);
//        });
//        assertEquals("Quantity must be positive. To remove, use the remove endpoint.", exception.getMessage());
//
//        exception = assertThrows(IllegalArgumentException.class, () -> {
//            cartService.updateCartItem(testUser.getUserId(), productA.getProductId(), -1);
//        });
//        assertEquals("Quantity must be positive. To remove, use the remove endpoint.", exception.getMessage());
//    }

    @Test
    @DisplayName("Remove Cart Item - Success")
    void removeCartItem_Success() {
        // Giả sử cartItemA_existing có trong giỏ
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(testCart.getCartId(), productA.getProductId()))
                .thenReturn(Optional.of(cartItemA_existing));
        doNothing().when(cartItemRepository).delete(cartItemA_existing);

        assertDoesNotThrow(() -> {
            cartService.removeCartItem(testUser.getUserId(), productA.getProductId());
        });

        verify(cartItemRepository, times(1)).delete(cartItemA_existing);
    }

    @Test
    @DisplayName("Remove Cart Item - Item Not Found in Cart")
    void removeCartItem_ItemNotFoundInCart_ThrowsRuntimeException() {
        Long nonExistentProductIdInCart = productB.getProductId();

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(testCart.getCartId(), nonExistentProductIdInCart))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.removeCartItem(testUser.getUserId(), nonExistentProductIdInCart);
        });
        assertTrue(exception.getMessage().contains("not found in cart for user ID"));
        assertTrue(exception.getMessage().contains("Cannot remove."));
    }

//    @Test
//    @DisplayName("Get Cart Items - User Has Cart With Items")
//    void getCartItems_UserHasCartWithItems() {
//        CartItem cartItemB_new = new CartItem(); // Thêm một item nữa để test list
//        cartItemB_new.setCartItemId(202L);
//        cartItemB_new.setCart(testCart);
//        cartItemB_new.setProduct(productB);
//        cartItemB_new.setQuantity(1);
//
//        List<CartItem> expectedItems = Arrays.asList(cartItemA_existing, cartItemB_new);
//
//        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser)); // Cần cho getOrCreateCart
//        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
//        when(cartItemRepository.findByCart_CartId(testCart.getCartId())).thenReturn(expectedItems);
//
//        List<CartItem> actualItems = cartService.getCartItems(testUser.getUserId());
//
//        assertNotNull(actualItems);
//        assertEquals(2, actualItems.size());
//        assertTrue(actualItems.contains(cartItemA_existing));
//        assertTrue(actualItems.contains(cartItemB_new));
//    }

//    @Test
//    @DisplayName("Get Cart Items - User Has No Cart (or empty cart)")
//    void getCartItems_UserHasNoCartOrEmptyCart() {
//        // Trường hợp 1: User không có cart
//        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
//        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.empty());
//        // Không cần mock cartItemRepository.findByCart_CartId vì nó sẽ không được gọi
//
//        List<CartItem> actualItemsNoCart = cartService.getCartItems(testUser.getUserId());
//        assertNotNull(actualItemsNoCart);
//        assertTrue(actualItemsNoCart.isEmpty());
//
//        // Trường hợp 2: User có cart nhưng rỗng
//        testCart.setCartItems(new ArrayList<>()); // Đảm bảo cart rỗng
//        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
//        when(cartItemRepository.findByCart_CartId(testCart.getCartId())).thenReturn(new ArrayList<>());
//
//        List<CartItem> actualItemsEmptyCart = cartService.getCartItems(testUser.getUserId());
//        assertNotNull(actualItemsEmptyCart);
//        assertTrue(actualItemsEmptyCart.isEmpty());
//    }

    @Test
    @DisplayName("Get Details For Selected Items - Success")
    void getDetailsForSelectedItems_Success() {
        List<Long> selectedCartItemIds = Arrays.asList(cartItemA_existing.getCartItemId());
        // cartItemA_existing đã được setup

        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartItemIdInAndCart_CartId(selectedCartItemIds, testCart.getCartId()))
                .thenReturn(Arrays.asList(cartItemA_existing));

        List<SelectedItemDetailDTO> resultDTOs = cartService.getDetailsForSelectedItems(testUser, selectedCartItemIds);

        assertNotNull(resultDTOs);
        assertEquals(1, resultDTOs.size());
        SelectedItemDetailDTO dto = resultDTOs.get(0);
        assertEquals(cartItemA_existing.getCartItemId(), dto.getCartItemId());
        assertEquals(cartItemA_existing.getQuantity(), dto.getQuantity());
        assertNotNull(dto.getProduct());
        assertEquals(productA.getProductId(), dto.getProduct().getProductId());
        assertEquals(productA.getName(), dto.getProduct().getName());
    }

    @Test
    @DisplayName("Get Details For Selected Items - Empty ID List")
    void getDetailsForSelectedItems_EmptyIdList() {
        List<SelectedItemDetailDTO> resultDTOs = cartService.getDetailsForSelectedItems(testUser, new ArrayList<>());
        assertTrue(resultDTOs.isEmpty());

        resultDTOs = cartService.getDetailsForSelectedItems(testUser, null);
        assertTrue(resultDTOs.isEmpty());
    }

    @Test
    @DisplayName("Get Details For Selected Items - Cart Not Found")
    void getDetailsForSelectedItems_CartNotFound() {
        List<Long> selectedCartItemIds = Arrays.asList(cartItemA_existing.getCartItemId());
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.empty());

        List<SelectedItemDetailDTO> resultDTOs = cartService.getDetailsForSelectedItems(testUser, selectedCartItemIds);
        assertTrue(resultDTOs.isEmpty());
    }

    @Test
    @DisplayName("Get Details For Selected Items - Some IDs Not Found or Not Belonging to User's Cart")
    void getDetailsForSelectedItems_SomeIdsNotFoundOrNotBelonging() {
        Long validId = cartItemA_existing.getCartItemId();
        Long invalidId = 999L; // ID không tồn tại hoặc không thuộc giỏ hàng user
        List<Long> selectedCartItemIds = Arrays.asList(validId, invalidId);

        // Chỉ cartItemA_existing được tìm thấy và thuộc về giỏ hàng
        when(cartRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartItemIdInAndCart_CartId(selectedCartItemIds, testCart.getCartId()))
                .thenReturn(Arrays.asList(cartItemA_existing));

        List<SelectedItemDetailDTO> resultDTOs = cartService.getDetailsForSelectedItems(testUser, selectedCartItemIds);

        assertNotNull(resultDTOs);
        assertEquals(1, resultDTOs.size()); // Chỉ trả về item hợp lệ
        assertEquals(validId, resultDTOs.get(0).getCartItemId());
    }
}