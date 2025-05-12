package vn.iostar.doan.activity;

// --- Đảm bảo có đủ các import này ---
import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor; // Cho getOriginalFileName
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns; // Cho getOriginalFileName
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection; // Để lấy MIME type
import java.util.Optional; // Dùng trong prepareImagePart
import java.util.UUID; // Dùng trong prepareImagePart
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.iostar.doan.R;
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.model.User;
import vn.iostar.doan.modelResponse.*;
import vn.iostar.doan.modelRequest.*;
import android.annotation.SuppressLint; // Cho getOriginalFileName

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private EditText etFullName, etPhone;
    private ImageView ivAvatar;
    private Button saveProfileButton;
    private ProgressBar progressBar;
    private String currentAvatarUrl, token;
    private Uri selectedImageUri = null;
    private String uploadedAvatarUrl = null; // Lưu URL sau khi upload thành công

    // --- ActivityResultLaunchers ---
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    selectedImageUri = result.getData().getData();
                    uploadedAvatarUrl = null; // Reset URL đã upload khi chọn ảnh mới
                    ivAvatar.setImageURI(selectedImageUri);
                    Log.i(TAG, "New image selected: " + selectedImageUri);
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) { openGallery(); }
                else { Toast.makeText(this, "Permission denied to read storage", Toast.LENGTH_SHORT).show(); }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etFullName = findViewById(R.id.editFullNameEditText);
        etPhone = findViewById(R.id.editPhoneEditText);
        ivAvatar = findViewById(R.id.editAvatarImageView);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        progressBar = findViewById(R.id.progressBar);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            token = extras.getString("TOKEN");
            currentAvatarUrl = extras.getString("CURRENT_AVATAR_URL");
            etFullName.setText(extras.getString("CURRENT_FULL_NAME"));
            etPhone.setText(extras.getString("CURRENT_PHONE"));
            if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                Glide.with(this).load(currentAvatarUrl).circleCrop().placeholder(R.drawable.ic_launcher_foreground).into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
            }
            Log.i(TAG, "Token received: " + (token != null && !token.isEmpty()));
            Log.i(TAG, "Current Avatar URL: " + currentAvatarUrl);
        } else {
            Log.e(TAG, "No extras received in Intent. Cannot proceed.");
            Toast.makeText(this, "Lỗi tải dữ liệu hồ sơ.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ivAvatar.setOnClickListener(v -> checkStoragePermissionAndOpenGallery());
        saveProfileButton.setOnClickListener(v -> {
            saveProfileChanges(); // Gọi hàm lưu chính
        });
    }

    private void checkStoragePermissionAndOpenGallery() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        saveProfileButton.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        // Có thể vô hiệu hóa EditText khi đang loading nếu muốn
        // etFullName.setEnabled(!isLoading);
        // etPhone.setEnabled(!isLoading);
        // ivAvatar.setEnabled(!isLoading);
    }

    // --- Hàm chính xử lý lưu thay đổi ---
    private void saveProfileChanges() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (fullName.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token is missing. Cannot proceed.");
            Toast.makeText(this, "Lỗi xác thực. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true); // Hiện loading

        // Reset URL đã upload trước đó
        uploadedAvatarUrl = null;

        if (selectedImageUri != null) {
            // Có ảnh mới -> Upload trước
            Log.i(TAG, "New image selected. Starting upload...");
            uploadImageFirst(selectedImageUri, fullName, phone);
        } else {
            // Không có ảnh mới -> Update trực tiếp với URL cũ
            Log.i(TAG, "No new image. Updating profile with current avatar: " + currentAvatarUrl);
            callUpdateProfileApi(fullName, phone, currentAvatarUrl);
        }
    }

    // --- Hàm Upload Ảnh ---
    private void uploadImageFirst(Uri imageUri, String fullName, String phone) {
        MultipartBody.Part imagePart;
        try {
            // Tên part là "file" khớp với @RequestParam("file") backend
            imagePart = prepareImagePart("file", imageUri);
            if (imagePart == null) {
                Toast.makeText(this, "Không thể chuẩn bị file ảnh.", Toast.LENGTH_SHORT).show();
                showLoading(false);
                return;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error preparing image part", e);
            Toast.makeText(this, "Lỗi xử lý ảnh.", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        Log.i(TAG, "Calling upload image API (/api/upload/image)...");
        // Gọi API upload (phải có trong ApiService)
        ApiService.apiService.uploadAvatarImage("Bearer " + token, imagePart) // Gửi token
                .enqueue(new Callback<ImageUploadResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ImageUploadResponse> call, @NonNull Response<ImageUploadResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getImageUrl() != null) {
                            uploadedAvatarUrl = response.body().getImageUrl();
                            Log.i(TAG, "Image uploaded successfully. New URL: " + uploadedAvatarUrl);
                            // Sau khi upload thành công -> Gọi API cập nhật profile với URL MỚI
                            callUpdateProfileApi(fullName, phone, uploadedAvatarUrl);
                        } else {
                            // Upload thất bại
                            String errorMsg = "Upload ảnh thất bại: " + response.code();
                            try { if(response.errorBody() != null) errorMsg += " " + response.errorBody().string(); } catch (Exception e) {}
                            Log.e(TAG, errorMsg);
                            Toast.makeText(EditProfileActivity.this, "Upload ảnh thất bại! Mã lỗi: " + response.code(), Toast.LENGTH_LONG).show();
                            showLoading(false); // Dừng loading
                            // Quyết định: Không tiếp tục cập nhật profile nếu upload lỗi
                            Toast.makeText(EditProfileActivity.this, "Không thể cập nhật hồ sơ.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ImageUploadResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Image upload network error", t);
                        Toast.makeText(EditProfileActivity.this, "Lỗi mạng khi upload ảnh: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        showLoading(false); // Dừng loading
                        Toast.makeText(EditProfileActivity.this, "Không thể cập nhật hồ sơ.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- Hàm Cập Nhật Profile (Gửi JSON) ---
    private void callUpdateProfileApi(String fullName, String phone, String avatarUrlToSend) {
        // Tạo DTO Request
        UpdateProfileRequest updateRequest = new UpdateProfileRequest(fullName, phone, avatarUrlToSend);

        Log.i(TAG, "Calling update profile API (/api/auth/update) with JSON...");
        Log.d(TAG, "Update Request Body: {fullName=" + fullName + ", phone=" + phone + ", avatar=" + avatarUrlToSend + "}");

        // Gọi API update profile (phải có trong ApiService)
        ApiService.apiService.updateProfile("Bearer " + token, updateRequest)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                        showLoading(false); // Luôn ẩn loading sau khi có response
                        if (response.isSuccessful() && response.body() != null) {
                            Log.i(TAG, "Profile updated successfully.");
                            Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();

                            Intent resultIntent = new Intent();
                            User updatedUser = response.body();
                            resultIntent.putExtra("UPDATED_FULL_NAME", updatedUser.getFullName());
                            resultIntent.putExtra("UPDATED_PHONE", updatedUser.getPhone());
                            resultIntent.putExtra("UPDATED_AVATAR_URL", updatedUser.getAvatar()); // Lấy URL mới nhất
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            String errorMsg = "Cập nhật hồ sơ thất bại: " + response.code();
                            try { if(response.errorBody() != null) errorMsg += " " + response.errorBody().string(); } catch (Exception e) {}
                            Log.e(TAG, errorMsg);
                            Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ thất bại! Mã lỗi: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                        showLoading(false); // Luôn ẩn loading sau khi có response
                        Log.e(TAG, "Update profile network error", t);
                        Toast.makeText(EditProfileActivity.this, "Lỗi mạng khi cập nhật: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --- Hàm chuẩn bị MultipartBody.Part (Khớp với code trước đó) ---
    private MultipartBody.Part prepareImagePart(String partName, Uri fileUri) throws IOException {
        ContentResolver contentResolver = getContentResolver();
        InputStream inputStream = contentResolver.openInputStream(fileUri);
        if (inputStream == null) { Log.e(TAG, "InputStream is null for URI: " + fileUri); return null; }

        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096]; int len;
        while ((len = inputStream.read(buffer)) != -1) { byteBuffer.write(buffer, 0, len); }
        byte[] inputData = byteBuffer.toByteArray();
        inputStream.close();

        String mimeType = contentResolver.getType(fileUri);
        if (mimeType == null) {
            try {
                InputStream testStream = contentResolver.openInputStream(fileUri);
                if (testStream != null) {
                    mimeType = URLConnection.guessContentTypeFromStream(testStream);
                    testStream.close();
                }
                if (mimeType == null) {
                    String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
                }
            } catch (Exception e) { Log.w(TAG, "Could not determine MIME type reliably", e); }
        }
        if (mimeType == null) { mimeType = "application/octet-stream"; Log.w(TAG, "MIME type fallback: " + mimeType); }
        Log.d(TAG, "Detected MIME type: " + mimeType);

        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), inputData);
        String originalFileName = getOriginalFileName(fileUri);
        String filename = UUID.randomUUID().toString();
        String extension = Optional.ofNullable(originalFileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".") + 1).toLowerCase())
                .orElse("");
        if (!extension.isEmpty()) { filename += "." + extension; }
        else {
            String guessedExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (guessedExtension != null) { filename += "." + guessedExtension; }
        }

        Log.d(TAG, "Prepared image part: name='" + partName + "', filename='" + filename + "', type='" + mimeType + "'");
        return MultipartBody.Part.createFormData(partName, filename, requestFile);
    }

    // --- Hàm lấy tên file gốc (Giữ nguyên) ---
    @SuppressLint("Range")
    private String getOriginalFileName(Uri uri) {
        String result = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) { Log.e(TAG, "Error getting original file name from ContentResolver", e); }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) { result = result.substring(cut + 1); }
        }
        return result;
    }
}