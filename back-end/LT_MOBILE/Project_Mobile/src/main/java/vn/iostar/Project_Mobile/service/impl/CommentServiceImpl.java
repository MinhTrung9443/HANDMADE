package vn.iostar.Project_Mobile.service.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Quan trọng

import vn.iostar.Project_Mobile.DTO.CommentResponse;
import vn.iostar.Project_Mobile.entity.Comment;
import vn.iostar.Project_Mobile.entity.Order;
import vn.iostar.Project_Mobile.entity.Product;
import vn.iostar.Project_Mobile.entity.User; // Thêm User
import vn.iostar.Project_Mobile.entity.OrderLine; // Thêm OrderLine

import vn.iostar.Project_Mobile.repository.*;
import vn.iostar.Project_Mobile.repository.IUserRepository; // Thêm repository cho User
import vn.iostar.Project_Mobile.service.ICommentService;
import vn.iostar.Project_Mobile.util.OrderStatus; // Giả sử bạn có enum này và OrderLine có liên kết đến Order

@Service
public class CommentServiceImpl implements ICommentService {

	@Autowired
	private ICommentRepository commentRepository; // Giả sử đây là tên đúng của repo

	// Thêm các repository cần thiết
	 @Autowired
	    private IOrderRepository orderRepository;
	@Autowired
	private IUserRepository userRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private OrderLineRepository orderLineRepository; // Cần tạo file này

	@Override
	public Comment save(Comment comment) {
		// Bạn có thể cần logic kiểm tra trước khi lưu ở đây nếu dùng trực tiếp
		return commentRepository.save(comment);
	}

	@Override
	public List<CommentResponse> getCommentsByProduct(Product product) {
		return commentRepository.findByProduct(product).stream().map(comment -> {
			CommentResponse response = new CommentResponse();
			response.setCommentId(comment.getCommentId());
			response.setImage(comment.getImage());
			response.setContent(comment.getContent());
			response.setRating(comment.getRating());
			response.setCreatedAt(comment.getCreatedAt());
			if (comment.getUser() != null) {
				response.setFullname(comment.getUser().getFullName());
				response.setAvatar(comment.getUser().getAvatar());
			} else {
				response.setFullname("Anonymous User"); // Hoặc giá trị mặc định khác
				// response.setAvatar(null); // hoặc avatar mặc định
			}
			return response;
		}).collect(Collectors.toList());
	}

	@Override
	public void deleteComment(Long id) {
		commentRepository.deleteById(id);
	}

	@Override
	public boolean hasUserReviewedProduct(User user, Product product, Order order) {
		return commentRepository.existsByUserAndProductAndReviewedIsTrue(user, product);
	}
	 private static final List<OrderStatus> ELIGIBLE_STATUSES_FOR_REVIEW =
	            List.of(OrderStatus.RECEIVED, OrderStatus.DELIVERED);


	 @Transactional
	 public Comment createComment(Comment comment) {
	     if (comment.getOrder() != null) {
	         Order order = orderRepository.findById(comment.getOrder().getOrderId())
	                 .orElseThrow(() -> new RuntimeException("Order not found with id: " + comment.getOrder().getOrderId()));

	         if (order.getReviewed() != null && order.getReviewed()) {
	             comment.setReviewed(true);
	         }
	     }

	     // Lưu bình luận
	     Comment savedComment = commentRepository.save(comment);

	     // Gọi kiểm tra và cập nhật trạng thái đơn hàng
	     checkAndUpdateOrderStatusIfAllReviewed(comment.getUser(), comment.getProduct());

	     return savedComment;
	 }

	    private void checkAndUpdateOrderStatusIfAllReviewed(User user, Product commentedProduct) {
	        // 4. Find candidate orders
	        List<Order> candidateOrders = orderRepository.findCandidateOrdersForReviewCheck(
	                user, commentedProduct, ELIGIBLE_STATUSES_FOR_REVIEW);

	        // 5. Iterate through each candidate order
	        for (Order order : candidateOrders) {
	            // 6. Get all unique product IDs in this order
	            Set<Long> productIdsInOrder = order.getOrderLines().stream()
	                    .map(orderLine -> orderLine.getProduct().getProductId())
	                    .collect(Collectors.toSet());

	            // 7. Count how many of these products the user has reviewed
	            long reviewedProductCount = commentRepository.countByUserAndProductInAndReviewedTrue(user, productIdsInOrder);

	            // 8. Check if all products in the order have been reviewed
	            if (reviewedProductCount == productIdsInOrder.size()) {
	                // 9. Update the order status (Recommended)
	                order.setStatus(OrderStatus.REVIEWED);
	                // OR update the boolean flag (Alternative)
	                 order.setReviewed(true);

	                orderRepository.save(order); // Persist the change
	            }
	        }
	    }
	    @Override
	    public void resetReviewStatus(User user, Product product) {
	        List<Comment> comments = commentRepository.findByUserAndProduct(user, product);
	        for (Comment comment : comments) {
	            comment.setReviewed(false);
	            commentRepository.save(comment);
	        }
	    }


}