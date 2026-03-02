package com.carrental.service;

import com.carrental.model.AvailabilityResponse;
import com.carrental.model.Car;
import com.carrental.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(AvailabilityService.class);

    private final DataLoaderService dataLoaderService;

    public AvailabilityService(DataLoaderService dataLoaderService) {
        this.dataLoaderService = dataLoaderService;
    }

    /**
     * Checks whether a reservation overlaps with the query interval [queryFrom,
     * queryTo).
     * Overlap condition: reservation.from < queryTo AND reservation.to > queryFrom
     */
    public static boolean isOverlapping(LocalDate resFrom, LocalDate resTo, LocalDate queryFrom, LocalDate queryTo) {
        if (resFrom == null || resTo == null) {
            return false; // Can't determine overlap without valid dates
        }
        return resFrom.isBefore(queryTo) && resTo.isAfter(queryFrom);
    }

    /**
     * Checks if a reservation has invalid/dirty date data.
     * A reservation has a DATA conflict if:
     * - from or to is missing (null)
     * - to <= from (invalid range)
     */
    public static boolean hasDataIssue(Reservation reservation) {
        if (reservation.getFromDate() == null || reservation.getToDate() == null) {
            return true;
        }
        return !reservation.getToDate().isAfter(reservation.getFromDate()); // to <= from
    }

    /**
     * Determines if a status indicates a hard-blocking reservation.
     */
    public static boolean isBlockingStatus(String status) {
        return "CONFIRMED".equals(status) || "PICKED_UP".equals(status);
    }

    /**
     * Determines if a status indicates a cancelled reservation (to be ignored).
     */
    public static boolean isCancelledStatus(String status) {
        return "CANCELLED".equals(status);
    }

    /**
     * Computes availability for all cars over the given [from, to) interval.
     */
    public List<AvailabilityResponse> checkAvailability(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to);
        List<Car> cars = dataLoaderService.getCars();
        List<Reservation> allReservations = dataLoaderService.getReservations();

        List<AvailabilityResponse> results = new ArrayList<>();

        for (Car car : cars) {
            List<Reservation> carReservations = allReservations.stream()
                    .filter(r -> car.getCarId().equals(r.getCarId()))
                    .toList();

            boolean isAvailable = true;
            int conflicts = 0;

            for (Reservation reservation : carReservations) {
                // Skip CANCELLED reservations entirely
                if (isCancelledStatus(reservation.getStatus())) {
                    continue;
                }

                // Check for DATA conflict (invalid or missing dates)
                if (hasDataIssue(reservation)) {
                    conflicts++;
                    logger.warn("DATA conflict for reservation {} on car {}: invalid/missing dates",
                            reservation.getReservationId(), car.getCarId());
                    continue; // DATA conflicts don't block availability
                }

                // Check temporal overlap
                boolean overlaps = isOverlapping(
                        reservation.getFromDate(),
                        reservation.getToDate(),
                        from, to);

                if (overlaps) {
                    if (isBlockingStatus(reservation.getStatus())) {
                        // HARD conflict
                        conflicts++;
                        isAvailable = false;
                        logger.info("HARD conflict: reservation {} ({}) overlaps query [{}, {}) for car {}",
                                reservation.getReservationId(), reservation.getStatus(), from, to, car.getCarId());
                    } else {
                        // SOFT conflict — status is UNKNOWN or missing
                        conflicts++;
                        logger.info("SOFT conflict: reservation {} (status={}) overlaps query [{}, {}) for car {}",
                                reservation.getReservationId(), reservation.getStatus(), from, to, car.getCarId());
                    }
                }
            }

            // Calculate estimated price
            Double estimatedPrice = null;
            if (isAvailable) {
                if (car.getPricePerDay() != null && car.getPricePerDay() > 0) {
                    estimatedPrice = days * car.getPricePerDay();
                } else {
                    logger.warn("Car {} ({}) has invalid or missing pricePerDay: {}",
                            car.getCarId(), car.getModel(), car.getPricePerDay());
                }
            }

            results.add(new AvailabilityResponse(
                    car.getCarId(),
                    car.getModel(),
                    isAvailable,
                    conflicts,
                    estimatedPrice));
        }

        return results;
    }
}
