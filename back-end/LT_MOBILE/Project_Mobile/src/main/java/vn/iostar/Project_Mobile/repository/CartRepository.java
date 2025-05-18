package vn.iostar.Project_Mobile.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.iostar.Project_Mobile.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser_UserId(Long userId);
    
}
