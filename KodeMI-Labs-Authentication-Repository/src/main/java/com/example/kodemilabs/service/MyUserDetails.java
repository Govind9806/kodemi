package com.example.kodemilabs.service;

import com.example.kodemilabs.enums.Role;
import com.example.kodemilabs.model.OTP;
import com.example.kodemilabs.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class MyUserDetails implements UserDetails {

    private transient User user;
    private transient OTP otp;

    public MyUserDetails(User user, OTP otp) {
        this.user = user;
        this.otp = otp;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role role = user != null ? user.getRole() : null;
        if (role != null) {
            return Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + role.name())
            );
        }
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return user != null ? user.getPasswordHash() : null;
    }

    @Override
    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user != null && user.isActive() && otp != null && otp.isEnable();
    }
}