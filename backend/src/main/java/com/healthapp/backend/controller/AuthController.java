package com.healthapp.backend.controller;

import com.healthapp.backend.dto.UserRegistrationDto;
import com.healthapp.backend.entity.User;
import com.healthapp.backend.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String showLandingPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && 
            !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/interface.html";
        }
        return "landing";
    }

    @GetMapping("/login")
    public String showLoginPage(Authentication authentication,
                                @RequestParam(value = "role", defaultValue = "patient") String role,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "captchaError", required = false) String captchaError,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "resetSuccess", required = false) String resetSuccess,
                                Model model) {
        if (authentication != null && authentication.isAuthenticated() && 
            !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/interface.html";
        }
        model.addAttribute("role", role);
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password.");
        }
        if (captchaError != null) {
            model.addAttribute("errorMessage", "Invalid Captcha Code! Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        if (resetSuccess != null) {
            model.addAttribute("successMessage", "Password reset successfully! Please login with your new password.");
        }
        return "login";
    }

    @GetMapping("/auth/success")
    public String showAuthSuccess(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated() || 
            (authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/login";
        }
        String username = authentication.getName();
        User user = userService.findByUsername(username).orElseThrow();
        model.addAttribute("user", user);
        return "auth-success";
    }

    @GetMapping("/register")
    public String showRegisterPage(@RequestParam(value = "role", defaultValue = "patient") String role, Model model) {
        UserRegistrationDto user = new UserRegistrationDto();
        user.setRole(role.equalsIgnoreCase("doctor") ? "ROLE_DOCTOR" : "ROLE_PATIENT");
        model.addAttribute("user", user);
        model.addAttribute("role", role);
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                               BindingResult result,
                               @RequestParam(value = "role", defaultValue = "patient") String role,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        
        // 1. Validate password match
        if (userDto.getPassword() != null && !userDto.getPassword().equals(userDto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.user", "Passwords do not match");
        }

        // 2. Validate username uniqueness
        if (userDto.getUsername() != null && userService.usernameExists(userDto.getUsername())) {
            result.rejectValue("username", "error.user", "Username is already taken");
        }

        if (result.hasErrors()) {
            model.addAttribute("role", role);
            return "register";
        }

        userService.registerNewUser(userDto);
        redirectAttributes.addFlashAttribute("successMessage", "Account created successfully! Please login.");
        return "redirect:/login?role=" + (userDto.getRole().equals("ROLE_DOCTOR") ? "doctor" : "patient");
    }

    // Forgot Password Flow
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("identity") String identity,
                                        HttpSession session,
                                        Model model) {
        boolean initiated = userService.initiatePasswordReset(identity);
        if (initiated) {
            session.setAttribute("recoveryIdentity", identity);
            return "redirect:/verify-otp";
        } else {
            model.addAttribute("errorMessage", "No account found with that username or email address.");
            return "forgot-password";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtpForm(HttpSession session, Model model) {
        String identity = (String) session.getAttribute("recoveryIdentity");
        if (identity == null) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("identity", identity);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String processVerifyOtp(@RequestParam("otp") String otp,
                                   HttpSession session,
                                   Model model) {
        String identity = (String) session.getAttribute("recoveryIdentity");
        if (identity == null) {
            return "redirect:/forgot-password";
        }

        boolean verified = userService.verifyOtp(identity, otp);
        if (verified) {
            session.setAttribute("otpVerified", true);
            return "redirect:/reset-password";
        } else {
            model.addAttribute("errorMessage", "Invalid or expired OTP. Please try again.");
            model.addAttribute("identity", identity);
            return "verify-otp";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(HttpSession session) {
        String identity = (String) session.getAttribute("recoveryIdentity");
        Boolean verified = (Boolean) session.getAttribute("otpVerified");
        if (identity == null || verified == null || !verified) {
            return "redirect:/forgot-password";
        }
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("password") String password,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       HttpSession session,
                                       Model model) {
        String identity = (String) session.getAttribute("recoveryIdentity");
        Boolean verified = (Boolean) session.getAttribute("otpVerified");
        if (identity == null || verified == null || !verified) {
            return "redirect:/forgot-password";
        }

        if (password == null || password.trim().length() < 6) {
            model.addAttribute("errorMessage", "Password must be at least 6 characters.");
            return "reset-password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "reset-password";
        }

        userService.resetPassword(identity, password);
        session.removeAttribute("recoveryIdentity");
        session.removeAttribute("otpVerified");
        return "redirect:/login?resetSuccess=true";
    }
}
