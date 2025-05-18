package vn.iostar.Project_Mobile.service;

import java.util.List;

import vn.iostar.Project_Mobile.DTO.CommentResponse;
import vn.iostar.Project_Mobile.entity.Comment;
import vn.iostar.Project_Mobile.entity.Order;
import vn.iostar.Project_Mobile.entity.Product;

import vn.iostar.Project_Mobile.entity.User;

public interface ICommentService {
    Comment save(Comment comment);
    List<CommentResponse> getCommentsByProduct(Product product);
    void deleteComment(Long id);
    Comment createComment(Comment comment);
    boolean hasUserReviewedProduct(User user, Product product, Order order);
    public void resetReviewStatus(User user, Product product);

}
