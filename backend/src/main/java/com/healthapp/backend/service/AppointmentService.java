package com.healthapp.backend.service;

import com.healthapp.backend.entity.Appointment;
import com.healthapp.backend.entity.User;
import com.healthapp.backend.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public List<Appointment> getDoctorTodayAppointments(Long doctorId) {
        String today = LocalDate.now().toString();
        return appointmentRepository.findByDoctorIdAndBookingDate(doctorId, today);
    }

    public Appointment bookAppointment(User patient, User doctor, String date, String timeSlot, String symptoms) throws Exception {
        // Enforce maximum 15 bookings per doctor per day rule
        long existingBookings = appointmentRepository.countByDoctorIdAndBookingDate(doctor.getId(), date);
        if (existingBookings >= 15) {
            throw new Exception("This doctor is fully booked for the selected date. Maximum 15 bookings allowed.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setBookingDate(date);
        appointment.setTimeSlot(timeSlot);
        appointment.setSymptoms(symptoms);
        appointment.setStatus("Pending");

        return appointmentRepository.save(appointment);
    }

    public void approveAppointment(Long appointmentId) throws Exception {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new Exception("Appointment not found");
        }
        Appointment appointment = appointmentOpt.get();
        appointment.setStatus("Approved");
        appointmentRepository.save(appointment);
    }

    public void cancelAppointment(Long appointmentId) throws Exception {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new Exception("Appointment not found");
        }
        Appointment appointment = appointmentOpt.get();
        appointment.setStatus("Cancelled");
        appointmentRepository.save(appointment);
    }
}
