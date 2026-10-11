package com.smartpark.controller;

import com.smartpark.dto.RegisterRequest;
import com.smartpark.dto.SlotSearchRequest;
import com.smartpark.service.ParkingSlotService;
import com.smartpark.service.ParkingZoneService;
import com.smartpark.service.PricingService;
import com.smartpark.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final ParkingZoneService parkingZoneService;
    private final ParkingSlotService parkingSlotService;
    private final PricingService pricingService;

    public AuthController(UserService userService,
                          ParkingZoneService parkingZoneService,
                          ParkingSlotService parkingSlotService,
                          PricingService pricingService) {
        this.userService = userService;
        this.parkingZoneService = parkingZoneService;
        this.parkingSlotService = parkingSlotService;
        this.pricingService = pricingService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("totalSlots", parkingSlotService.countTotalSlots());
        model.addAttribute("availableSlots", parkingSlotService.countSlotsByStatus(com.smartpark.enums.SlotStatus.AVAILABLE));
        model.addAttribute("occupiedSlots", parkingSlotService.countSlotsByStatus(com.smartpark.enums.SlotStatus.OCCUPIED));
        model.addAttribute("zones", parkingZoneService.getActiveZones());
        model.addAttribute("searchRequest", new SlotSearchRequest());
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password, or account is disabled.");
        }
        if (logout != null) {
            model.addAttribute("infoMessage", "You have been logged out successfully.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Registration successful! You may now sign in.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.registerRequest", "Passwords do not match");
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest", result);
            redirectAttributes.addFlashAttribute("registerRequest", request);
            return "redirect:/register";
        }

        try {
            userService.registerDriver(request);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully! Please sign in.");
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", request);
            return "redirect:/register";
        }
    }

    @GetMapping("/pricing")
    public String pricingPage(Model model) {
        model.addAttribute("rules", pricingService.getAllRules());
        return "pricing";
    }

    @GetMapping("/about")
    public String aboutPage() {
        return "about";
    }
}
