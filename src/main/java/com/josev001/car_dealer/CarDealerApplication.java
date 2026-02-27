package com.josev001.car_dealer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CarDealerApplication {

    // Inicializa o Spring Boot e dispara o job configurado.
    public static void main(String[] args) {
        SpringApplication.run(CarDealerApplication.class, args);
    }

}
