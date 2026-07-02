-- SQL Schema for Healthcare Management System

-- 1. Users Table
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `fullname` VARCHAR(100) NOT NULL,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `gender` VARCHAR(20) NOT NULL,
    `age` INT NOT NULL,
    `address` VARCHAR(255) NOT NULL,
    `gmail` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(30) NOT NULL,
    `role` VARCHAR(30) NOT NULL,
    `otp` VARCHAR(10),
    `otp_expiry` DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Appointments Table
CREATE TABLE IF NOT EXISTS `appointments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `patient_id` BIGINT NOT NULL,
    `doctor_id` BIGINT NOT NULL,
    `booking_date` VARCHAR(30) NOT NULL,
    `time_slot` VARCHAR(30) NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'Pending',
    `symptoms` TEXT,
    CONSTRAINT `fk_appointment_patient` FOREIGN KEY (`patient_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_appointment_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Prescriptions Table
CREATE TABLE IF NOT EXISTS `prescriptions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `patient_id` BIGINT NOT NULL,
    `doctor_id` BIGINT NOT NULL,
    `medicine_name` VARCHAR(100) NOT NULL,
    `dosage` VARCHAR(100) NOT NULL,
    `instructions` TEXT,
    `date` VARCHAR(30) NOT NULL,
    CONSTRAINT `fk_prescription_patient` FOREIGN KEY (`patient_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_prescription_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
