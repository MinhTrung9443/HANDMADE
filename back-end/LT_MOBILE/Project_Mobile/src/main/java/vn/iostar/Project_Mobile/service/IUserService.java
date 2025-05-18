package vn.iostar.Project_Mobile.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import vn.iostar.Project_Mobile.entity.User;

@Service
public interface IUserService {
	 Optional<User> findById(Long id);

	void saveOtp(User user, String otp);

	Optional<User> findByEmail(String email);
	Optional<User> findByUserIdentifier(String identifier);

	void saveUser(User user, String otp);

	boolean emailExists(String email);


	boolean verifyOtpRegister(String email, String otp);

	void saveActive(String email);

	boolean verifyOtpForgotPassword(String email, String otp);
	boolean resetPassword(String email, String newPassword);
	User save(User user);

	Optional<User> findByToken(String token);

}
