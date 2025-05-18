package vn.iostar.Project_Mobile.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface IFileStorageService {

	String storeFile(MultipartFile file) throws IOException;

}
