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
@RequestMapping("/patient")
public class PatientController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PrescriptionService prescriptionService;

    @GetMapping("/dashboard")
    public String showDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User patient = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        List<Appointment> appointments = appointmentService.getPatientAppointments(patient.getId());
        List<Prescription> prescriptions = prescriptionService.getPatientPrescriptions(patient.getId());
        List<User> doctors = userService.findAllDoctors();

        model.addAttribute("patient", patient);
        model.addAttribute("appointments", appointments);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("doctors", doctors);
        
        // Tab routing support
        if (!model.containsAttribute("activeTab")) {
            model.addAttribute("activeTab", "home");
        }

        return "patient/dashboard";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam("fullname") String fullname,
                                @RequestParam("age") Integer age,
                                @RequestParam("gender") String gender,
                                @RequestParam("phone") String phone,
                                @RequestParam("gmail") String gmail,
                                @RequestParam("address") String address,
                                RedirectAttributes redirectAttributes) {
        
        User patient = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        patient.setFullname(fullname);
        patient.setAge(age);
        patient.setGender(gender);
        patient.setPhone(phone);
        patient.setGmail(gmail);
        patient.setAddress(address);

        userService.updateUser(patient);
        
        // Update userDetails references
        userDetails.getUser().setFullname(fullname);
        userDetails.getUser().setAge(age);
        userDetails.getUser().setGender(gender);
        userDetails.getUser().setPhone(phone);
        userDetails.getUser().setGmail(gmail);
        userDetails.getUser().setAddress(address);

        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        redirectAttributes.addFlashAttribute("activeTab", "profile");
        return "redirect:/patient/dashboard";
    }

    @PostMapping("/appointment/book")
    public String bookAppointment(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam("doctorId") Long doctorId,
                                  @RequestParam("bookingDate") String bookingDate,
                                  @RequestParam("timeSlot") String timeSlot,
                                  @RequestParam("symptoms") String symptoms,
                                  RedirectAttributes redirectAttributes) {
        
        User patient = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        User doctor = userService.findAllDoctors().stream()
                .filter(d -> d.getId().equals(doctorId))
                .findFirst()
                .orElse(null);

        if (doctor == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected doctor not found.");
            redirectAttributes.addFlashAttribute("activeTab", "book");
            return "redirect:/patient/dashboard";
        }

        try {
            appointmentService.bookAppointment(patient, doctor, bookingDate, timeSlot, symptoms);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment booked successfully!");
            redirectAttributes.addFlashAttribute("activeTab", "appointments");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "book");
        }

        return "redirect:/patient/dashboard";
    }
}
