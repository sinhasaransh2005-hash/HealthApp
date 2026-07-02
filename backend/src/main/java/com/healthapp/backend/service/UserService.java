package com.healthapp.backend.service;

import com.healthapp.backend.dto.UserRegistrationDto;
import com.healthapp.backend.entity.User;
import com.healthapp.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public User registerNewUser(UserRegistrationDto registrationDto) {
        User user = new User();
        user.setFullname(registrationDto.getFullname());
        user.setUsername(registrationDto.getUsername());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setGender(registrationDto.getGender());
        user.setAge(registrationDto.getAge());
        user.setAddress(registrationDto.getAddress());
        user.setGmail(registrationDto.getGmail());
        user.setPhone(registrationDto.getPhone());
        user.setRole(registrationDto.getRole());
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findAllDoctors() {
        return userRepository.findByRole("ROLE_DOCTOR");
    }

    public List<User> findAllPatients() {
        return userRepository.findByRole("ROLE_PATIENT");
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    // Password Recovery Flows
    public boolean initiatePasswordReset(String identity) {
        Optional<User> userOpt = userRepository.findByUsername(identity);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByGmail(identity);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Generate 6-digit numeric OTP
            String otp = String.format("%06d", new Random().nextInt(999999));
            user.setOtp(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            // Simulate sending email by printing to standard output console
            System.out.println("=================================================");
            System.out.println("[SIMULATED EMAIL SERVICE] Password Recovery OTP");
            System.out.println("User: " + user.getUsername());
            System.out.println("Gmail: " + user.getGmail());
            System.out.println("Generated OTP Code: " + otp);
            System.out.println("Valid for 5 minutes");
            System.out.println("=================================================");

            return true;
        }
        return false;
    }

    public boolean verifyOtp(String identity, String otp) {
        Optional<User> userOpt = userRepository.findByUsername(identity);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByGmail(identity);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getOtp() != null && user.getOtp().equals(otp) &&
                user.getOtpExpiry() != null && user.getOtpExpiry().isAfter(LocalDateTime.now())) {
                return true;
            }
        }
        return false;
    }

    public boolean resetPassword(String identity, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsername(identity);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByGmail(identity);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setOtp(null); // Clear OTP
            user.setOtpExpiry(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
