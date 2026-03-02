package com.carrental.service;

import com.carrental.model.AvailabilityResponse;
import com.carrental.model.Car;
import com.carrental.model.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private DataLoaderService dataLoaderService;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityService(dataLoaderService);
    }

    // ========== Overlap Detection Tests ==========

    @Test
    @DisplayName("Overlap: reservation fully inside query range → overlaps")
    void overlap_reservationInsideRange() {
        assertTrue(AvailabilityService.isOverlapping(
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5)));
    }

    @Test
    @DisplayName("Overlap: reservation ends exactly at query start → no overlap (half-open interval)")
    void overlap_reservationEndsAtQueryStart() {
        assertFalse(AvailabilityService.isOverlapping(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 3),
                LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 5)));
    }

    @Test
    @DisplayName("Overlap: reservation starts exactly at query end → no overlap")
    void overlap_reservationStartsAtQueryEnd() {
        assertFalse(AvailabilityService.isOverlapping(
                LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 7),
                LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 5)));
    }

    @Test
    @DisplayName("Overlap: partial overlap at start → overlaps")
    void overlap_partialOverlapStart() {
        assertTrue(AvailabilityService.isOverlapping(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 5)));
    }

    @Test
    @DisplayName("Overlap: partial overlap at end → overlaps")
    void overlap_partialOverlapEnd() {
        assertTrue(AvailabilityService.isOverlapping(
                LocalDate.of(2026, 3, 4), LocalDate.of(2026, 3, 7),
                LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 5)));
    }

    @Test
    @DisplayName("Overlap: null dates → no overlap")
    void overlap_nullDates() {
        assertFalse(AvailabilityService.isOverlapping(
                null, LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 5)));
    }

    // ========== Data Issue Detection Tests ==========

    @Test
    @DisplayName("Data issue: missing from date")
    void dataIssue_missingFromDate() {
        Reservation res = new Reservation("R-1", "C-1", null, LocalDate.of(2026, 3, 5), "CONFIRMED");
        assertTrue(AvailabilityService.hasDataIssue(res));
    }

    @Test
    @DisplayName("Data issue: to <= from (invalid range)")
    void dataIssue_invalidRange() {
        Reservation res = new Reservation("R-1", "C-1",
                LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 6), "CONFIRMED");
        assertTrue(AvailabilityService.hasDataIssue(res));
    }

    @Test
    @DisplayName("Data issue: valid dates → no issue")
    void dataIssue_validDates() {
        Reservation res = new Reservation("R-1", "C-1",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), "CONFIRMED");
        assertFalse(AvailabilityService.hasDataIssue(res));
    }

    // ========== Conflict Counting Tests ==========

    @Test
    @DisplayName("Conflicts: HARD + SOFT + DATA conflicts counted correctly")
    void conflicts_mixedTypes() {
        Car car = new Car();
        car.setCarId("C-1");
        car.setModel("Test Car");
        car.setPricePerDay(50.0);

        List<Reservation> reservations = List.of(
                // HARD conflict: overlaps + CONFIRMED
                new Reservation("R-1", "C-1",
                        LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4), "CONFIRMED"),
                // SOFT conflict: overlaps + UNKNOWN
                new Reservation("R-2", "C-1",
                        LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 6), "UNKNOWN"),
                // DATA conflict: invalid dates (to <= from)
                new Reservation("R-3", "C-1",
                        LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 8), "CONFIRMED"),
                // CANCELLED: should be ignored
                new Reservation("R-4", "C-1",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), "CANCELLED"));

        when(dataLoaderService.getCars()).thenReturn(List.of(car));
        when(dataLoaderService.getReservations()).thenReturn(reservations);

        List<AvailabilityResponse> results = availabilityService.checkAvailability(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5));

        assertEquals(1, results.size());
        AvailabilityResponse result = results.get(0);
        assertEquals("C-1", result.getCarId());
        assertFalse(result.isAvailable()); // HARD conflict blocks availability
        assertEquals(3, result.getConflicts()); // 1 HARD + 1 SOFT + 1 DATA
        assertNull(result.getEstimatedPrice()); // unavailable → null price
    }

    @Test
    @DisplayName("Pricing: available car with valid pricePerDay gets correct price")
    void pricing_availableCarWithValidPrice() {
        Car car = new Car();
        car.setCarId("C-1");
        car.setModel("Test Car");
        car.setPricePerDay(40.0);

        when(dataLoaderService.getCars()).thenReturn(List.of(car));
        when(dataLoaderService.getReservations()).thenReturn(List.of());

        List<AvailabilityResponse> results = availabilityService.checkAvailability(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 4) // 3 days
        );

        assertEquals(1, results.size());
        assertTrue(results.get(0).isAvailable());
        assertEquals(120.0, results.get(0).getEstimatedPrice());
    }

    @Test
    @DisplayName("Pricing: available car with pricePerDay=0 → estimatedPrice null")
    void pricing_zeroPricePerDay() {
        Car car = new Car();
        car.setCarId("C-1");
        car.setModel("Test Car");
        car.setPricePerDay(0.0);

        when(dataLoaderService.getCars()).thenReturn(List.of(car));
        when(dataLoaderService.getReservations()).thenReturn(List.of());

        List<AvailabilityResponse> results = availabilityService.checkAvailability(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 4));

        assertNull(results.get(0).getEstimatedPrice());
    }

    @Test
    @DisplayName("Pricing: available car with missing pricePerDay → estimatedPrice null")
    void pricing_missingPricePerDay() {
        Car car = new Car();
        car.setCarId("C-1");
        car.setModel("Test Car");
        // pricePerDay is null

        when(dataLoaderService.getCars()).thenReturn(List.of(car));
        when(dataLoaderService.getReservations()).thenReturn(List.of());

        List<AvailabilityResponse> results = availabilityService.checkAvailability(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 4));

        assertNull(results.get(0).getEstimatedPrice());
    }

    // ========== Public Scenario S1 ==========

    @Test
    @DisplayName("Scenario S1: Early March – overlap + soft conflicts")
    void scenario_S1() {
        setupActualData();

        List<AvailabilityResponse> results = availabilityService.checkAvailability(
                LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 5));

        assertEquals(5, results.size());

        // C-100: HARD (R-001 CONFIRMED overlaps) + SOFT (R-002 UNKNOWN overlaps) = 2
        // conflicts, unavailable
        AvailabilityResponse c100 = findByCarId(results, "C-100");
        assertFalse(c100.isAvailable());
        assertEquals(2, c100.getConflicts());
        assertNull(c100.getEstimatedPrice());

        // C-200: HARD (R-011 CONFIRMED overlaps) = 1 conflict, unavailable
        AvailabilityResponse c200 = findByCarId(results, "C-200");
        assertFalse(c200.isAvailable());
        assertEquals(1, c200.getConflicts());
        assertNull(c200.getEstimatedPrice());

        // C-300: DATA (R-020 invalid dates) + DATA (R-022 missing from) = 2 conflicts,
        // available
        AvailabilityResponse c300 = findByCarId(results, "C-300");
        assertTrue(c300.isAvailable());
        assertEquals(2, c300.getConflicts());
        assertEquals(158.0, c300.getEstimatedPrice()); // 2 days * 79

        // C-400: no overlaps, available, but pricePerDay=0 → null price
        AvailabilityResponse c400 = findByCarId(results, "C-400");
        assertTrue(c400.isAvailable());
        assertEquals(0, c400.getConflicts());
        assertNull(c400.getEstimatedPrice());

        // C-500: no reservations, available, missing pricePerDay → null price
        AvailabilityResponse c500 = findByCarId(results, "C-500");
        assertTrue(c500.isAvailable());
        assertEquals(0, c500.getConflicts());
        assertNull(c500.getEstimatedPrice());
    }

    // ========== Public Scenario S2 ==========

    @Test
    @DisplayName("Scenario S2: Boundary check – reservation ends at from is NOT overlap")
    void scenario_S2() {
        setupActualData();

        List<AvailabilityResponse> results = availabilityService.checkAvailability(
                LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 9));

        // C-100: no overlaps (R-003 is CANCELLED, R-001 ends 3/5, R-002 ends 3/6)
        AvailabilityResponse c100 = findByCarId(results, "C-100");
        assertTrue(c100.isAvailable());
        assertEquals(0, c100.getConflicts());
        assertEquals(78.0, c100.getEstimatedPrice()); // 2 * 39

        // C-200: R-011 ends 3/7, query starts 3/7 → NOT overlap (half-open interval)
        AvailabilityResponse c200 = findByCarId(results, "C-200");
        assertTrue(c200.isAvailable());
        assertEquals(0, c200.getConflicts());
        assertEquals(90.0, c200.getEstimatedPrice()); // 2 * 45

        // C-300: DATA conflicts persist
        AvailabilityResponse c300 = findByCarId(results, "C-300");
        assertTrue(c300.isAvailable());
        assertEquals(2, c300.getConflicts());
        assertEquals(158.0, c300.getEstimatedPrice()); // 2 * 79
    }

    // ========== Public Scenario S3 ==========

    @Test
    @DisplayName("Scenario S3: Mid March – Tesla reserved but invalid price still null")
    void scenario_S3() {
        setupActualData();

        List<AvailabilityResponse> results = availabilityService.checkAvailability(
                LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 18));

        // C-100: no overlaps
        AvailabilityResponse c100 = findByCarId(results, "C-100");
        assertTrue(c100.isAvailable());
        assertEquals(0, c100.getConflicts());
        assertEquals(78.0, c100.getEstimatedPrice()); // 2 * 39

        // C-400: R-030 CONFIRMED overlaps → unavailable, 1 conflict
        AvailabilityResponse c400 = findByCarId(results, "C-400");
        assertFalse(c400.isAvailable());
        assertEquals(1, c400.getConflicts());
        assertNull(c400.getEstimatedPrice());

        // C-300: still 2 DATA conflicts
        AvailabilityResponse c300 = findByCarId(results, "C-300");
        assertTrue(c300.isAvailable());
        assertEquals(2, c300.getConflicts());
        assertEquals(158.0, c300.getEstimatedPrice());
    }

    // ========== Helpers ==========

    private void setupActualData() {
        Car c100 = new Car();
        c100.setCarId("C-100");
        c100.setModel("Skoda Octavia 1.6 TDI");
        c100.setPricePerDay(39.0);
        Car c200 = new Car();
        c200.setCarId("C-200");
        c200.setModel("Toyota Corolla Hybrid");
        c200.setPricePerDay(45.0);
        Car c300 = new Car();
        c300.setCarId("C-300");
        c300.setModel("VW Transporter T6");
        c300.setPricePerDay(79.0);
        Car c400 = new Car();
        c400.setCarId("C-400");
        c400.setModel("Tesla Model 3");
        c400.setPricePerDay(0.0);
        Car c500 = new Car();
        c500.setCarId("C-500");
        c500.setModel("Kia Ceed SW"); // no pricePerDay

        when(dataLoaderService.getCars()).thenReturn(List.of(c100, c200, c300, c400, c500));

        List<Reservation> reservations = List.of(
                new Reservation("R-001", "C-100", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), "CONFIRMED"),
                new Reservation("R-002", "C-100", LocalDate.of(2026, 3, 4), LocalDate.of(2026, 3, 6), "UNKNOWN"),
                new Reservation("R-003", "C-100", LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12), "CANCELLED"),
                new Reservation("R-010", "C-200", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 3), "PICKED_UP"),
                new Reservation("R-011", "C-200", LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 7), "CONFIRMED"),
                new Reservation("R-020", "C-300", LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 6), "CONFIRMED"), // DATA:
                                                                                                                    // to
                                                                                                                    // <
                                                                                                                    // from
                new Reservation("R-021", "C-300", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2), null), // missing
                                                                                                             // status
                new Reservation("R-022", "C-300", null, LocalDate.of(2026, 3, 4), "CONFIRMED"), // DATA: missing from
                new Reservation("R-030", "C-400", LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 18), "CONFIRMED"));

        when(dataLoaderService.getReservations()).thenReturn(reservations);
    }

    private AvailabilityResponse findByCarId(List<AvailabilityResponse> results, String carId) {
        return results.stream()
                .filter(r -> carId.equals(r.getCarId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Car not found: " + carId));
    }
}
