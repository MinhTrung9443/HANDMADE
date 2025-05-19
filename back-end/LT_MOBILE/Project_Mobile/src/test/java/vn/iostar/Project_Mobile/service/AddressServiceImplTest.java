package vn.iostar.Project_Mobile.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.iostar.Project_Mobile.DTO.AddressDTO;
import vn.iostar.Project_Mobile.DTO.AddressInputDTO;
import vn.iostar.Project_Mobile.entity.Address;
import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.exception.ResourceNotFoundException;
import vn.iostar.Project_Mobile.repository.AddressRepository;
import vn.iostar.Project_Mobile.repository.IUserRepository;
import vn.iostar.Project_Mobile.service.impl.AddressService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private AddressService addressService;

    private User testUser;
    private Address address1, address2_default;
    private AddressInputDTO addressInputDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("user@example.com");

        address1 = new Address(10L, "Recipient 1", "0123456781", "1 Street 1", "Ward A", "District X", "City P", "Country Z", false, testUser, null);
        address2_default = new Address(11L, "Recipient 2 Default", "0123456782", "2 Street 2", "Ward B", "District Y", "City Q", "Country Z", true, testUser, null);

        addressInputDTO = new AddressInputDTO(
                "New Recipient", "0987654321", "123 New Street",
                "New Ward", "New District", "New City", "New Country",
                false, null
        );
    }

    private AddressDTO convertToDTO(Address address) {
        if (address == null) return null;
        return new AddressDTO(address.getAddressId(), address.getRecipientName(), address.getRecipientPhone(),
                address.getStreetAddress(), address.getWard(), address.getDistrict(), address.getCity(),
                address.getCountry(), address.isDefault(), address.getGoongPlaceId());
    }

    @Test
    @DisplayName("Get Addresses By User ID - Success")
    void getAddressesByUserId_Success() {
        when(userRepository.existsById(testUser.getUserId())).thenReturn(true);
        when(addressRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Arrays.asList(address1, address2_default));

        List<AddressDTO> resultDTOs = addressService.getAddressesByUserId(testUser.getUserId());

        assertNotNull(resultDTOs);
        assertEquals(2, resultDTOs.size());
        List<Long> resultIds = resultDTOs.stream().map(AddressDTO::getAddressId).collect(Collectors.toList());
        assertTrue(resultIds.contains(address1.getAddressId()));
        assertTrue(resultIds.contains(address2_default.getAddressId()));
    }

    @Test
    @DisplayName("Get Addresses By User ID - User Not Found")
    void getAddressesByUserId_UserNotFound_ThrowsResourceNotFoundException() {
        Long nonExistentUserId = 99L;
        when(userRepository.existsById(nonExistentUserId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            addressService.getAddressesByUserId(nonExistentUserId);
        });
    }

    @Test
    @DisplayName("Get Address By ID And User ID - Success")
    void getAddressByIdAndUserId_Success() {
        when(addressRepository.findByAddressIdAndUser_UserId(address1.getAddressId(), testUser.getUserId()))
                .thenReturn(Optional.of(address1));

        AddressDTO resultDTO = addressService.getAddressByIdAndUserId(address1.getAddressId(), testUser.getUserId());

        assertNotNull(resultDTO);
        assertEquals(address1.getAddressId(), resultDTO.getAddressId());
        assertEquals(address1.getRecipientName(), resultDTO.getRecipientName());
    }

    @Test
    @DisplayName("Get Address By ID And User ID - Not Found")
    void getAddressByIdAndUserId_NotFound_ThrowsResourceNotFoundException() {
        Long nonExistentAddressId = 999L;
        when(addressRepository.findByAddressIdAndUser_UserId(nonExistentAddressId, testUser.getUserId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            addressService.getAddressByIdAndUserId(nonExistentAddressId, testUser.getUserId());
        });
    }

    @Test
    @DisplayName("Add Address - Not Default, User Has Existing Default")
    void addAddress_NotDefault_UserHasExistingDefault() {
        addressInputDTO.setIsDefault(false); // Thêm địa chỉ mới không phải mặc định

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(addressRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Arrays.asList(address1, address2_default)); // User đã có địa chỉ, 1 là default
        // findByUser_UserIdAndIsDefaultTrue sẽ được gọi nhưng không làm gì vì isDefault=false
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            saved.setAddressId(12L); // Gán ID giả
            return saved;
        });

        AddressDTO resultDTO = addressService.addAddress(testUser.getUserId(), addressInputDTO);

        assertNotNull(resultDTO);
        assertEquals(12L, resultDTO.getAddressId());
        assertEquals(addressInputDTO.getRecipientName(), resultDTO.getRecipientName());
        assertFalse(resultDTO.isDefault()); // Phải là false

        verify(addressRepository, times(1)).save(argThat(addr ->
                !addr.isDefault() && addr.getRecipientName().equals(addressInputDTO.getRecipientName())
        ));
        verify(addressRepository, never()).findByUser_UserIdAndIsDefaultTrue(anyLong()); // Không nên gọi khi isDefault=false và đã có address
    }

    @Test
    @DisplayName("Add Address - As Default, User Has Existing Default")
    void addAddress_AsDefault_UserHasExistingDefault() {
        addressInputDTO.setIsDefault(true); // Thêm địa chỉ mới và đặt làm mặc định

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(addressRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Arrays.asList(address1, address2_default));
        // Giả sử address2_default là địa chỉ mặc định hiện tại
        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(testUser.getUserId())).thenReturn(Optional.of(address2_default));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            // Nếu là address2_default được save lại (isDefault=false)
            if (saved.getAddressId() == address2_default.getAddressId()) {
                 assertFalse(saved.isDefault(), "Existing default should be unset");
            } else { // Nếu là địa chỉ mới được save (isDefault=true)
                saved.setAddressId(12L); // Gán ID giả cho địa chỉ mới
                assertTrue(saved.isDefault(), "New address should be set as default");
            }
            return saved;
        });

        AddressDTO resultDTO = addressService.addAddress(testUser.getUserId(), addressInputDTO);

        assertNotNull(resultDTO);
        assertEquals(12L, resultDTO.getAddressId());
        assertTrue(resultDTO.isDefault());

        // Verify address2_default (old default) was saved with isDefault=false
        verify(addressRepository, times(1)).save(address2_default);
        // Verify new address was saved with isDefault=true
        verify(addressRepository, times(1)).save(argThat(addr ->
                addr.isDefault() && addr.getRecipientName().equals(addressInputDTO.getRecipientName())
        ));
    }
    
    @Test
    @DisplayName("Add Address - First Address, Input isDefault=false, Should Become Default")
    void addAddress_FirstAddress_InputIsDefaultFalse_ShouldBecomeDefault() {
        addressInputDTO.setIsDefault(false);

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(addressRepository.findByUser_UserId(testUser.getUserId())).thenReturn(new ArrayList<>()); // No existing addresses
        // findByUser_UserIdAndIsDefaultTrue sẽ trả về empty vì không có địa chỉ nào
        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(testUser.getUserId())).thenReturn(Optional.empty());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            saved.setAddressId(13L);
            return saved;
        });

        AddressDTO resultDTO = addressService.addAddress(testUser.getUserId(), addressInputDTO);

        assertNotNull(resultDTO);
        assertEquals(13L, resultDTO.getAddressId());
        assertTrue(resultDTO.isDefault(), "First address should be default even if input isDefault is false");

        verify(addressRepository, times(1)).save(argThat(addr ->
                addr.isDefault() && addr.getRecipientName().equals(addressInputDTO.getRecipientName())
        ));
    }

    @Test
    @DisplayName("Add Address - First Address, Input isDefault=true, Should Become Default")
    void addAddress_FirstAddress_InputIsDefaultTrue_ShouldBecomeDefault() {
        addressInputDTO.setIsDefault(true);

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(addressRepository.findByUser_UserId(testUser.getUserId())).thenReturn(new ArrayList<>()); // No existing addresses
        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(testUser.getUserId())).thenReturn(Optional.empty());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            saved.setAddressId(14L);
            return saved;
        });

        AddressDTO resultDTO = addressService.addAddress(testUser.getUserId(), addressInputDTO);

        assertNotNull(resultDTO);
        assertEquals(14L, resultDTO.getAddressId());
        assertTrue(resultDTO.isDefault(), "First address should be default");

        verify(addressRepository, times(1)).save(argThat(addr ->
                addr.isDefault() && addr.getRecipientName().equals(addressInputDTO.getRecipientName())
        ));
    }


    @Test
    @DisplayName("Update Address - Make It Default")
    void updateAddress_MakeItDefault() {
        // address1 (isDefault=false) sẽ được cập nhật thành default
        // address2_default (isDefault=true) sẽ bị unset default
        AddressInputDTO updateToDefaultDTO = new AddressInputDTO(
                "Updated Recipient 1", "0111111111", "Updated Street 1",
                "Updated Ward A", "Updated District X", "Updated City P", "Country Z",
                true, null // isDefault = true
        );

        when(addressRepository.findByAddressIdAndUser_UserId(address1.getAddressId(), testUser.getUserId()))
                .thenReturn(Optional.of(address1)); // address1 hiện tại không phải default
        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(testUser.getUserId()))
                .thenReturn(Optional.of(address2_default)); // address2_default là default hiện tại

        // Mock save: một lần cho address2_default (isDefault=false), một lần cho address1 (isDefault=true)
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address savedAddr = invocation.getArgument(0);
            if (savedAddr.getAddressId() == address2_default.getAddressId()) {
                assertFalse(savedAddr.isDefault(), "Old default should be unset");
            } else if (savedAddr.getAddressId() == address1.getAddressId()) {
                assertTrue(savedAddr.isDefault(), "Updated address should be default");
                assertEquals(updateToDefaultDTO.getRecipientName(), savedAddr.getRecipientName());
            }
            return savedAddr;
        });

        AddressDTO resultDTO = addressService.updateAddress(address1.getAddressId(), testUser.getUserId(), updateToDefaultDTO);

        assertNotNull(resultDTO);
        assertTrue(resultDTO.isDefault());
        assertEquals(updateToDefaultDTO.getRecipientName(), resultDTO.getRecipientName());

        verify(addressRepository, times(1)).save(address2_default); // Old default
        verify(addressRepository, times(1)).save(address1);         // Updated address
    }

    @Test
    @DisplayName("Update Address - Unset Default")
    void updateAddress_UnsetDefault() {
        // address2_default (isDefault=true) sẽ được cập nhật thành isDefault=false
        AddressInputDTO updateToNotDefaultDTO = new AddressInputDTO(
                address2_default.getRecipientName(), address2_default.getRecipientPhone(), address2_default.getStreetAddress(),
                address2_default.getWard(), address2_default.getDistrict(), address2_default.getCity(), address2_default.getCountry(),
                false, null // isDefault = false
        );

        when(addressRepository.findByAddressIdAndUser_UserId(address2_default.getAddressId(), testUser.getUserId()))
                .thenReturn(Optional.of(address2_default)); // address2_default hiện tại là default
        // findByUser_UserIdAndIsDefaultTrue không cần thiết ở đây vì không có địa chỉ nào khác được set default

        when(addressRepository.save(any(Address.class))).thenReturn(address2_default);


        AddressDTO resultDTO = addressService.updateAddress(address2_default.getAddressId(), testUser.getUserId(), updateToNotDefaultDTO);

        assertNotNull(resultDTO);
        assertFalse(resultDTO.isDefault());

        verify(addressRepository, times(1)).save(address2_default);
        assertFalse(address2_default.isDefault(), "Address should no longer be default after update");
    }

    @Test
    @DisplayName("Update Address - No Change in Default Status (Remains Default)")
    void updateAddress_NoChangeInDefaultStatus_RemainsDefault() {
        addressInputDTO.setIsDefault(true); // Input muốn nó là default
        address2_default.setDefault(true);  // Và nó đã là default

        when(addressRepository.findByAddressIdAndUser_UserId(address2_default.getAddressId(), testUser.getUserId()))
                .thenReturn(Optional.of(address2_default));
        when(addressRepository.save(any(Address.class))).thenReturn(address2_default);

        AddressDTO resultDTO = addressService.updateAddress(address2_default.getAddressId(), testUser.getUserId(), addressInputDTO);

        assertNotNull(resultDTO);
        assertTrue(resultDTO.isDefault());
        assertEquals(addressInputDTO.getRecipientName(), resultDTO.getRecipientName()); // Kiểm tra các trường khác được cập nhật
        verify(addressRepository, times(1)).save(address2_default);
        // Không có lời gọi nào để unset default cho địa chỉ khác
        verify(addressRepository, never()).findByUser_UserIdAndIsDefaultTrue(argThat(id -> !id.equals(testUser.getUserId())));
    }

    @Test
    @DisplayName("Delete Address - Was Default, Set New Default")
    void deleteAddress_WasDefault_SetNewDefault() {
        // Xóa address2_default (đang là default), address1 sẽ trở thành default mới
        when(addressRepository.findByAddressIdAndUser_UserId(address2_default.getAddressId(), testUser.getUserId()))
                .thenReturn(Optional.of(address2_default));
        doNothing().when(addressRepository).delete(address2_default);
        // Sau khi xóa, findByUser_UserId sẽ trả về danh sách còn lại (chỉ có address1)
        when(addressRepository.findByUser_UserId(testUser.getUserId())).thenReturn(Arrays.asList(address1));
        when(addressRepository.save(address1)).thenReturn(address1); // Mock save cho address1 khi nó được set default

        assertDoesNotThrow(() -> addressService.deleteAddress(address2_default.getAddressId(), testUser.getUserId()));

        verify(addressRepository, times(1)).delete(address2_default);
        verify(addressRepository, times(1)).findByUser_UserId(testUser.getUserId()); // Để tìm new default
        verify(addressRepository, times(1)).save(address1);
        assertTrue(address1.isDefault(), "address1 should be the new default");
    }

    @Test
    @DisplayName("Delete Address - Was Default, No Other Addresses Left")
    void deleteAddress_WasDefault_NoOtherAddressesLeft() {
        when(addressRepository.findByAddressIdAndUser_UserId(address2_default.getAddressId(), testUser.getUserId()))
                .thenReturn(Optional.of(address2_default));
        doNothing().when(addressRepository).delete(address2_default);
        when(addressRepository.findByUser_UserId(testUser.getUserId())).thenReturn(new ArrayList<>()); // Không còn địa chỉ nào

        assertDoesNotThrow(() -> addressService.deleteAddress(address2_default.getAddressId(), testUser.getUserId()));

        verify(addressRepository, times(1)).delete(address2_default);
        verify(addressRepository, times(1)).findByUser_UserId(testUser.getUserId());
        verify(addressRepository, never()).save(any(Address.class)); // Không có địa chỉ nào để set default
    }

    @Test
    @DisplayName("Delete Address - Not Default")
    void deleteAddress_NotDefault() {
        // Xóa address1 (không phải default)
        address1.setDefault(false); // Đảm bảo address1 không phải default
        when(addressRepository.findByAddressIdAndUser_UserId(address1.getAddressId(), testUser.getUserId()))
                .thenReturn(Optional.of(address1));
        doNothing().when(addressRepository).delete(address1);

        assertDoesNotThrow(() -> addressService.deleteAddress(address1.getAddressId(), testUser.getUserId()));

        verify(addressRepository, times(1)).delete(address1);
        verify(addressRepository, never()).findByUser_UserId(anyLong()); // Không cần tìm new default
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("Set Default Address - Success, Another Address Was Default")
    void setDefaultAddress_Success_AnotherWasDefault() {
        // address1 (hiện tại isDefault=false) sẽ được set thành default
        // address2_default (hiện tại isDefault=true) sẽ bị unset
        address1.setDefault(false);
        address2_default.setDefault(true);

        when(addressRepository.findByAddressIdAndUser_UserId(address1.getAddressId(), testUser.getUserId()))
                .thenReturn(Optional.of(address1));
        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(testUser.getUserId()))
                .thenReturn(Optional.of(address2_default)); // address2_default là default hiện tại

        // Mock save cho cả hai địa chỉ
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            if (saved.getAddressId() == address1.getAddressId()) {
                assertTrue(saved.isDefault());
            } else if (saved.getAddressId() == address2_default.getAddressId()) {
                assertFalse(saved.isDefault());
            }
            return saved;
        });

        assertDoesNotThrow(() -> addressService.setDefaultAddress(address1.getAddressId(), testUser.getUserId()));

        verify(addressRepository, times(1)).save(address1); // address1 được set default
        verify(addressRepository, times(1)).save(address2_default); // address2_default bị unset
        assertTrue(address1.isDefault());
        assertFalse(address2_default.isDefault());
    }

    @Test
    @DisplayName("Set Default Address - Target Address Already Default")
    void setDefaultAddress_TargetAlreadyDefault() {
        address2_default.setDefault(true); // address2_default đã là default

        when(addressRepository.findByAddressIdAndUser_UserId(address2_default.getAddressId(), testUser.getUserId()))
                .thenReturn(Optional.of(address2_default));

        // Không cần mock findByUser_UserIdAndIsDefaultTrue vì logic sẽ return sớm
        // Không có lời gọi save nào xảy ra

        assertDoesNotThrow(() -> addressService.setDefaultAddress(address2_default.getAddressId(), testUser.getUserId()));

        verify(addressRepository, never()).save(any(Address.class));
    }
    
    @Test
    @DisplayName("Set Default Address - No Other Address Was Default")
    void setDefaultAddress_NoOtherWasDefault() {
        address1.setDefault(false); // address1 chưa phải default
        // Giả sử không có địa chỉ nào khác là default
        when(addressRepository.findByAddressIdAndUser_UserId(address1.getAddressId(), testUser.getUserId()))
                .thenReturn(Optional.of(address1));
        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(testUser.getUserId()))
                .thenReturn(Optional.empty()); // Không có default nào khác
        when(addressRepository.save(address1)).thenReturn(address1);


        assertDoesNotThrow(() -> addressService.setDefaultAddress(address1.getAddressId(), testUser.getUserId()));

        verify(addressRepository, times(1)).save(address1);
        assertTrue(address1.isDefault());
        // findByUser_UserIdAndIsDefaultTrue được gọi, nhưng không có ifPresent nào được thực thi
        verify(addressRepository, times(1)).findByUser_UserIdAndIsDefaultTrue(testUser.getUserId());
    }

}