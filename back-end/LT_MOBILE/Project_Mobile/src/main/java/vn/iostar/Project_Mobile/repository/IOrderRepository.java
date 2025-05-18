package vn.iostar.Project_Mobile.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.iostar.Project_Mobile.entity.Order;
import vn.iostar.Project_Mobile.entity.Product;
import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.util.OrderStatus;

public interface IOrderRepository extends JpaRepository<Order, Long> {
	@Query("SELECT o FROM Order o WHERE o.user.userId = :userId")
	List<Order> findByUserId(@Param("userId") Long userId);

	List<Order> findByUser_UserId(Long userId);

	List<Order> findByUser_UserIdOrderByOrderDateDesc(Long userId);
	   @Query("SELECT DISTINCT o FROM Order o JOIN o.orderLines ol " +
	            "WHERE o.user = :user AND ol.product = :product " +
	            "AND o.status IN :eligibleStatuses AND o.reviewed = false") // or check o.status != OrderStatus.REVIEWED if using status
	     List<Order> findCandidateOrdersForReviewCheck(
	             @Param("user") User user,
	             @Param("product") Product product,
	             @Param("eligibleStatuses") List<OrderStatus> eligibleStatuses);
}
