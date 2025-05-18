package vn.iostar.Project_Mobile.service;

import java.util.List;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import vn.iostar.Project_Mobile.DTO.CreateOrderRequest;
import vn.iostar.Project_Mobile.DTO.CreateOrderResponseDTO;
import vn.iostar.Project_Mobile.entity.Order;
import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.exception.ResourceNotFoundException;
import vn.iostar.Project_Mobile.repository.AddressRepository;
import vn.iostar.Project_Mobile.repository.CartItemRepository;
import vn.iostar.Project_Mobile.repository.CartRepository;
import vn.iostar.Project_Mobile.repository.OrderLineRepository;
import vn.iostar.Project_Mobile.repository.IOrderRepository;
import vn.iostar.Project_Mobile.repository.ProductRepository;


public interface IOrderService {

	CreateOrderResponseDTO createOrder(User currentUser, CreateOrderRequest request, HttpServletRequest httpServletRequest);

	Order getOrderDetailsById(Long orderId, User user);
	void handleVnpayIpn(Map<String, String> vnpayData, HttpServletRequest request)
		throws NumberFormatException, NoSuchElementException, IllegalStateException, IllegalArgumentException, RuntimeException;
		
	public List<Order> getOrdersByUserId(Long userId);
	Order getOrderDetailsById(Long orderId) throws ResourceNotFoundException;
   Order cancelOrder(Long orderId) throws ResourceNotFoundException, IllegalStateException;

}
