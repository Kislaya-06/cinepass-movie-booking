package com.example.moviereservation.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    // Spring 6.1+ reports unknown URLs as NoResourceFoundException; without it they fell to the 500 handler
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex, Model model) {
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", "Oops! The page or resource you requested does not exist.");
        return "error";
    }

    @ExceptionHandler(SeatAlreadyBookedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleSeatAlreadyBooked(SeatAlreadyBookedException ex, Model model) {
        model.addAttribute("errorCode", 409);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(ApplicationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleApplicationException(ApplicationException ex, Model model) {
        model.addAttribute("errorCode", 400);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleUnauthorized(UnauthorizedActionException ex, Model model) {
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDataIntegrity(DataIntegrityViolationException ex, Model model) {
        model.addAttribute("errorCode", 409);
        model.addAttribute("errorMessage", "A concurrency or unique constraint conflict occurred. One or more seats may have already been reserved.");
        return "error";
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public void handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        throw ex;
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public void handleAuthentication(org.springframework.security.core.AuthenticationException ex) {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model) {
        log.error("Unhandled exception caught in GlobalExceptionHandler: ", ex);
        model.addAttribute("errorCode", 500);
        // Raw messages can expose SQL, class names or URLs; details stay in the log above
        model.addAttribute("errorMessage", "Something went wrong on our side. Please try again in a moment.");
        return "error";
    }
}
