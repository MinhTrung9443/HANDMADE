package vn.iostar.doan.activity; // <<<< THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.iostar.doan.R; // <<<< Đảm bảo R đúng
import vn.iostar.doan.adapter.CommentAdapter;
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.model.Comment;
import vn.iostar.doan.modelRequest.CommentRequest;
import vn.iostar.doan.modelResponse.ImageUploadResponse;
import vn.iostar.doan.utils.SharedPreferencesUtils;

public class ProductRatingActivity extends AppCompatActivity {

    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private ProgressBar progressBar;
    private TextView tvNoComments;
    private Toolbar toolbar;
    private ImageView ivAttachComment;
    private EditText etCommentInput;
    private MaterialButton btnSendComment;
    private ImageView ivSelectedImagePreview;
    private ImageView ivClearSelectedImage;
    private RatingBar ratingBar;

    private long productId;
    private long currentUserId = 0L;
    private Uri selectedImageUri = null;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;

    private static final String TAG = "ProductRatingActivity";
    private static final String READ_STORAGE_PERMISSION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? Manifest.permission.READ_MEDIA_IMAGES
            : Manifest.permission.READ_EXTERNAL_STORAGE;

    private Call<ImageUploadResponse> imageUploadCall;
    private Call<Comment> createCommentCall;
    private Call<List<Comment>> fetchCommentsCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivityResultLaunchers();
        setContentView(R.layout.activity_product_rating);

        // 1. Lấy Product ID từ Intent
        productId = getIntent().getLongExtra("product_id", -1L); // Key này phải khớp

        // 2. Lấy User ID: Ưu tiên từ Intent, nếu không có thì từ SharedPreferences
        if (getIntent().hasExtra("user_id")) { // Key này phải khớp với key truyền từ OrderDetailActivity
            currentUserId = getIntent().getLongExtra("user_id", 0L);
            Log.i(TAG, "User ID received from Intent: " + currentUserId);
        } else {
            currentUserId = SharedPreferencesUtils.getLong(this, "user_id", 0L);
            Log.i(TAG, "User ID retrieved from SharedPreferences: " + currentUserId + " (as fallback or if not passed via Intent)");
        }


        if (productId == -1L) {
            Toast.makeText(this, "Invalid Product ID.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Invalid Product ID: " + productId);
            finish();
            return;
        }
        Log.i(TAG, "Activity started for Product ID: " + productId + ", User ID: " + currentUserId);

        toolbar = findViewById(R.id.toolbar);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        progressBar = findViewById(R.id.progressBar);
        tvNoComments = findViewById(R.id.tvNoComments);
        ivAttachComment = findViewById(R.id.ivAttachComment);
        etCommentInput = findViewById(R.id.etCommentInput);
        btnSendComment = findViewById(R.id.btnSendComment);
        ivSelectedImagePreview = findViewById(R.id.ivSelectedImagePreview);
        ivClearSelectedImage = findViewById(R.id.ivClearSelectedImage);
        ratingBar = findViewById(R.id.ratingBar);

