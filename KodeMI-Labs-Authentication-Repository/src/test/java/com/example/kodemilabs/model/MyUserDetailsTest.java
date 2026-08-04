package com.example.kodemilabs.model;

import com.example.kodemilabs.enums.Role;
import com.example.kodemilabs.service.MyUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class MyUserDetailsTest {

    // 🔧 helper methods
    private User createUser(Role role, boolean active) {
        User user = new User();
        user.setUsername("testUser");
        user.setPasswordHash("password123");
        user.setRole(role);
        user.setActive(active);
        return user;
    }

    private OTP createOtp(boolean enabled) {
        OTP otp = new OTP();
        otp.setEnable(enabled);
        return otp;
    }

    // ✅ AUTHORITIES
    @Test
    void getAuthorities_shouldReturnRole() {
        User user = createUser(Role.LEARNER, true);
        MyUserDetails details = new MyUserDetails(user, createOtp(true));

        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

        assertEquals(1, authorities.size());
        assertEquals("ROLE_LEARNER",
                authorities.iterator().next().getAuthority());
    }

    // ❌ ROLE NULL
    @Test
    void getAuthorities_shouldReturnEmpty_whenRoleNull() {
        User user = createUser(null, true);
        MyUserDetails details = new MyUserDetails(user, createOtp(true));

        assertTrue(details.getAuthorities().isEmpty());
    }

    // ❌ USER NULL
    @Test
    void getAuthorities_shouldReturnEmpty_whenUserNull() {
        MyUserDetails details = new MyUserDetails(null, createOtp(true));

        assertTrue(details.getAuthorities().isEmpty());
    }

    // ✅ PASSWORD
    @Test
    void getPassword_shouldReturnPassword() {
        User user = createUser(Role.LEARNER, true);
        user.setPasswordHash("secret");

        MyUserDetails details = new MyUserDetails(user, createOtp(true));

        assertEquals("secret", details.getPassword());
    }

    // ❌ PASSWORD NULL USER
    @Test
    void getPassword_shouldReturnNull_whenUserNull() {
        MyUserDetails details = new MyUserDetails(null, createOtp(true));

        assertNull(details.getPassword());
    }

    // ✅ USERNAME
    @Test
    void getUsername_shouldReturnUsername() {
        User user = createUser(Role.LEARNER, true);

        MyUserDetails details = new MyUserDetails(user, createOtp(true));

        assertEquals("testUser", details.getUsername());
    }

    // ❌ USERNAME NULL USER
    @Test
    void getUsername_shouldReturnNull_whenUserNull() {
        MyUserDetails details = new MyUserDetails(null, createOtp(true));

        assertNull(details.getUsername());
    }

    // ✅ ENABLED TRUE
    @Test
    void isEnabled_shouldReturnTrue_whenValid() {
        User user = createUser(Role.LEARNER, true);
        OTP otp = createOtp(true);

        MyUserDetails details = new MyUserDetails(user, otp);

        assertTrue(details.isEnabled());
    }

    // ❌ USER INACTIVE
    @Test
    void isEnabled_shouldReturnFalse_whenUserInactive() {
        User user = createUser(Role.LEARNER, false);
        OTP otp = createOtp(true);

        MyUserDetails details = new MyUserDetails(user, otp);

        assertFalse(details.isEnabled());
    }

    // ❌ OTP NULL
    @Test
    void isEnabled_shouldReturnFalse_whenOtpNull() {
        User user = createUser(Role.LEARNER, true);

        MyUserDetails details = new MyUserDetails(user, null);

        assertFalse(details.isEnabled());
    }

    // ❌ OTP DISABLED
    @Test
    void isEnabled_shouldReturnFalse_whenOtpDisabled() {
        User user = createUser(Role.LEARNER, true);
        OTP otp = createOtp(false);

        MyUserDetails details = new MyUserDetails(user, otp);

        assertFalse(details.isEnabled());
    }

    // ❌ USER NULL
    @Test
    void isEnabled_shouldReturnFalse_whenUserNull() {
        MyUserDetails details = new MyUserDetails(null, createOtp(true));

        assertFalse(details.isEnabled());
    }

    // ✅ ACCOUNT FLAGS
    @Test
    void accountFlags_shouldAlwaysBeTrue() {
        User user = createUser(Role.LEARNER, true);
        MyUserDetails details = new MyUserDetails(user, createOtp(true));

        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
    }
}