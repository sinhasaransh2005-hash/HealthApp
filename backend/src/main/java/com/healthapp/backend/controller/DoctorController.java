package com.healthapp.backend.controller;

import com.healthapp.backend.entity.Appointment;
import com.healthapp.backend.entity.Prescription;
import com.healthapp.backend.entity.User;
import com.healthapp.backend.security.CustomUserDetails;
import com.healthapp.backend.service.AppointmentService;
import com.healthapp.backend.service.PrescriptionService;
import com.healthapp.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PrescriptionService prescriptionService;

    @GetMapping("/dashboard")
    public String showDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User doctor = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctor.getId());
        List<Appointment> todayAppointments = appointmentService.getDoctorTodayAppointments(doctor.getId());
        List<Prescription> prescriptions = prescriptionService.getDoctorPrescriptions(doctor.getId());
        List<User> patients = userService.findAllPatients();

        model.addAttribute("doctor", doctor);
        model.addAttribute("appointments", appointments);
        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("patients", patients);

        if (!model.containsAttribute("activeTab")) {
            model.addAttribute("activeTab", "home");
        }

        return "doctor/dashboard";
    }

    @PostMapping("/appointment/approve")
    public String approveAppointment(@RequestParam("appointmentId") Long appointmentId,
                                     RedirectAttributes redirectAttributes) {
        try {
            appointmentService.approveAppointment(appointmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment approved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        redirectAttributes.addFlashAttribute("activeTab", "requests");
        return "redirect:/doctor/dashboard";
    }

    @PostMapping("/appointment/cancel")
    public String cancelAppointment(@RequestParam("appointmentId") Long appointmentId,
                                    RedirectAttributes redirectAttributes) {
        try {
            appointmentService.cancelAppointment(appointmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment cancelled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        redirectAttributes.addFlashAttribute("activeTab", "requests");
        return "redirect:/doctor/dashboard";
    }

    @PostMapping("/prescription/create")
    public String createPrescription(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam("patientId") Long patientId,
                                     @RequestParam("medicineName") String medicineName,
                                     @RequestParam("dosage") String dosage,
                                     @RequestParam("instructions") String instructions,
                                     RedirectAttributes redirectAttributes) {
        
        User doctor = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        User patient = userService.findAllPatients().stream()
                .filter(p -> p.getId().equals(patientId))
                .findFirst()
                .orElse(null);

        if (patient == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Patient not found.");
            redirectAttributes.addFlashAttribute("activeTab", "prescriptions");
            return "redirect:/doctor/dashboard";
        }

        try {
            prescriptionService.createPrescription(patient, doctor, medicineName, dosage, instructions);
            redirectAttributes.addFlashAttribute("successMessage", "Prescription issued successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to issue prescription.");
        }

        redirectAttributes.addFlashAttribute("activeTab", "prescriptions");
        return "redirect:/doctor/dashboard";
    }
}
