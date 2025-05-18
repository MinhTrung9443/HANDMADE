package vn.iostar.doan.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.iostar.doan.R;
import vn.iostar.doan.adapter.AddressAdapter;
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.model.Address;

public class ShippingAddressActivity extends AppCompatActivity implements AddressAdapter.OnAddressActionListener {

    private static final String TAG = "ShippingAddressActivity";
    private boolean defaultAddressChanged = false;
    private RecyclerView addressesRecyclerView;
    private AddressAdapter addressAdapter;
    private List<Address> addressList = new ArrayList<>();
    private Button addNewAddressButton;
    private ImageView backButton;
    private TextView noAddressesTextView; // Add the TextView for empty state
    public static final String ACTION_DEFAULT_ADDRESS_CHANGED = "vn.iostar.doan.DEFAULT_ADDRESS_CHANGED";
    private String token;

    private ActivityResultLauncher<Intent> addressFormLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipping_address);

        token = getIntent().getStringExtra("TOKEN");
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Lỗi xác thực.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String authHeader = "Bearer " + token;
        Log.d(TAG, "Received Token for Address Activity.");

        AnhXa();
        setupRecyclerView();
        setupListeners(authHeader);

        addressFormLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "ActivityResult from AddressFormActivity. ResultCode: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        Log.i(TAG, "Address form activity finished with RESULT_OK. Refreshing address list.");
                        fetchAddresses(authHeader); // Re-fetch the list
                        defaultAddressChanged = true;
                        setResult(RESULT_OK); // Báo cho ProfileActivity
                        sendDefaultAddressChangedBroadcast();
                    } else {
                        Log.i(TAG, "Address form activity finished with non-OK result.");
                    }
                });

        fetchAddresses(authHeader);
    }

    private void AnhXa() {
        addressesRecyclerView = findViewById(R.id.addressesRecyclerView);
        addNewAddressButton = findViewById(R.id.addNewAddressButton);
        backButton = findViewById(R.id.backButton);
        noAddressesTextView = findViewById(R.id.noAddressesTextView);
    }

    private void setupRecyclerView() {
        addressesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        addressAdapter = new AddressAdapter(addressList, this); // 'this' refers to the activity implementing the interface
        addressesRecyclerView.setAdapter(addressAdapter);
    }

    private void setupListeners(String authHeader) {
        backButton.setOnClickListener(v -> finish()); // Go back to ProfileActivity

        addNewAddressButton.setOnClickListener(v -> {
            // Navigate to the Add Address Form Activity
            Intent intent = new Intent(ShippingAddressActivity.this, AddressFormActivity.class);
            intent.putExtra("TOKEN", token); // Pass token
            // No address object is passed for adding a new one
            intent.putExtra("IS_EDIT_MODE", false); // Indicate add mode
            addressFormLauncher.launch(intent); // Use the launcher
        });
    }

    private void fetchAddresses(String authHeader) {
        Log.d(TAG, "Fetching addresses...");
        ApiService.apiService.getUserAddresses(authHeader)
                .enqueue(new Callback<List<Address>>() {
                    @Override
                    public void onResponse(Call<List<Address>> call, Response<List<Address>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Addresses fetched successfully. Count: " + response.body().size());
                            addressList = response.body();
                            addressAdapter.setAddressList(addressList); // Update adapter data
                            updateEmptyState(); // Check if list is empty
                        } else {
                            Log.e(TAG, "Failed to fetch addresses. Code: " + response.code());
                            try {
                                Log.e(TAG, "Error Body: " + response.errorBody().string());
                            } catch (Exception e) {
                                Log.e(TAG, "Could not read error body", e);
                            }
                            Toast.makeText(ShippingAddressActivity.this, "Lỗi tải địa chỉ: " + response.code(), Toast.LENGTH_SHORT).show();
                            updateEmptyState(); // Ensure empty state is checked even on error
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Address>> call, Throwable t) {
                        Log.e(TAG, "API Error fetching addresses: " + t.getMessage(), t);
                        Toast.makeText(ShippingAddressActivity.this, "Lỗi kết nối API", Toast.LENGTH_SHORT).show();
                        updateEmptyState(); // Ensure empty state is checked on failure
                    }
                });
    }

    private void updateEmptyState() {
        if (addressList == null || addressList.isEmpty()) {
            addressesRecyclerView.setVisibility(View.GONE);
            noAddressesTextView.setVisibility(View.VISIBLE);
        } else {
            addressesRecyclerView.setVisibility(View.VISIBLE);
            noAddressesTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEditClick(Address address) {
        Log.d(TAG, "Edit clicked for address ID: " + address.getAddressId());
        // Navigate to the Edit Address Form Activity
        Intent intent = new Intent(ShippingAddressActivity.this, AddressFormActivity.class);
        intent.putExtra("TOKEN", token); // Pass token
        intent.putExtra("ADDRESS_OBJECT", address); // Pass the Address object to pre-fill the form
        intent.putExtra("IS_EDIT_MODE", true); // Indicate edit mode
        addressFormLauncher.launch(intent); // Use the launcher
    }

    @Override
    public void onDeleteClick(Address address) {
        Log.d(TAG, "Delete clicked for address ID: " + address.getAddressId());
        // Show a confirmation dialog before deleting
        new AlertDialog.Builder(this)
                .setTitle("Xóa địa chỉ")
                .setMessage("Bạn có chắc chắn muốn xóa địa chỉ này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Call delete API
                    deleteAddress(address);
                })
                .setNegativeButton("Hủy", null) // Dismiss dialog on cancel
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onSetDefaultClick(Address address) {
        Log.d(TAG, "Set Default clicked for address ID: " + address.getAddressId());
        setDefaultAddress(address);
    }


    private void deleteAddress(Address address) {
        if (address.getAddressId() == null) {
            Toast.makeText(this, "Không thể xóa địa chỉ chưa được lưu.", Toast.LENGTH_SHORT).show();
            return;
        }
        String authHeader = "Bearer " + token;
        ApiService.apiService.deleteAddress(address.getAddressId(), authHeader)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Address deleted successfully: " + address.getAddressId());
                            Toast.makeText(ShippingAddressActivity.this, "Địa chỉ đã xóa.", Toast.LENGTH_SHORT).show();
                            // Refresh the list after successful deletion
                            fetchAddresses(authHeader);
                        } else {
                            Log.e(TAG, "Failed to delete address. Code: " + response.code());
                            try {
                                Log.e(TAG, "Error Body: " + response.errorBody().string());
                            } catch (Exception e) {
                                Log.e(TAG, "Could not read error body", e);
                            }
                            String errorMessage = "Lỗi xóa địa chỉ: " + response.code();
                            if (response.code() == 403) { // Check for Forbidden status
                                errorMessage = "Không thể xóa địa chỉ mặc định.";
                            }
                            Toast.makeText(ShippingAddressActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "API Error deleting address: " + t.getMessage(), t);
                        Toast.makeText(ShippingAddressActivity.this, "Lỗi kết nối khi xóa.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setDefaultAddress(Address address) {
        if (address.getAddressId() == null) {
            Toast.makeText(this, "Không thể đặt địa chỉ chưa được lưu làm mặc định.", Toast.LENGTH_SHORT).show();
            return;
        }
        String authHeader = "Bearer " + token;
        ApiService.apiService.setDefaultAddress(address.getAddressId(), authHeader)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Address set as default successfully: " + address.getAddressId());
                            Toast.makeText(ShippingAddressActivity.this, "Địa chỉ đã được đặt làm mặc định.", Toast.LENGTH_SHORT).show();

                            defaultAddressChanged = true; // Đặt cờ thành true
                            setResult(RESULT_OK); // <--- ****** THÊM DÒNG NÀY ******
                            sendDefaultAddressChangedBroadcast();
                            fetchAddresses(authHeader);
                        } else {
                            Log.e(TAG, "Failed to set address as default. Code: " + response.code());
                            try {
                                Log.e(TAG, "Error Body: " + response.errorBody().string());
                            } catch (Exception e) {
                                Log.e(TAG, "Could not read error body", e);
                            }
                            Toast.makeText(ShippingAddressActivity.this, "Lỗi đặt địa chỉ mặc định: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "API Error setting address as default: " + t.getMessage(), t);
                        Toast.makeText(ShippingAddressActivity.this, "Lỗi kết nối khi đặt mặc định.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    @Override
    public void finish() {
        if (defaultAddressChanged) {
            setResult(RESULT_OK);
        }
        super.finish();
    }
    private void sendDefaultAddressChangedBroadcast() {
        Intent intent = new Intent(ACTION_DEFAULT_ADDRESS_CHANGED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Sent ACTION_DEFAULT_ADDRESS_CHANGED broadcast.");
    }
}