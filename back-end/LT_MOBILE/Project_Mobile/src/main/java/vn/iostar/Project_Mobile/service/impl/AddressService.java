package vn.iostar.Project_Mobile.service.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iostar.Project_Mobile.DTO.AddressDTO;
import vn.iostar.Project_Mobile.DTO.AddressInputDTO;
import vn.iostar.Project_Mobile.entity.Address;
import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.exception.ResourceNotFoundException;
import vn.iostar.Project_Mobile.repository.AddressRepository;
import vn.iostar.Project_Mobile.repository.IUserRepository;
import vn.iostar.Project_Mobile.service.IAddressService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
public class AddressService implements IAddressService {
@Autowired
private AddressRepository addressRepository;

@Autowired
private IUserRepository userRepository;

private AddressDTO convertToDTO(Address address) {
    AddressDTO dto = new AddressDTO();
    dto.setAddressId(address.getAddressId());
    dto.setRecipientName(address.getRecipientName());
    dto.setRecipientPhone(address.getRecipientPhone());
    dto.setStreetAddress(address.getStreetAddress());
    dto.setWard(address.getWard());
    dto.setDistrict(address.getDistrict());
    dto.setCity(address.getCity());
    dto.setCountry(address.getCountry());
    dto.setDefault(address.isDefault());
    return dto;
}

private Address convertToEntity(AddressInputDTO dto, User user) {
    Address address = new Address();
    address.setRecipientName(dto.getRecipientName());
    address.setRecipientPhone(dto.getRecipientPhone());
    address.setStreetAddress(dto.getStreetAddress());
    address.setWard(dto.getWard());
    address.setDistrict(dto.getDistrict());
    address.setCity(dto.getCity());
    address.setCountry(dto.getCountry());
    address.setDefault(false); // Default to false when creating
    address.setUser(user); // Associate with the user
    return address;
}

@Override
@Transactional(readOnly = true)
public List<AddressDTO> getAddressesByUserId(Long userId) {
    if (!userRepository.existsById(userId)) {
         // Ném ResourceNotFoundException nếu người dùng không tồn tại
         // Tùy logic, có thể chỉ trả về empty list nếu không tìm thấy user
         throw new ResourceNotFoundException("User not found with id: " + userId);
    }

    List<Address> addresses = addressRepository.findByUser_UserId(userId);

    return addresses.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
}

@Override
@Transactional(readOnly = true)
public AddressDTO getAddressByIdAndUserId(Long addressId, Long userId){
    Optional<Address> addressOpt = addressRepository.findByAddressIdAndUser_UserId(addressId, userId);

    Address address = addressOpt.orElseThrow(
            () -> new ResourceNotFoundException("Address not found with id: " + addressId + " for user id: " + userId));

    return convertToDTO(address);
}

@Override
@Transactional
public AddressDTO addAddress(Long userId, AddressInputDTO addressInputDTO) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Address newAddress = convertToEntity(addressInputDTO, user);

    List<Address> existingAddresses = addressRepository.findByUser_UserId(userId);
    // Logic: Set as default if input requests it OR if this is the first address
    boolean shouldSetAsDefault = addressInputDTO.getIsDefault() || existingAddresses.isEmpty();

    if (shouldSetAsDefault) {
        // Find the current default address (if any) and unset it
        addressRepository.findByUser_UserIdAndIsDefaultTrue(userId)
                .ifPresent(currentDefault -> {
                    currentDefault.setDefault(false);
                    addressRepository.save(currentDefault); // <<< LƯU TRẠNG THÁI MẶC ĐỊNH CŨ VÀO DB
                });
        newAddress.setDefault(true); // Set the new address as default
    } else {
         newAddress.setDefault(false); // Not setting as default
    }

    Address savedAddress = addressRepository.save(newAddress); // Save the new address

    return convertToDTO(savedAddress);
}

