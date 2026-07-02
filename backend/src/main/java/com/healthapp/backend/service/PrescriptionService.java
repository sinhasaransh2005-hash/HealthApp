package com.healthapp.backend.service;

import com.healthapp.backend.entity.Prescription;
import com.healthapp.backend.entity.User;
import com.healthapp.backend.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class PrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    public List<Prescription> getPatientPrescriptions(Long patientId) {
        return prescriptionRepository.findByPatientId(patientId);
    }

    public List<Prescription> getDoctorPrescriptions(Long doctorId) {
        return prescriptionRepository.findByDoctorId(doctorId);
    }

    public Prescription createPrescription(User patient, User doctor, String medicineName, String dosage, String instructions) {
        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescription.setMedicineName(medicineName);
        prescription.setDosage(dosage);
        prescription.setInstructions(instructions);
        prescription.setDate(LocalDate.now().toString());
        return prescriptionRepository.save(prescription);
    }
}
