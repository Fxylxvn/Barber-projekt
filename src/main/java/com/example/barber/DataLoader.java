package com.example.barber;

import com.example.barber.model.User;
import com.example.barber.repo.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(UserRepo repo) {
        return args -> {
            if (repo.count() == 0) {
                User b1 = new User();
                b1.setUsername("barber1");
                b1.setPassword("pass");
                b1.setRole("BARBER");
                b1.setName("Janusz");
                b1.setWorkStartHour(8);
                b1.setWorkEndHour(16);
                b1.setWorkDays("1,2,3,4,5");
                repo.save(b1);

                User k1 = new User();
                k1.setUsername("klient1");
                k1.setPassword("pass");
                k1.setRole("KLIENT");
                k1.setName("Mirek");
                repo.save(k1);

                User b2 = new User();
                b2.setUsername("barber2");
                b2.setPassword("pass");
                b2.setRole("BARBER");
                b2.setName("Andrzej");
                b2.setWorkStartHour(10);
                b2.setWorkEndHour(18);
                b2.setWorkDays("1,2,3,4,5,6");
                repo.save(b2);

                User k2 = new User();
                k2.setUsername("klient2");
                k2.setPassword("pass");
                k2.setRole("KLIENT");
                k2.setName("Zbyszek");
                repo.save(k2);

                User b3 = new User();
                b3.setUsername("barber3");
                b3.setPassword("pass");
                b3.setRole("BARBER");
                b3.setName("Krzysztof");
                b3.setWorkStartHour(9);
                b3.setWorkEndHour(17);
                b3.setWorkDays("2,3,4,5,6");
                repo.save(b3);

                User b4 = new User();
                b4.setUsername("barber4");
                b4.setPassword("pass");
                b4.setRole("BARBER");
                b4.setName("Wojciech");
                b4.setWorkStartHour(12);
                b4.setWorkEndHour(20);
                b4.setWorkDays("1,2,3,4,5,6,7");
                repo.save(b4);

                User k3 = new User();
                k3.setUsername("klient3");
                k3.setPassword("pass");
                k3.setRole("KLIENT");
                k3.setName("Adam");
                repo.save(k3);

                User k4 = new User();
                k4.setUsername("klient4");
                k4.setPassword("pass");
                k4.setRole("KLIENT");
                k4.setName("Piotr");
                repo.save(k4);

                User k5 = new User();
                k5.setUsername("klient5");
                k5.setPassword("pass");
                k5.setRole("KLIENT");
                k5.setName("Tomasz");
                repo.save(k5);
            }
        };
    }
}
