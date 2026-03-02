package com.carrental.controller;

import com.carrental.model.AvailabilityResponse;
import com.carrental.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/availability")
    public ResponseEntity<?> getAvailability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        // Validation: from and to must be present
        if (from == null || to == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Both 'from' and 'to' query parameters are required (format: YYYY-MM-DD)"));
        }

        // Validation: from must be before to
        if (!from.isBefore(to)) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "'from' must be before 'to'"));
        }

        // Validation: at least 1 day
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to);
        if (days == 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Date range must be at least 1 day"));
        }

        List<AvailabilityResponse> results = availabilityService.checkAvailability(from, to);
        return ResponseEntity.ok(results);
    }
}
