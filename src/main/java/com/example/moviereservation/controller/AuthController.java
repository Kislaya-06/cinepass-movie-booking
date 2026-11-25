package com.example.moviereservation.controller;

import com.example.moviereservation.dto.UserRegisterRequestDto;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller handling user registration and custom login page views.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            Authentication auth,
            Model model
    ) {
        if (isAuthenticated(auth)) {
            return "redirect:/";
        }

        if (error != null) {
            model.addAttribute("loginError", "Invalid email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutSuccess", "You have been safely signed out.");
        }
        if (registered != null) {
            model.addAttribute("registeredSuccess", "Account created successfully! You can now sign in.");
        }

        model.addAttribute("pageTitle", "Sign In - CinePass");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Authentication auth, Model model) {
        if (isAuthenticated(auth)) {
            return "redirect:/";
        }

        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new UserRegisterRequestDto());
        }
        model.addAttribute("pageTitle", "Create Account - CinePass");
        return "register";
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("registerRequest") UserRegisterRequestDto request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Create Account - CinePass");
            return "register";
        }

        try {
            userService.registerUser(request);
            redirectAttributes.addAttribute("registered", "true");
            return "redirect:/login";
        } catch (ApplicationException ex) {
            log.warn("Registration failed: {}", ex.getMessage());
            model.addAttribute("registrationError", ex.getMessage());
            model.addAttribute("pageTitle", "Create Account - CinePass");
            return "register";
        }
    }
}
