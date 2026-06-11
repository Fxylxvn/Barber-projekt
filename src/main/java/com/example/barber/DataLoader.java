package com.example.barber;

import com.example.barber.model.User;
import com.example.barber.repo.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(UserRepo repo, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return args -> {
            if (repo.count() == 0) {
                // Test Client Account
                User clientTest = new User();
                clientTest.setUsername("login@klient.pl");
                clientTest.setPassword(passwordEncoder.encode("test1234"));
                clientTest.setRole("KLIENT");
                clientTest.setName("Tomek Klient (Test)");
                clientTest.setEmail("login@klient.pl");
                repo.save(clientTest);

                // Test Barber Account
                User barberTest = new User();
                barberTest.setUsername("login@barber.pl");
                barberTest.setPassword(passwordEncoder.encode("test1234"));
                barberTest.setRole("BARBER");
                barberTest.setName("Sebastian Master (Test)");
                barberTest.setEmail("login@barber.pl");
                barberTest.setWorkStartHour(9);
                barberTest.setWorkEndHour(18);
                barberTest.setWorkDays("1,2,3,4,5");
                barberTest.setTitle("Senior Master Barber");
                barberTest.setRating(4.9);
                barberTest.setBio("Założyciel salonu, legenda lokalnej sceny barberskiej. Ekspert w strzyżeniu brzytwą i rekonstrukcji męskiej brody.");
                barberTest.setPhotoUrl("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300");
                repo.save(barberTest);

                // Other Barbers
                User b1 = new User();
                b1.setUsername("barber1");
                b1.setPassword(passwordEncoder.encode("pass"));
                b1.setRole("BARBER");
                b1.setName("Janusz");
                b1.setWorkStartHour(8);
                b1.setWorkEndHour(16);
                b1.setWorkDays("1,2,3,4,5");
                b1.setTitle("Master Barber");
                b1.setRating(4.8);
                b1.setBio("Mistrz klasyki i tradycyjnego strzyżenia. Z dbałością o każdy detal Twojego wizerunku.");
                b1.setPhotoUrl("https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=300");
                repo.save(b1);

                User b2 = new User();
                b2.setUsername("barber2");
                b2.setPassword(passwordEncoder.encode("pass"));
                b2.setRole("BARBER");
                b2.setName("Andrzej");
                b2.setWorkStartHour(10);
                b2.setWorkEndHour(18);
                b2.setWorkDays("1,2,3,4,5,6");
                b2.setTitle("Top Barber");
                b2.setRating(4.9);
                b2.setBio("Specjalista od nowoczesnych, mocnych fade'ów i perfekcyjnej stylizacji brody.");
                b2.setPhotoUrl("https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=300");
                repo.save(b2);

                User b3 = new User();
                b3.setUsername("barber3");
                b3.setPassword(passwordEncoder.encode("pass"));
                b3.setRole("BARBER");
                b3.setName("Krzysztof");
                b3.setWorkStartHour(9);
                b3.setWorkEndHour(17);
                b3.setWorkDays("2,3,4,5,6");
                b3.setTitle("Barber Stylist");
                b3.setRating(4.7);
                b3.setBio("Pasjonat nowoczesnych trendów i stylizacji. Stworzy cięcie idealnie dopasowane do Twojej twarzy.");
                b3.setPhotoUrl("https://images.unsplash.com/photo-1512485694743-9c9538b4e6e0?w=300");
                repo.save(b3);

                User b4 = new User();
                b4.setUsername("barber4");
                b4.setPassword(passwordEncoder.encode("pass"));
                b4.setRole("BARBER");
                b4.setName("Wojciech");
                b4.setWorkStartHour(12);
                b4.setWorkEndHour(20);
                b4.setWorkDays("1,2,3,4,5,6,7");
                b4.setTitle("Junior Barber");
                b4.setRating(4.5);
                b4.setBio("Młody, ambitny i pełen zapału. Wnosi świeżą energię i nowoczesne spojrzenie na fryzury męskie.");
                b4.setPhotoUrl("https://images.unsplash.com/photo-1605462863863-10d9e47e15ee?w=300");
                repo.save(b4);

                // Clients
                User k1 = new User();
                k1.setUsername("klient1");
                k1.setPassword(passwordEncoder.encode("pass"));
                k1.setRole("KLIENT");
                k1.setName("Mirek");
                repo.save(k1);

                User k2 = new User();
                k2.setUsername("klient2");
                k2.setPassword(passwordEncoder.encode("pass"));
                k2.setRole("KLIENT");
                k2.setName("Zbyszek");
                repo.save(k2);

                User k3 = new User();
                k3.setUsername("klient3");
                k3.setPassword(passwordEncoder.encode("pass"));
                k3.setRole("KLIENT");
                k3.setName("Adam");
                repo.save(k3);
            }
        };
    }
}
