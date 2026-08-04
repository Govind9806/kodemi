package com.example.kodemilabs.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResetPassword Model Tests")
class ResetPasswordTest {

    @Nested
    @DisplayName("Getters and Setters")
    class GetterSetterTests {

        @Test
        @DisplayName("Should set and get token correctly")
        void setAndGetToken() {
            ResetPassword resetPassword = new ResetPassword();
            resetPassword.setToken("test-token-uuid-123");

            assertThat(resetPassword.getToken()).isEqualTo("test-token-uuid-123");
        }

        @Test
        @DisplayName("Should set and get newPassword correctly")
        void setAndGetNewPassword() {
            ResetPassword resetPassword = new ResetPassword();
            resetPassword.setNewPassword("NewPassword@123");

            assertThat(resetPassword.getNewPassword()).isEqualTo("NewPassword@123");
        }

        @Test
        @DisplayName("Should return null when token is not set")
        void tokenNotSet_ReturnsNull() {
            ResetPassword resetPassword = new ResetPassword();
            assertThat(resetPassword.getToken()).isNull();
        }

        @Test
        @DisplayName("Should return null when newPassword is not set")
        void newPasswordNotSet_ReturnsNull() {
            ResetPassword resetPassword = new ResetPassword();
            assertThat(resetPassword.getNewPassword()).isNull();
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Should be equal when token and newPassword are the same")
        void equals_SameFields_ReturnsTrue() {
            ResetPassword r1 = new ResetPassword();
            r1.setToken("token-abc");
            r1.setNewPassword("Password@123");

            ResetPassword r2 = new ResetPassword();
            r2.setToken("token-abc");
            r2.setNewPassword("Password@123");

            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("Should have same hashCode when fields are equal")
        void hashCode_SameFields_ReturnsSameHash() {
            ResetPassword r1 = new ResetPassword();
            r1.setToken("token-abc");
            r1.setNewPassword("Password@123");

            ResetPassword r2 = new ResetPassword();
            r2.setToken("token-abc");
            r2.setNewPassword("Password@123");

            assertThat(r1).hasSameHashCodeAs(r2);
        }

        @Test
        @DisplayName("Should not be equal when tokens differ")
        void equals_DifferentToken_ReturnsFalse() {
            ResetPassword r1 = new ResetPassword();
            r1.setToken("token-abc");
            r1.setNewPassword("Password@123");

            ResetPassword r2 = new ResetPassword();
            r2.setToken("token-xyz");
            r2.setNewPassword("Password@123");

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("Should not be equal when passwords differ")
        void equals_DifferentPassword_ReturnsFalse() {
            ResetPassword r1 = new ResetPassword();
            r1.setToken("token-abc");
            r1.setNewPassword("Password@123");

            ResetPassword r2 = new ResetPassword();
            r2.setToken("token-abc");
            r2.setNewPassword("DifferentPassword@456");

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void equals_Null_ReturnsFalse() {
            ResetPassword r1 = new ResetPassword();
            r1.setToken("token-abc");

            assertThat(r1).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void equals_SameInstance_ReturnsTrue() {
            ResetPassword r1 = new ResetPassword();
            r1.setToken("token-abc");
            r1.setNewPassword("Password@123");

            assertThat(r1).isEqualTo(r1);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("Should contain token in toString output")
        void toString_ContainsToken() {
            ResetPassword resetPassword = new ResetPassword();
            resetPassword.setToken("token-123");
            resetPassword.setNewPassword("Password@123");

            assertThat(resetPassword.toString()).contains("token-123");
        }

        @Test
        @DisplayName("Should contain newPassword in toString output")
        void toString_ContainsNewPassword() {
            ResetPassword resetPassword = new ResetPassword();
            resetPassword.setToken("token-123");
            resetPassword.setNewPassword("Password@123");

            assertThat(resetPassword.toString()).contains("Password@123");
        }

        @Test
        @DisplayName("Should return non-null toString")
        void toString_ReturnsNonNull() {
            ResetPassword resetPassword = new ResetPassword();
            assertThat(resetPassword.toString()).isNotNull();
        }
    }
}