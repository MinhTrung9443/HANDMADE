package vn.iostar.Project_Mobile.service;

import java.util.List;

import vn.iostar.Project_Mobile.DTO.AddressDTO;
import vn.iostar.Project_Mobile.DTO.AddressInputDTO;

public interface IAddressService {
    List<AddressDTO> getAddressesByUserId(Long userId);
    AddressDTO getAddressByIdAndUserId(Long addressId, Long userId);
    AddressDTO addAddress(Long userId, AddressInputDTO addressInputDTO);
    AddressDTO updateAddress(Long addressId, Long userId, AddressInputDTO addressInputDTO);
    void deleteAddress(Long addressId, Long userId);
    void setDefaultAddress(Long addressId, Long userId) ;
}