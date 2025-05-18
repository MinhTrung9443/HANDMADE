package vn.iostar.doan.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import vn.iostar.doan.R;
import vn.iostar.doan.model.Address;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    private List<Address> addressList;
    private OnAddressActionListener listener;

    public interface OnAddressActionListener {
        void onEditClick(Address address);
        void onDeleteClick(Address address);
        void onSetDefaultClick(Address address);
        // Add listener for clicking the whole item if needed (e.g., for selection)
        // void onItemClick(Address address);
    }

    public AddressAdapter(List<Address> addressList, OnAddressActionListener listener) {
        this.addressList = addressList;
        this.listener = listener;
    }

    // Method to update data
    public void setAddressList(List<Address> addressList) {
        this.addressList = addressList;
        notifyDataSetChanged(); // Notify adapter that data has changed
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        Address address = addressList.get(position);
        if (address == null) return;

        holder.textViewRecipientNameAndPhone.setText(
                String.format("%s | %s", address.getFullName(), address.getPhone())
        );
        holder.textViewFullAddress.setText(address.getHouseNumber());

        // Show/hide default indicator
        holder.textViewDefaultIndicator.setVisibility(
                address.isDefaultAddress() ? View.VISIBLE : View.GONE
        );
        // Hide Set Default button if it's already the default
        holder.buttonSetDefault.setVisibility(
                address.isDefaultAddress() ? View.GONE : View.VISIBLE
        );


        // Set click listeners for buttons
        holder.buttonEditAddress.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(address);
            }
        });

        holder.buttonDeleteAddress.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(address);
            }
        });

        holder.buttonSetDefault.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetDefaultClick(address);
            }
        });

        // Optional: Listener for the whole item view
         /*
         holder.itemView.setOnClickListener(v -> {
              if (listener != null) {
                  listener.onItemClick(address);
              }
          });
         */
    }

    @Override
    public int getItemCount() {
        return addressList != null ? addressList.size() : 0;
    }

    static class AddressViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRecipientNameAndPhone;
        TextView textViewFullAddress;
        TextView textViewDefaultIndicator;
        Button buttonEditAddress;
        Button buttonDeleteAddress;
        Button buttonSetDefault;


        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRecipientNameAndPhone = itemView.findViewById(R.id.textViewRecipientNameAndPhone);
            textViewFullAddress = itemView.findViewById(R.id.textViewFullAddress);
            textViewDefaultIndicator = itemView.findViewById(R.id.textViewDefaultIndicator);
            buttonEditAddress = itemView.findViewById(R.id.buttonEditAddress);
            buttonDeleteAddress = itemView.findViewById(R.id.buttonDeleteAddress);
            buttonSetDefault = itemView.findViewById(R.id.buttonSetDefault);
        }
    }
}