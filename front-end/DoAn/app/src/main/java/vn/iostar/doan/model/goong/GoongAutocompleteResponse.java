package vn.iostar.doan.model.goong;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GoongAutocompleteResponse {
    @SerializedName("predictions")
    private List<Prediction> predictions;

    @SerializedName("status")
    private String status; // e.g., "OK"

    public List<Prediction> getPredictions() {
        return predictions;
    }

    public String getStatus() {
        return status;
    }

    // Inner class for predictions
    public static class Prediction {
        @SerializedName("description")
        private String description; // The full suggested address string

        @SerializedName("place_id")
        private String placeId; // ID to get more details if needed

        // Add other fields if relevant, e.g., "structured_formatting"

        public String getDescription() {
            return description;
        }

        public String getPlaceId() {
            return placeId;
        }
    }
}