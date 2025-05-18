package vn.iostar.Project_Mobile.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "Address")
@ToString
public class Address {
	 @Id
	 @GeneratedValue(strategy = GenerationType.IDENTITY)
	 private long addressId;
	 
	 	@Column(nullable = true) 
	    private String recipientName; 

	    @Column(nullable = true) 
	    private String recipientPhone; 

	    @Column(nullable = true) 
	    private String streetAddress;

	    @Column(nullable = true) 
	    private String ward; 

	    @Column(nullable = false)
	    private String district;

	    @Column(nullable = false) 
	    private String city;    

	    @Column(nullable = true) 
	    private String country;

	    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	    private boolean isDefault = false; 

	    @ManyToOne(fetch = FetchType.LAZY) 
	    @JoinColumn(name = "user_id", nullable = false)
	    @JsonBackReference 
	    @ToString.Exclude 
	    private User user;

	    @Column(nullable = true)
	    private String goongPlaceId;


}
