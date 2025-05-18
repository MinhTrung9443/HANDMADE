package vn.iostar.Project_Mobile.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.iostar.Project_Mobile.entity.Address;
import vn.iostar.Project_Mobile.entity.Address;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
	Optional<Address> findByAddressIdAndUser_UserId(Long addressId, Long userId);
	List<Address> findByUser_UserId(Long userId);
    Optional<Address> findByUser_UserIdAndIsDefaultTrue(Long userId);
}