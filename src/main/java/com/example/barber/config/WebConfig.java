package com.example.barber.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/*
  Konfiguracja MVC dla zasobów statycznych aplikacji.

  <p>Rejestruje mapowanie URL {@code /uploads/**} na lokalny katalog {@code uploads/},
  dzięki czemu zdjęcia wgrane przez barberów są dostępne przez przeglądarkę
  bez konieczności umieszczania ich w {@code src/main/resources/static}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /*
     Rejestruje handler zasobów dla uploadeowanych plików.
      @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadsPath = new File("uploads").getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsPath + "/");
    }
}
