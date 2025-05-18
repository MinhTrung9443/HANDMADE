package vn.iostar.Project_Mobile.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.iostar.Project_Mobile.entity.Comment;
import vn.iostar.Project_Mobile.entity.Order;
import vn.iostar.Project_Mobile.entity.Product;
import vn.iostar.Project_Mobile.entity.User;

public interface ICommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByProduct(Product product);
    boolean existsByUserAndProductAndReviewedIsTrue(User user, Product product);
	List<Comment> findByUserAndProductAndReviewedIsTrue(User user, Product productInOrder);

	     boolean existsByUserAndProduct(User user, Product product);
	     List<Comment> findByUserAndProduct(User user, Product product);
	   

	     long countByUserAndProductInAndReviewedTrue(User user, Set<Long> productIds);

	  
	 

}
