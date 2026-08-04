package com.example.kodemilabs.exceptions;

import com.example.kodemilabs.exceptions.jwt.AccessDeniedException;
import com.example.kodemilabs.exceptions.jwt.InvalidTokenException;
import com.example.kodemilabs.exceptions.jwt.TokenExpiredException;
import com.example.kodemilabs.exceptions.login.AccountLockedException;
import com.example.kodemilabs.exceptions.login.InvalidCredentialsException;
import com.example.kodemilabs.exceptions.login.TooManyRequestsException;
import com.example.kodemilabs.exceptions.login.UserNotFoundException;
import com.example.kodemilabs.exceptions.otp.OtpAlreadyUsedException;
import com.example.kodemilabs.exceptions.registration.EmailAlreadyExistsException;
import com.example.kodemilabs.exceptions.registration.InvalidRegistrationDataException;
import com.example.kodemilabs.exceptions.registration.UsernameAlreadyExistsException;
import com.example.kodemilabs.exceptions.registration.WeakPasswordException;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.core.MethodParameter;

import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEmailAlreadyExists_returns409() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleEmailAlreadyExists(new EmailAlreadyExistsException("Email already exists"));

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals(409, resp.getBody().get("status"));
        assertEquals("Email already exists", resp.getBody().get("message"));
        assertNotNull(resp.getBody().get("timestamp"));
    }

    @Test
    void handleUsernameAlreadyExists_returns409() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleUsernameAlreadyExists(new UsernameAlreadyExistsException("Username taken"));

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("Username taken", resp.getBody().get("message"));
    }

    @Test
    void handleWeakPassword_returns400() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleWeakPassword(new WeakPasswordException("Too weak"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Too weak", resp.getBody().get("message"));
    }

    @Test
    void handleInvalidRegistrationData_returns400() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleInvalidRegistrationData(new InvalidRegistrationDataException("Invalid"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(400, resp.getBody().get("status"));
    }

    // ===================== LOGIN =====================

    @Test
    void handleUserNotFound_returns404() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleUserNotFound(new UserNotFoundException("Not found"));

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Not found", resp.getBody().get("message"));
    }

    @Test
    void handleInvalidCredentials_returns401() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleInvalidCredentials(new InvalidCredentialsException("Auth failed"));

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Auth failed", resp.getBody().get("message"));
    }

    @Test
    void handleAccountLocked_returns403() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleAccountLocked(new AccountLockedException("Locked"));

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("Locked", resp.getBody().get("message"));
    }

    @Test
    void handleTooManyRequests_returns429() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleTooManyRequests(new TooManyRequestsException("Slow down"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, resp.getStatusCode());
        assertEquals(429, resp.getBody().get("status"));
    }

    // ===================== OTP =====================

    @Test
    void handleOtpAlreadyUsed_returns400() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleOtpAlreadyUsed(new OtpAlreadyUsedException("Used"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Used", resp.getBody().get("message"));
    }

    @Test
    void handleOtpInvalidToken_returns400() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleOtpInvalidToken(
                        new com.example.kodemilabs.exceptions.otp.InvalidTokenException("Bad OTP"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Bad OTP", resp.getBody().get("message"));
    }

    @Test
    void handleOtpTokenExpired_returns400() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleOtpTokenExpired(
                        new com.example.kodemilabs.exceptions.otp.TokenExpiredException("OTP expired"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("OTP expired", resp.getBody().get("message"));
    }

    // ===================== JWT =====================

    @Test
    void handleJwtInvalidToken_returns401() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleJwtInvalidToken(new InvalidTokenException("Invalid JWT"));

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Invalid JWT", resp.getBody().get("message"));
    }

    @Test
    void handleJwtTokenExpired_returns401() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleJwtTokenExpired(new TokenExpiredException("JWT expired"));

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals(401, resp.getBody().get("status"));
    }

    @Test
    void handleAccessDenied_returns403() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleAccessDenied(new AccessDeniedException("No access"));

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("No access", resp.getBody().get("message"));
    }



    @Test
    void handleMethodArgumentNotValid_returns400_withValidationErrors() throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        bindingResult.addError(new FieldError("request", "password", "must not be blank"));

        java.lang.reflect.Method method = String.class.getDeclaredMethod("trim");
        MethodParameter mp = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, bindingResult);

        ResponseEntity<Map<String, Object>> resp = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Validation failed", resp.getBody().get("message"));
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) resp.getBody().get("validationErrors");
        assertNotNull(errors);
        assertEquals("must not be blank", errors.get("email"));
    }

    @Test
    void handleConstraintViolation_returns400() {
        ConstraintViolationException ex =
                new ConstraintViolationException("Constraint failed", new HashSet<>());

        ResponseEntity<Map<String, Object>> resp = handler.handleConstraintViolations(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Validation failed", resp.getBody().get("message"));
    }

    @Test
    void handleHttpMessageNotReadable_returns400() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Malformed JSON",
                        new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Map<String, Object>> resp = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(
                "Malformed JSON request. Please check your spelling and formatting.",
                resp.getBody().get("message")
        );
    }

    @Test
    void handleMissingParam_returns400() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("email", "String");

        ResponseEntity<Map<String, Object>> resp = handler.handleMissingParams(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().get("message").toString().contains("email"));
    }

    @Test
    void handleTypeMismatch_returns400() {
        MethodParameter mp = Mockito.mock(MethodParameter.class);
        Mockito.when(mp.getParameterType()).thenReturn((Class) Integer.class);

        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "id", mp, null);

        ResponseEntity<Map<String, Object>> resp = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().get("message").toString().contains("id"));
    }

    // ===================== GENERIC =====================

    @Test
    void handleIllegalArgument_returns400() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleIllegalArgument(new IllegalArgumentException("Bad arg"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Bad arg", resp.getBody().get("message"));
    }

    @Test
    void handleGenericException_returns500() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleGenericException(new RuntimeException("Unexpected"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("An unexpected error occurred", resp.getBody().get("message"));
        assertEquals(500, resp.getBody().get("status"));
    }
}
