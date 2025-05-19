// src/test/java/vn/iostar/Project_Mobile/config/TestServiceConfiguration.java
package vn.iostar.Project_Mobile.config; // Hoặc package phù hợp

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import vn.iostar.Project_Mobile.service.IOrderService;
import vn.iostar.Project_Mobile.service.impl.UserServiceImpl; // Hoặc IUserService nếu bạn inject interface

@TestConfiguration
public class TestServiceConfiguration {

    @Bean
    @Primary // Quan trọng: Ưu tiên bean này hơn bean thật
    public IOrderService mockOrderService() {
        return Mockito.mock(IOrderService.class);
    }

    @Bean
    @Primary
    public UserServiceImpl mockUserServiceImpl() { // Hoặc IUserService
        return Mockito.mock(UserServiceImpl.class);
    }

    // Thêm các mock service khác nếu cần
}