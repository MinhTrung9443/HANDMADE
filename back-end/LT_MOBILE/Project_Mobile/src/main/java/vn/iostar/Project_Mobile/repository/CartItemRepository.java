package vn.iostar.Project_Mobile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.iostar.Project_Mobile.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCart_CartIdAndProduct_ProductId(Long cartId, Long productId);
    List<CartItem> findByCartItemIdInAndCart_CartId(List<Long> ids, Long cartId);
    List<CartItem> findByCart_CartId(Long cartId);
    List<CartItem> findByCart_CartIdAndProduct_ProductIdIn(Long cartId, List<Long> productIds);

}