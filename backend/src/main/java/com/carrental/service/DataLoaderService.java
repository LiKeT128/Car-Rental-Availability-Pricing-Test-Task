package com.carrental.service;

import com.carrental.model.Car;
import com.carrental.model.Reservation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(DataLoaderService.class);

    @Value("${data.directory:../data}")
    private String dataDirectory;

    private List<Car> cars = new ArrayList<>();
    private List<Reservation> reservations = new ArrayList<>();

    @PostConstruct
    public void loadData() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        loadCars(mapper);
        loadReservations(mapper);
    }

    private void loadCars(ObjectMapper mapper) {
        try {
            File carsFile = new File(dataDirectory, "cars.json");
            logger.info("Loading cars from: {}", carsFile.getAbsolutePath());

            JsonNode root = mapper.readTree(carsFile);
            JsonNode carsNode = root.get("cars");

            if (carsNode != null && carsNode.isArray()) {
                for (JsonNode carNode : carsNode) {
                    try {
                        Car car = new Car();
                        car.setCarId(carNode.has("carId") ? carNode.get("carId").asText() : null);
                        car.setModel(carNode.has("model") ? carNode.get("model").asText() : null);
                        car.setCarClass(carNode.has("class") ? carNode.get("class").asText() : null);

                        if (carNode.has("pricePerDay") && !carNode.get("pricePerDay").isNull()) {
                            car.setPricePerDay(carNode.get("pricePerDay").asDouble());
                        }

                        cars.add(car);
                        logger.info("Loaded car: {} ({})", car.getCarId(), car.getModel());
                    } catch (Exception e) {
                        logger.warn("Failed to parse car entry: {}", e.getMessage());
                    }
                }
            }

            logger.info("Total cars loaded: {}", cars.size());
        } catch (IOException e) {
            logger.error("Failed to load cars.json: {}", e.getMessage());
        }
    }

    private void loadReservations(ObjectMapper mapper) {
        try {
            File reservationsFile = new File(dataDirectory, "reservations.json");
            logger.info("Loading reservations from: {}", reservationsFile.getAbsolutePath());

            JsonNode root = mapper.readTree(reservationsFile);
            JsonNode reservationsNode = root.get("reservations");

            if (reservationsNode != null && reservationsNode.isArray()) {
                for (JsonNode resNode : reservationsNode) {
                    try {
                        Reservation reservation = mapper.treeToValue(resNode, Reservation.class);
                        reservations.add(reservation);
                        logger.info("Loaded reservation: {} for car {}", reservation.getReservationId(),
                                reservation.getCarId());
                    } catch (Exception e) {
                        logger.warn("Failed to parse reservation entry: {}", e.getMessage());
                    }
                }
            }

            logger.info("Total reservations loaded: {}", reservations.size());
        } catch (IOException e) {
            logger.error("Failed to load reservations.json: {}", e.getMessage());
        }
    }

    public List<Car> getCars() {
        return cars;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }
}