        if (toolbar == null || recyclerViewComments == null || progressBar == null || tvNoComments == null ||
                ivAttachComment == null || etCommentInput == null || btnSendComment == null ||
                ivSelectedImagePreview == null || ivClearSelectedImage == null || ratingBar == null) {
            Log.e(TAG, "CRITICAL ERROR: One or more essential views not found! Check XML IDs.");
            Toast.makeText(this, "UI Error. Cannot continue.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "All essential views found successfully.");

        setupToolbar();
        setupRecyclerView();

        if (currentUserId <= 0) {
            Log.i(TAG, "User not logged in or invalid User ID (User ID: " + currentUserId + "). Hiding rating/comment input section.");
            hideRatingInputSection();
        } else {
            Log.i(TAG, "User logged in (User ID: " + currentUserId + "). Showing rating/comment input section.");
            showRatingInputSection();
            setupClickListeners();
            updateSendButtonState();
        }
        fetchComments();
    }

    private void hideRatingInputSection() {
        if (ratingBar != null) ratingBar.setVisibility(View.GONE);
        if (etCommentInput != null) etCommentInput.setVisibility(View.GONE);
        if (ivAttachComment != null) ivAttachComment.setVisibility(View.GONE);
        if (btnSendComment != null) btnSendComment.setVisibility(View.GONE);
        if (ivSelectedImagePreview != null) ivSelectedImagePreview.setVisibility(View.GONE);
        if (ivClearSelectedImage != null) ivClearSelectedImage.setVisibility(View.GONE);
    }

    private void showRatingInputSection() {
        if (ratingBar != null) ratingBar.setVisibility(View.VISIBLE);
        if (etCommentInput != null) etCommentInput.setVisibility(View.VISIBLE);
        if (ivAttachComment != null) ivAttachComment.setVisibility(View.VISIBLE);
        if (btnSendComment != null) btnSendComment.setVisibility(View.VISIBLE);
    }

    private void initActivityResultLaunchers() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Storage permission granted.");
                        launchImagePicker();
                    } else {
                        Log.w(TAG, "Storage permission denied.");
                        Toast.makeText(this, "Permission denied to access images.", Toast.LENGTH_SHORT).show();
                    }
                });

        pickMediaLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                Log.d(TAG, "Media selected: " + uri.toString());
                selectedImageUri = uri;
                if (ivSelectedImagePreview != null && ivClearSelectedImage != null && ratingBar != null && ratingBar.getVisibility() == View.VISIBLE) {
                    ivSelectedImagePreview.setImageURI(selectedImageUri);
                    ivSelectedImagePreview.setVisibility(View.VISIBLE);
                    ivClearSelectedImage.setVisibility(View.VISIBLE);
                } else {
                    Log.w(TAG, "Image picked, but input section is hidden or views are null. Preview not shown.");
                }
            } else {
                Log.d(TAG, "No media selected by user.");
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Comment & Rating");
        } else {
            Log.e(TAG, "SupportActionBar is null after setting toolbar!");
        }
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter(this); // Giả sử CommentAdapter không cần List ban đầu
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewComments.setAdapter(commentAdapter);
    }

    private void setupClickListeners() {
        if (ivAttachComment != null) {
            ivAttachComment.setOnClickListener(v -> checkPermissionAndPickImage());
        }
        if (btnSendComment != null) {
            btnSendComment.setOnClickListener(v -> attemptSendComment());
        }
        if (ivClearSelectedImage != null) {
            ivClearSelectedImage.setOnClickListener(v -> clearSelectedImage());
        }
        if (ratingBar != null) {
            ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
                if (fromUser) updateSendButtonState();
            });
        }
        if (etCommentInput != null) {
            etCommentInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) { updateSendButtonState(); }
            });
        }
    }

    private void updateSendButtonState() {
        if (btnSendComment == null || ratingBar == null) {
            Log.w(TAG, "updateSendButtonState: Button or RatingBar is null. Cannot update state.");
            return;
        }
        if (ratingBar.getVisibility() == View.GONE) {
            btnSendComment.setEnabled(false);
            return;
        }
        if (etCommentInput == null) {
            Log.e(TAG, "updateSendButtonState: etCommentInput is null unexpectedly!");
            btnSendComment.setEnabled(false);
            return;
        }
        boolean hasRating = ratingBar.getRating() > 0;
        boolean hasText = !etCommentInput.getText().toString().trim().isEmpty();
        boolean isLoading = progressBar != null && progressBar.getVisibility() == View.VISIBLE;
        boolean shouldBeEnabled = hasRating && hasText && !isLoading;
        if (btnSendComment.isEnabled() != shouldBeEnabled) {
            Log.d(TAG, "Updating Send Button state. Should be enabled: " + shouldBeEnabled);
            btnSendComment.setEnabled(shouldBeEnabled);
        }
    }

    private void checkPermissionAndPickImage() {
        if (progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, READ_STORAGE_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            launchImagePicker();
        } else {
            Log.d(TAG, "Requesting storage permission: " + READ_STORAGE_PERMISSION);
            if (requestPermissionLauncher != null) {
                requestPermissionLauncher.launch(READ_STORAGE_PERMISSION);
            } else {
                Log.e(TAG, "requestPermissionLauncher is null! Cannot request permission.");
            }
        }
    }

    private void launchImagePicker() {
        Log.d(TAG, "Launching media picker...");
        if (pickMediaLauncher != null) {
            pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } else {
            Log.e(TAG, "pickMediaLauncher is null! Cannot launch picker.");
        }
    }

    private void clearSelectedImage() {
        selectedImageUri = null;
        if (ivSelectedImagePreview != null) {
            ivSelectedImagePreview.setImageURI(null);
            ivSelectedImagePreview.setVisibility(View.GONE);
        }
        if (ivClearSelectedImage != null) {
            ivClearSelectedImage.setVisibility(View.GONE);
        }
        Log.d(TAG, "Cleared selected image.");
    }

    private void attemptSendComment() {
        if (currentUserId <= 0) {
            Log.e(TAG, "AttemptSendComment called but user is not logged in (ID=" + currentUserId + ").");
            return;
        }
        if (ratingBar == null || etCommentInput == null || btnSendComment == null) {
            Log.e(TAG,"AttemptSendComment: Critical views for input are null!");
            return;
        }
        int ratingValue = (int) ratingBar.getRating();
        String commentText = etCommentInput.getText().toString().trim();
        if (commentText.isEmpty()) {
            etCommentInput.setError("Comment cannot be empty");
            etCommentInput.requestFocus();
            Log.w(TAG, "Send attempt failed: Comment text is empty.");
            return;
        }
        if (ratingValue == 0) {
            Toast.makeText(this, "Please provide a rating (at least 1 star)", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Send attempt failed: Rating is zero.");
            return;
        }
        Log.d(TAG, "Comment validation passed. Proceeding to send...");
        showLoading(true);

        if (selectedImageUri != null) {
            Log.d(TAG, "Image selected, starting upload process...");
            uploadImageToServer(selectedImageUri, new ImageUploadCallback() {
                @Override
                public void onSuccess(String uploadedImageUrl) {
                    if (isFinishing() || isDestroyed()) {
                        Log.w(TAG, "uploadImageToServer onSuccess: Activity is finishing/destroyed. Aborting sendCommentData.");
                        showLoading(false);
                        return;
                    }
                    Log.i(TAG, "Image upload successful. Sending comment data with URL: " + uploadedImageUrl);
                    sendCommentData(commentText, ratingValue, uploadedImageUrl);
                }
                @Override
                public void onError(String error) {
                    if (isFinishing() || isDestroyed()) {
                        Log.w(TAG, "uploadImageToServer onError: Activity is finishing/destroyed. Ignoring error display.");
                        return;
                    }
                    Log.e(TAG, "Image upload failed: " + error);
                    Toast.makeText(ProductRatingActivity.this, "Image upload failed: " + error, Toast.LENGTH_LONG).show();
                    showLoading(false);
                }
            });
        } else {
            Log.d(TAG, "No image selected. Sending comment data without image URL.");
            sendCommentData(commentText, ratingValue, null);
        }
    }

    private interface ImageUploadCallback {
        void onSuccess(String imageUrl);
        void onError(String error);
    }

    private void uploadImageToServer(Uri imageUri, ImageUploadCallback callback) {
        Log.d(TAG, "Executing image upload for URI: " + imageUri);
        if (imageUri == null) {
            callback.onError("Image URI is null");
            return;
        }
        ContentResolver contentResolver = getContentResolver();
        String mimeType = contentResolver.getType(imageUri);
        if (mimeType == null) {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(imageUri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
            mimeType = (mimeType == null) ? "image/jpeg" : mimeType;
        }
        Log.d(TAG, "Using MIME type: " + mimeType);
        InputStream inputStream = null;
        try {
            inputStream = contentResolver.openInputStream(imageUri);
            if (inputStream == null) throw new IOException("Unable to open InputStream for URI: " + imageUri);
            byte[] fileBytes;
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] data = new byte[4096];
                int nRead;
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                fileBytes = buffer.toByteArray();
            }
            RequestBody requestFileBody = RequestBody.create(fileBytes, MediaType.parse(mimeType));
            String fileName = getFileNameFromUri(imageUri);
            Log.d(TAG, "Resolved filename for upload: " + fileName);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestFileBody);
            Log.d(TAG, "Calling uploadImage API endpoint...");
            cancelCall(imageUploadCall);
            imageUploadCall = ApiService.apiService.uploadImage(body);
            imageUploadCall.enqueue(new Callback<ImageUploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<ImageUploadResponse> call, @NonNull Response<ImageUploadResponse> response) {
                    if (isFinishing() || isDestroyed()) {
                        Log.w(TAG, "uploadImageToServer onResponse: Activity is finishing/destroyed. Ignoring callback.");
                        return;
                    }
                    if (response.isSuccessful() && response.body() != null && response.body().getImageUrl() != null && !response.body().getImageUrl().isEmpty()) {
                        String uploadedImageUrl = response.body().getImageUrl();
                        Log.i(TAG, "API Image upload successful. URL: " + uploadedImageUrl);
                        callback.onSuccess(uploadedImageUrl);
                    } else {
                        String errorMsg = "Server error during upload: " + response.code() + " - " + response.message();
                        String responseBodyString = "";
                        try { if (response.errorBody() != null) responseBodyString = response.errorBody().string(); } catch (IOException e) { /* ignore */ }
                        Log.e(TAG, errorMsg + (!responseBodyString.isEmpty() ? "\nError Body: "+responseBodyString : ""));
                        callback.onError("Upload failed: " + response.code());
                    }
                }
                @Override
                public void onFailure(@NonNull Call<ImageUploadResponse> call, @NonNull Throwable t) {
                    if (isFinishing() || isDestroyed()) {
                        Log.w(TAG, "uploadImageToServer onFailure: Activity is finishing/destroyed. Ignoring callback.");
                        return;
                    }
                    if (call.isCanceled()) {
                        Log.d(TAG, "uploadImageToServer onFailure: Call was cancelled.");
                    } else {
                        Log.e(TAG, "Upload network failure: " + t.getMessage(), t);
                        callback.onError("Network Error: " + t.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "IOException processing image URI: " + e.getMessage(), e);
            callback.onError("Error reading image file");
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError reading image", e);
            callback.onError("Image file is too large");
        } finally {
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { Log.e(TAG, "Error closing InputStream", e); }
            }
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri != null && ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (nameIndex != -1) result = cursor.getString(nameIndex);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error getting filename from ContentResolver for URI: " + uri, e);
            }
        }
        if (result == null && uri != null) result = uri.getLastPathSegment();
        if (result != null) result = result.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        return (result != null && !result.isEmpty()) ? result : ("upload_" + System.currentTimeMillis());
    }

    private void sendCommentData(String text, int rating, String imageUrl) {
        Log.d(TAG, "Preparing to send: Text=" + text + ", Rating=" + rating + ", ImageUrl=" + imageUrl + ", UserID=" + currentUserId + ", ProductID=" + productId);

        String authToken = SharedPreferencesUtils.getString(this, "token", null);

        if (authToken == null || authToken.isEmpty()) {
            showLoading(false);
            Toast.makeText(this, "Vui lòng đăng nhập để bình luận.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Cannot send comment: Auth token is missing.");
            updateSendButtonState();
            return;
        }
        String authHeader = "Bearer " + authToken;

        // === TẠO REQUEST BODY - SỬA THỨ TỰ ĐỐI SỐ CUỐI CÙNG ===
        // Constructor là: CommentRequest(String content, int rating, String image, long userId, long productId)
        // Phải truyền: currentUserId (cho userId) và productId (cho productId)
        CommentRequest requestBody = new CommentRequest(text, rating, imageUrl, currentUserId, productId); // <--- SỬA Ở ĐÂY! currentUserId TRƯỚC, productId SAU

        // Log the values *in* the object after setting them (Optional but good for debugging)
        Log.d(TAG, "CommentRequest object values after constructor: userId=" + requestBody.getUserId() + ", productId=" + requestBody.getProductId());


        cancelCall(createCommentCall);

        // === GỌI API VÀ TRUYỀN HEADER XÁC THỰC ===
        // Đảm bảo phương thức createComment trong ApiService interface CÓ @Header("Authorization")
        createCommentCall = ApiService.apiService.createComment(authHeader, requestBody);

        createCommentCall.enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(@NonNull Call<Comment> call, @NonNull Response<Comment> response) {
                // ... (phần xử lý response) ...
                if (isFinishing() || isDestroyed()) return;
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "Comment created successfully! ID: " + response.body().getCommentId());
                    Toast.makeText(ProductRatingActivity.this, "Review submitted!", Toast.LENGTH_SHORT).show();
                    if (etCommentInput != null) etCommentInput.setText("");
                    if (ratingBar != null) ratingBar.setRating(0);
                    clearSelectedImage();
                    fetchComments();
                } else {
                    Log.e(TAG, "Failed to create comment API error: " + response.code() + " - " + response.message());
                    String errorBody = "";
                    try { if (response.errorBody() != null) errorBody = response.errorBody().string(); } catch (Exception e) { /* ignore */ }
                    Log.e(TAG, "Create Comment Error Body: " + errorBody);

                    String userFriendlyError = "Failed to submit review: " + response.code();
                    if (response.code() == 401 || response.code() == 403) {
                        userFriendlyError = "Bạn không có quyền thực hiện thao tác này. Vui lòng đăng nhập lại.";
                    } else if (response.code() == 400) {
                        userFriendlyError = "Dữ liệu gửi đi không hợp lệ."; // Hoặc parse errorBody chi tiết hơn
                    } else if (!errorBody.isEmpty()) {
                        userFriendlyError += " - " + errorBody.substring(0, Math.min(errorBody.length(), 100));
                    }
                    Toast.makeText(ProductRatingActivity.this, userFriendlyError, Toast.LENGTH_LONG).show();
                }
                updateSendButtonState();
            }
            @Override
            public void onFailure(@NonNull Call<Comment> call, @NonNull Throwable t) {
                // ... (phần xử lý failure) ...
                if (isFinishing() || isDestroyed()) return;
                if (call.isCanceled()) {
                    Log.d(TAG, "Create comment call was cancelled.");
                } else {
                    Log.e(TAG, "Create comment NETWORK failure: " + t.getMessage(), t);
                    Toast.makeText(ProductRatingActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
                showLoading(false);
                updateSendButtonState();
            }
        });
    }

    private void fetchComments() {
        Log.d(TAG, "Fetching comments for product ID: " + productId);
        showLoading(true);
        cancelCall(fetchCommentsCall);
        fetchCommentsCall = ApiService.apiService.getCommentsByProduct(productId);
        fetchCommentsCall.enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(@NonNull Call<List<Comment>> call, @NonNull Response<List<Comment>> response) {
                if (isFinishing() || isDestroyed()) {
                    Log.w(TAG, "fetchComments onResponse: Activity is finishing/destroyed. Ignoring callback.");
                    return;
                }
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Comment> comments = response.body();
                    Log.d(TAG, "Successfully fetched " + comments.size() + " comments.");
                    if (comments.isEmpty()) {
                        if(tvNoComments != null) { tvNoComments.setText("Be the first to review!"); tvNoComments.setVisibility(View.VISIBLE); }
                        if (recyclerViewComments != null) recyclerViewComments.setVisibility(View.GONE);
                    } else {
                        if(tvNoComments != null) tvNoComments.setVisibility(View.GONE);
                        if (recyclerViewComments != null) recyclerViewComments.setVisibility(View.VISIBLE);
                        if (commentAdapter != null) commentAdapter.setComments(comments);
                    }
                } else {
                    Log.e(TAG, "Failed to load comments: " + response.code() + " - " + response.message());
                    if(tvNoComments != null) { tvNoComments.setText("Failed to load reviews."); tvNoComments.setVisibility(View.VISIBLE); }
                    if (recyclerViewComments != null) recyclerViewComments.setVisibility(View.GONE);
                    Toast.makeText(ProductRatingActivity.this, "Failed load: " + response.code(), Toast.LENGTH_SHORT).show();
                }
                updateSendButtonState();
            }
            @Override
            public void onFailure(@NonNull Call<List<Comment>> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    Log.w(TAG, "fetchComments onFailure: Activity is finishing/destroyed. Ignoring callback.");
                    return;
                }
                if (call.isCanceled()) {
                    Log.d(TAG, "fetchComments onFailure: Call was cancelled.");
                } else {
                    Log.e(TAG, "Error fetching comments (Network): " + t.getMessage(), t);
                    if(tvNoComments != null) { tvNoComments.setText("Network error loading reviews."); tvNoComments.setVisibility(View.VISIBLE); }
                    if (recyclerViewComments != null) recyclerViewComments.setVisibility(View.GONE);
                    Toast.makeText(ProductRatingActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
                showLoading(false);
                updateSendButtonState();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        Log.d(TAG, "Setting loading state to: " + isLoading);
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        else Log.w(TAG, "showLoading: progressBar is null!");
        boolean isInputSectionVisible = (ratingBar != null && ratingBar.getVisibility() == View.VISIBLE);
        if (isInputSectionVisible) {
            if (ivAttachComment != null) ivAttachComment.setEnabled(!isLoading);
            if (etCommentInput != null) etCommentInput.setEnabled(!isLoading);
            if (ratingBar != null) ratingBar.setEnabled(!isLoading);
            if (ivClearSelectedImage != null) ivClearSelectedImage.setEnabled(!isLoading);
        }
        updateSendButtonState();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return true;
        }
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Cancelling ongoing network calls.");
        cancelCall(imageUploadCall);
        cancelCall(createCommentCall);
        cancelCall(fetchCommentsCall);
    }

    private void cancelCall(Call<?> call) {
        if (call != null && !call.isCanceled() && !call.isExecuted()) {
            call.cancel();
        }
    }
}