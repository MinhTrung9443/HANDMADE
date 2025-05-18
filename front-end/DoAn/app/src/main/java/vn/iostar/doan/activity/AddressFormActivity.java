package vn.iostar.doan.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.iostar.doan.R;
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.api.ApiService1;
import vn.iostar.doan.api.GoongApiService; // Import Goong API Service
import vn.iostar.doan.model.Address;
import vn.iostar.doan.modelResponse.AddressInputDTO;
import vn.iostar.doan.model.goong.GoongAutocompleteResponse; // Import Goong response model
import vn.iostar.doan.utils.Constants; // Import Constants

public class AddressFormActivity extends AppCompatActivity {

    private static final String TAG = "AddressFormActivity";

    private ImageView backButton;
    private TextView toolbarTitle;
    private TextInputEditText editTextFullName, editTextPhone, editTextHouseNumber,
            editTextWard, editTextDistrict, editTextCity, editTextCountry;
    private CheckBox checkboxDefaultAddress;
    private Button buttonSaveAddress;
    private ListView listViewSuggestions; // ListView for Goong suggestions

    private String token;
    private Address currentAddress; // Null if adding, contains data if editing
    private boolean isEditMode = false;

    // Goong API and data
    private GoongApiService goongApiService;
    private ArrayAdapter<String> suggestionsAdapter;
    private List<GoongAutocompleteResponse.Prediction> currentPredictions = new ArrayList<>();
    private TextWatcher houseNumberTextWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_form);

        token = getIntent().getStringExtra("TOKEN");
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Lỗi xác thực.", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED); // Indicate failure
            finish();
            return;
        }
        String authHeader = "Bearer " + token;

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
        if (isEditMode) {
            currentAddress = (Address) getIntent().getSerializableExtra("ADDRESS_OBJECT");
            if (currentAddress == null) {
                Toast.makeText(this, "Lỗi tải dữ liệu địa chỉ.", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED); // Indicate failure
                finish();
                return;
            }
            Log.d(TAG, "Edit mode, address ID: " + currentAddress.getAddressId());
        } else {
            Log.d(TAG, "Add mode.");
        }


        AnhXa();
        setupToolbarTitle();
        populateFieldsIfEditing();
        setupListeners(authHeader);
        setupGoongAutocomplete(); // Setup Goong listener
    }

    private void AnhXa() {
        backButton = findViewById(R.id.backButton);
        toolbarTitle = findViewById(R.id.toolbarTitle);
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextHouseNumber = findViewById(R.id.editTextHouseNumber);
        editTextWard = findViewById(R.id.editTextWard);
        editTextDistrict = findViewById(R.id.editTextDistrict);
        editTextCity = findViewById(R.id.editTextCity);
        editTextCountry = findViewById(R.id.editTextCountry);
        checkboxDefaultAddress = findViewById(R.id.checkboxDefaultAddress);
        buttonSaveAddress = findViewById(R.id.buttonSaveAddress);
        listViewSuggestions = findViewById(R.id.listViewSuggestions); // Initialize ListView
    }

    private void setupToolbarTitle() {
        if (isEditMode) {
            toolbarTitle.setText("Edit Address");
            buttonSaveAddress.setText("Save Changes");
        } else {
            toolbarTitle.setText("Add New Address");
            buttonSaveAddress.setText("Save Address");
        }
    }

    private void populateFieldsIfEditing() {
        if (isEditMode && currentAddress != null) {
            editTextFullName.setText(currentAddress.getFullName());
            editTextPhone.setText(currentAddress.getPhone());
            editTextHouseNumber.setText(currentAddress.getHouseNumber());
            editTextWard.setText(currentAddress.getWard());
            editTextDistrict.setText(currentAddress.getDistrict());
            editTextCity.setText(currentAddress.getCity());
            editTextCountry.setText(currentAddress.getCountry());
            checkboxDefaultAddress.setChecked(currentAddress.isDefaultAddress());

            // Cannot unset the *only* default address. Disable checkbox if it's the default?
            // Or let the backend handle the constraint. For now, leave it enabled.
        }
    }

    private void setupListeners(String authHeader) {
        backButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED); // User cancelled
            finish();
        });

        buttonSaveAddress.setOnClickListener(v -> {
            if (validateInput()) {
                saveAddress(authHeader);
            }
        });
    }

    private boolean validateInput() {
        String fullName = editTextFullName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String houseNumber = editTextHouseNumber.getText().toString().trim();
        String ward = editTextWard.getText().toString().trim();
        String district = editTextDistrict.getText().toString().trim();
        String city = editTextCity.getText().toString().trim();
        String country = editTextCountry.getText().toString().trim();


        if (fullName.isEmpty() || phone.isEmpty() || houseNumber.isEmpty() ||
                ward.isEmpty() || district.isEmpty() || city.isEmpty() || country.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin địa chỉ.", Toast.LENGTH_SHORT).show();
            return false;
        }
        // Basic phone validation (optional)
        if (!android.util.Patterns.PHONE.matcher(phone).matches()) {
            Toast.makeText(this, "Số điện thoại không hợp lệ.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }


    private void saveAddress(String authHeader) {
        // Create the AddressInputDTO from current field values
        AddressInputDTO addressInput = new AddressInputDTO(
                editTextFullName.getText().toString().trim(),
                editTextPhone.getText().toString().trim(),
                editTextHouseNumber.getText().toString().trim(),
                editTextWard.getText().toString().trim(),
                editTextDistrict.getText().toString().trim(),
                editTextCity.getText().toString().trim(),
                editTextCountry.getText().toString().trim(),
                checkboxDefaultAddress.isChecked()
        );
        Log.d(TAG, "Token received: " + token); // Log giá trị của token gốc
        Log.d(TAG, "Authorization Header being sent: " + authHeader);

        Call<Address> call;
        if (isEditMode && currentAddress != null && currentAddress.getAddressId() != null) {
            Log.d(TAG, "Calling update API for address ID: " + currentAddress.getAddressId());
            Log.d(TAG, "Calling update API for address ID: " + addressInput.getWard());
            call = ApiService.apiService.updateAddress(currentAddress.getAddressId(), authHeader, addressInput);
        } else {
            Log.d(TAG, "Calling add API.");
            call = ApiService.apiService.addAddress(authHeader, addressInput);
        }

        call.enqueue(new Callback<Address>() {
            @Override
            public void onResponse(Call<Address> call, Response<Address> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, (isEditMode ? "Updated" : "Added") + " address successfully.");
                    Toast.makeText(AddressFormActivity.this, "Đã lưu địa chỉ.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Indicate success
                    finish(); // Close the form activity
                } else {
                    Log.e(TAG, "Failed to " + (isEditMode ? "update" : "add") + " address. Code: " + response.code());
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e(TAG, "Error Body: " + errorBody);

                        Toast.makeText(AddressFormActivity.this, "Lỗi: " + response.code() + " - " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read error body", e);
                        Toast.makeText(AddressFormActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Address> call, Throwable t) {
                Log.e(TAG, "API Error " + (isEditMode ? "updating" : "adding") + " address: " + t.getMessage(), t);
                Toast.makeText(AddressFormActivity.this, "Lỗi kết nối khi lưu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Goong Autocomplete Implementation ---

    private void setupGoongAutocomplete() {
        goongApiService = ApiService1.getGoongApiService(); // Get Goong API service instance

        suggestionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        listViewSuggestions.setAdapter(suggestionsAdapter);

        houseNumberTextWatcher = new TextWatcher() {
            private final long DELAY = 300; // Milliseconds delay
            private long lastTextChange = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Hide suggestions if text is empty
                if (s.length() == 0) {
                    listViewSuggestions.setVisibility(View.GONE);
                    currentPredictions.clear(); // Clear previous predictions
                    suggestionsAdapter.clear();
                    suggestionsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {
                lastTextChange = System.currentTimeMillis();
                if (s.length() > 2) {
                    editTextHouseNumber.postDelayed(() -> {
                        if (System.currentTimeMillis() > lastTextChange + DELAY - 50) { // Check if no new change occurred within delay
                            fetchGoongSuggestions(s.toString());
                        }
                    }, DELAY);
                }
            }
        };
        editTextHouseNumber.addTextChangedListener(houseNumberTextWatcher);
        listViewSuggestions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GoongAutocompleteResponse.Prediction selectedPrediction = currentPredictions.get(position);
                String fullAddress = selectedPrediction.getDescription();

                editTextHouseNumber.removeTextChangedListener(houseNumberTextWatcher);

                editTextHouseNumber.setText(fullAddress);
                editTextHouseNumber.setSelection(editTextHouseNumber.getText().length());
                editTextHouseNumber.addTextChangedListener(houseNumberTextWatcher);

                // Hide the suggestions list
                listViewSuggestions.setVisibility(View.GONE);
                currentPredictions.clear();
                suggestionsAdapter.clear();
                suggestionsAdapter.notifyDataSetChanged();


                String[] parts = fullAddress.split(", ");
                String ward = "";
                String district = "";
                String city = "";
                String country = "";
                if (parts.length >= 2) ward = parts[parts.length - 3].trim();
                if (parts.length >= 3) district = parts[parts.length - 2].trim();
                if (parts.length >= 4) city = parts[parts.length - 1].trim();
                country = "Vietnam";

                if (!ward.isEmpty() && !ward.equals(parts[parts.length - 2].trim())) {
                    editTextWard.setText(ward);
                } else {
                    editTextWard.setText("");
                }

                if (!district.isEmpty() && !district.equals(parts[parts.length - 1].trim())) {
                    editTextDistrict.setText(district);
                } else {
                    editTextDistrict.setText("");
                }
                if (!city.isEmpty()) {
                    editTextCity.setText(city);
                } else {
                    editTextCity.setText("");
                }
                editTextCountry.setText(country); // Assuming Vietnam

                editTextWard.requestFocus(); // Hoặc trường nào bạn muốn người dùng nhập tiếp
            }
        });
    }

    private void fetchGoongSuggestions(String input) {
        if (input.isEmpty() || Constants.GOONG_API_KEY.equals("YOUR_GOONG_API_KEY")) {
            listViewSuggestions.setVisibility(View.GONE);
            currentPredictions.clear();
            suggestionsAdapter.clear();
            suggestionsAdapter.notifyDataSetChanged();
            if (Constants.GOONG_API_KEY.equals("YOUR_GOONG_API_KEY")) {
                Log.e(TAG, "Goong API Key not set!");
            }
            return;
        }

        goongApiService.autocomplete(Constants.GOONG_API_KEY, input, 10) // Limit to 10 suggestions
                .enqueue(new Callback<GoongAutocompleteResponse>() {
                    @Override
                    public void onResponse(Call<GoongAutocompleteResponse> call, Response<GoongAutocompleteResponse> response) {
                        if (response.isSuccessful() && response.body() != null && "OK".equals(response.body().getStatus())) {
                            currentPredictions = response.body().getPredictions();
                            List<String> descriptions = new ArrayList<>();
                            if (currentPredictions != null) {
                                for (GoongAutocompleteResponse.Prediction prediction : currentPredictions) {
                                    descriptions.add(prediction.getDescription());
                                }
                            }
                            suggestionsAdapter.clear();
                            suggestionsAdapter.addAll(descriptions);
                            suggestionsAdapter.notifyDataSetChanged();
                            listViewSuggestions.setVisibility(descriptions.isEmpty() ? View.GONE : View.VISIBLE);
                            Log.d(TAG, "Fetched " + descriptions.size() + " Goong suggestions.");
                        } else {
                            Log.e(TAG, "Goong Autocomplete API error. Status: " + (response.body() != null ? response.body().getStatus() : "N/A") + ", Code: " + response.code());
                            // Optionally show an error, but perhaps quietly fail for suggestions
                            listViewSuggestions.setVisibility(View.GONE); // Hide suggestions on error
                        }
                    }

                    @Override
                    public void onFailure(Call<GoongAutocompleteResponse> call, Throwable t) {
                        Log.e(TAG, "Goong Autocomplete API connection error: " + t.getMessage(), t);
                        // Optionally show an error, but perhaps quietly fail for suggestions
                        listViewSuggestions.setVisibility(View.GONE); // Hide suggestions on error
                    }
                });
    }
}