package com.example.barber_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*") // Zezwala na połączenia ze wszystkich domen (np. z frontendu React)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // Dozwolone metody HTTP
                .allowedHeaders("*");
    }
}
