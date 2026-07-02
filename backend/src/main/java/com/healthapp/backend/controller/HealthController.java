package com.healthapp.backend.controller;

import com.healthapp.backend.model.DashboardData;
import com.healthapp.backend.model.MedicalReport;
import com.healthapp.backend.model.ChatRequest;
import com.healthapp.backend.model.ChatResponse;
import com.healthapp.backend.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import com.healthapp.backend.model.AppointmentBooking;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend requests if served from different origins during dev
public class HealthController {

    @Autowired
    private ChatbotService chatbotService;

    private final Path uploadDir = Paths.get("uploads");
    
    // In-memory dashboard data that can be updated
    private DashboardData dashboardData = new DashboardData(
            86, // healthScore
            72, // heartRate
            8540, // steps
            2.1, // water (L)
            7.5, // sleep (hrs)
            "Cardiologist", // appointmentDoctor
            "Tomorrow, 11:30 AM" // appointmentTime
    );

    // List of active bookings
    private final List<AppointmentBooking> bookings = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!", e);
        }
    }

    // Get dashboard stats
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardData> getDashboard() {
        return ResponseEntity.ok(dashboardData);
    }

    // Update dashboard stats dynamically from frontend
    @PostMapping("/dashboard/update")
    public ResponseEntity<DashboardData> updateDashboard(@RequestBody DashboardData newData) {
        if (newData.getHealthScore() > 0) dashboardData.setHealthScore(newData.getHealthScore());
        if (newData.getHeartRate() > 0) dashboardData.setHeartRate(newData.getHeartRate());
        if (newData.getSteps() > 0) dashboardData.setSteps(newData.getSteps());
        if (newData.getWater() > 0) dashboardData.setWater(newData.getWater());
        if (newData.getSleep() > 0) dashboardData.setSleep(newData.getSleep());
        if (newData.getAppointmentDoctor() != null) dashboardData.setAppointmentDoctor(newData.getAppointmentDoctor());
        if (newData.getAppointmentTime() != null) dashboardData.setAppointmentTime(newData.getAppointmentTime());
        
        return ResponseEntity.ok(dashboardData);
    }

    // Book appointment and validate slots
    @PostMapping("/appointments/book")
    public ResponseEntity<?> bookAppointment(@RequestBody AppointmentBooking booking) {
        if (booking.getDoctorName() == null || booking.getBookingDate() == null || booking.getTimeSlot() == null) {
            return ResponseEntity.badRequest().body("Doctor name, booking date, and time slot are required.");
        }

        // Count existing bookings for the same doctor on the same date
        long existingBookingsCount = bookings.stream()
                .filter(b -> b.getDoctorName().equalsIgnoreCase(booking.getDoctorName()) && 
                             b.getBookingDate().equals(booking.getBookingDate()))
                .count();

        if (existingBookingsCount >= 15) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("This doctor is fully booked for this date. Maximum 15 bookings allowed.");
        }

        // Add booking
        bookings.add(booking);

        // Format time and date for dashboard update display
        try {
            String rawDate = booking.getBookingDate();
            // rawDate format: "yyyy-MM-dd", parse and convert to readable "19 Jun 2026"
            LocalDate date = LocalDate.parse(rawDate);
            String formattedDate = date.format(DateTimeFormatter.ofPattern("d MMM yyyy"));
            
            dashboardData.setAppointmentDoctor(booking.getDoctorName());
            dashboardData.setAppointmentTime(formattedDate + ", " + booking.getTimeSlot());
        } catch (Exception e) {
            // Fallback if parsing fails
            dashboardData.setAppointmentDoctor(booking.getDoctorName());
            dashboardData.setAppointmentTime(booking.getBookingDate() + ", " + booking.getTimeSlot());
        }

        return ResponseEntity.ok(booking);
    }

    // List all uploaded report files
    @GetMapping("/reports")
    public ResponseEntity<List<MedicalReport>> getReports() {
        try {
            List<MedicalReport> reports = Files.walk(this.uploadDir, 1)
                    .filter(path -> !path.equals(this.uploadDir))
                    .map(path -> {
                        try {
                            String filename = path.getFileName().toString();
                            long sizeBytes = Files.size(path);
                            String sizeFormatted = formatFileSize(sizeBytes);
                            String lastModified = new SimpleDateFormat("dd MMM yyyy, hh:mm a")
                                    .format(new Date(Files.getLastModifiedTime(path).toMillis()));
                            String fileUrl = "/api/reports/view/" + filename;
                            
                            // Extract original filename from UUID prefix
                            String displayName = filename;
                            if (filename.contains("_")) {
                                displayName = filename.substring(filename.indexOf("_") + 1);
                            }
                            
                            return new MedicalReport(filename, displayName, lastModified, sizeFormatted, fileUrl);
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(reports);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Upload a report image
    @PostMapping("/reports/upload")
    public ResponseEntity<?> uploadReport(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        // Validate content type is an image
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("Only image files (JPEG, PNG, GIF, WebP) are allowed.");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "report.png";
            }
            
            // Clean filename and prep UUID to avoid naming collisions
            String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
            String storedFilename = UUID.randomUUID().toString() + "_" + sanitizedFilename;
            
            Files.copy(file.getInputStream(), this.uploadDir.resolve(storedFilename), StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/api/reports/view/" + storedFilename;
            String sizeFormatted = formatFileSize(file.getSize());
            String uploadDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new Date());

            MedicalReport report = new MedicalReport(storedFilename, sanitizedFilename, uploadDate, sizeFormatted, fileUrl);
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to store file: " + e.getMessage());
        }
    }

    // Serve/View file
    @GetMapping("/reports/view/{filename:.+}")
    public ResponseEntity<Resource> viewFile(@PathVariable String filename) {
        try {
            Path file = uploadDir.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/octet-stream";
                try {
                    contentType = Files.probeContentType(file);
                } catch (IOException e) {
                    // fall back
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Helper method to format file sizes
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

    // Chatbot query endpoint
    @PostMapping("/chatbot/query")
    public ResponseEntity<ChatResponse> queryChatbot(@RequestBody ChatRequest request) {
        ChatResponse response = chatbotService.processQuery(request.getMessage(), request.getApiKey(), dashboardData);
        return ResponseEntity.ok(response);
    }
}
