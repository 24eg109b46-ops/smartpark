package com.smartpark.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int statusCode = 500;
        if (status != null) {
            try {
                statusCode = Integer.parseInt(status.toString());
            } catch (NumberFormatException ignored) {}
        }

        model.addAttribute("status", statusCode);
        model.addAttribute("path", requestUri != null ? requestUri : request.getRequestURI());
        model.addAttribute("message", (message != null && !message.toString().isBlank()) ? message : "An error occurred processing your request.");

        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            model.addAttribute("error", "Page Not Found");
            return "error/404";
        } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
            model.addAttribute("error", "Access Denied");
            return "error/403";
        }

        model.addAttribute("error", "Server Error");
        return "error/500";
    }

    @RequestMapping("/error/403")
    public String accessDenied(Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("error", "Access Denied");
        model.addAttribute("message", "You do not have permission to access this page.");
        return "error/403";
    }

    @RequestMapping("/error/404")
    public String notFound(Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Page Not Found");
        model.addAttribute("message", "The page or resource you requested could not be found.");
        return "error/404";
    }

    @RequestMapping("/error/500")
    public String serverError(Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message", "A server error occurred. Please try again later.");
        return "error/500";
    }
}
