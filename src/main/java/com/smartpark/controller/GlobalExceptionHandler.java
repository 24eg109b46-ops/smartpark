package com.smartpark.controller;

import com.smartpark.exception.BadRequestException;
import com.smartpark.exception.InvalidBookingStateException;
import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.exception.SlotNotAvailableException;
import com.smartpark.exception.VehicleAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request, Model model) {
        log.warn("Resource not found: {} for path: {}", ex.getMessage(), request.getRequestURI());
        model.addAttribute("status", 404);
        model.addAttribute("error", "Not Found");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, HttpServletRequest request, Model model) {
        log.warn("Access denied for path: {}", request.getRequestURI());
        model.addAttribute("status", 403);
        model.addAttribute("error", "Forbidden");
        model.addAttribute("message", "You do not have permission to access this resource.");
        model.addAttribute("path", request.getRequestURI());
        return "error/403";
    }

    @ExceptionHandler({
        BadRequestException.class,
        SlotNotAvailableException.class,
        InvalidBookingStateException.class,
        VehicleAlreadyExistsException.class
    })
    public String handleBusinessExceptions(RuntimeException ex, HttpServletRequest request, Model model) {
        log.warn("Business rule violation: {} on path: {}", ex.getMessage(), request.getRequestURI());
        model.addAttribute("status", 400);
        model.addAttribute("error", "Bad Request");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error/500";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled internal server error on path: {}", request.getRequestURI(), ex);
        model.addAttribute("status", 500);
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message", "An unexpected error occurred. Please try again or contact support.");
        model.addAttribute("path", request.getRequestURI());
        return "error/500";
    }
}