@Override
@Transactional
public AddressDTO updateAddress(Long addressId, Long userId, AddressInputDTO addressInputDTO) {
    // 1. Tìm địa chỉ hiện có và đảm bảo nó thuộc về người dùng
    Address existingAddress = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId + " for user id: " + userId));

    // 2. Cập nhật các trường từ DTO
    existingAddress.setRecipientName(addressInputDTO.getRecipientName());
    existingAddress.setRecipientPhone(addressInputDTO.getRecipientPhone());
    existingAddress.setStreetAddress(addressInputDTO.getStreetAddress());
    existingAddress.setWard(addressInputDTO.getWard());
    existingAddress.setDistrict(addressInputDTO.getDistrict());
    existingAddress.setCity(addressInputDTO.getCity());
    existingAddress.setCountry(addressInputDTO.getCountry());
    //existingAddress.setGoongPlaceId(addressInputDTO.getGoongPlaceId()); // Nếu có trường goongPlaceId

    // 3. Xử lý logic địa chỉ mặc định nếu trạng thái mặc định bị thay đổi trong input
    boolean inputWantsDefault = addressInputDTO.getIsDefault();
    boolean isCurrentlyDefault = existingAddress.isDefault();

    if (inputWantsDefault && !isCurrentlyDefault) {
        // Input muốn địa chỉ này là mặc định, và trước đây nó không phải
        // Tìm địa chỉ mặc định hiện tại và đặt nó thành false
        addressRepository.findByUser_UserIdAndIsDefaultTrue(userId)
                .ifPresent(currentDefault -> {
                	// Tránh cập nhật lại chính địa chỉ đang thao tác nếu nó TÌNH CỜ cũng được tìm thấy
                	// trong query findByUser_UserIdAndIsDefaultTrue (trường hợp này xảy ra nếu nó VỪA MỚI được set default trong transaction này)
                	// Nhưng thường thì query này sẽ tìm địa chỉ MẶC ĐỊNH khác.
                	// Tuy nhiên, việc kiểm tra ID là an toàn.
                	if (currentDefault.getAddressId() != existingAddress.getAddressId()) { // So sánh giá trị long
                        currentDefault.setDefault(false);
                        addressRepository.save(currentDefault); // <<< LƯU TRẠNG THÁI MẶC ĐỊNH CŨ VÀO DB
                     }
                });
        existingAddress.setDefault(true); // Đặt địa chỉ này làm mặc định
    } else if (!inputWantsDefault && isCurrentlyDefault) {
         // Input muốn địa chỉ này KHÔNG là mặc định, nhưng hiện tại nó LÀ
         // Nếu đây là địa chỉ mặc định DUY NHẤT, hành động này có thể không hợp lệ tùy business rule.
         // Code hiện tại cho phép, để người dùng tự đặt lại mặc định sau.
         // Cần kiểm tra thêm nếu business rule yêu cầu LUÔN CÓ một địa chỉ mặc định.
         existingAddress.setDefault(false); // Bỏ đặt mặc định cho địa chỉ này
    }
    // Nếu inputWantsDefault == isCurrentlyDefault, không cần thay đổi logic trạng thái mặc định ở đây.

    // 4. Lưu địa chỉ đã cập nhật
    Address updatedAddress = addressRepository.save(existingAddress);

    // 5. Chuyển đổi Entity đã cập nhật trở lại DTO và trả về
    return convertToDTO(updatedAddress);
}

@Override
@Transactional
public void deleteAddress(Long addressId, Long userId) {
    // 1. Tìm địa chỉ và đảm bảo nó thuộc về người dùng
    Address addressToDelete = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId + " for user id: " + userId));

    boolean wasDefault = addressToDelete.isDefault();

    addressRepository.delete(addressToDelete); // Xóa địa chỉ

    // 2. Nếu địa chỉ bị xóa là mặc định, tìm một địa chỉ khác và đặt làm mặc định mới (nếu còn địa chỉ)
    if (wasDefault) {
        List<Address> remainingAddresses = addressRepository.findByUser_UserId(userId);
        if (!remainingAddresses.isEmpty()) {
            // Chọn địa chỉ đầu tiên làm mặc định mới. Logic này có thể tùy chỉnh (ví dụ: địa chỉ được tạo gần nhất, v.v.)
            Address newDefaultAddress = remainingAddresses.get(0);
            newDefaultAddress.setDefault(true);
            addressRepository.save(newDefaultAddress); // Lưu địa chỉ mặc định mới
        }
        // Nếu remainingAddresses rỗng, người dùng không còn địa chỉ nào -> không có địa chỉ mặc định, hợp lý.
    }
}

@Override
@Transactional
public void setDefaultAddress(Long addressId, Long userId) {
    Address addressToSetDefault = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId + " for user id: " + userId));

    // Nếu địa chỉ đã là mặc định, không cần làm gì nữa
    if (addressToSetDefault.isDefault()) {
        return;
    }

    // Tìm địa chỉ mặc định hiện tại (nếu có) và bỏ đặt mặc định cho nó
    addressRepository.findByUser_UserIdAndIsDefaultTrue(userId)
            .ifPresent(currentDefault -> {
                currentDefault.setDefault(false); // <<< ĐẶT setDefault(false) TRƯỚC KHI save
                addressRepository.save(currentDefault); // <<< LƯU TRẠNG THÁI MẶC ĐỊNH CŨ VÀO DB
            });

    // Đặt địa chỉ được yêu cầu làm mặc định mới
    addressToSetDefault.setDefault(true);
    addressRepository.save(addressToSetDefault); // Lưu trạng thái mặc định mới
}
}