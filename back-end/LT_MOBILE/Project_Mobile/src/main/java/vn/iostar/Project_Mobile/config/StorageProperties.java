package vn.iostar.Project_Mobile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("storage") 
public class StorageProperties {

    private String location = "uploads"; 

    // Getter
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
}