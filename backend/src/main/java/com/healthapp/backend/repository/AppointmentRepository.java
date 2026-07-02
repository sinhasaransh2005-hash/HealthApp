package com.healthapp.backend.repository;

import com.healthapp.backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorId(Long doctorId);
    long countByDoctorIdAndBookingDate(Long doctorId, String bookingDate);
    List<Appointment> findByDoctorIdAndBookingDate(Long doctorId, String bookingDate);
}
