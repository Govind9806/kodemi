package com.example.kodemilabs.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserFootPrintTest {

    @Test
    void testGettersAndSetters() {
        UserFootPrint footPrint = new UserFootPrint();

        footPrint.setEmail("test@example.com");
        footPrint.setUserId("u123");
        footPrint.setBrowser("Chrome");
        footPrint.setBrowserVersion("112.0");
        footPrint.setOs("Windows 10");
        footPrint.setPlatform("Desktop");
        footPrint.setDeviceType("Laptop");
        footPrint.setLanguage("en-US");
        footPrint.setTimeZone("GMT+0");
        footPrint.setScreenResolution("1920x1080");
        footPrint.setViewport("1920x1040");
        footPrint.setColorDepth("24");
        footPrint.setTouchSupport("false");
        footPrint.setConnectionType("wifi");
        footPrint.setDownLink("100Mbps");
        footPrint.setRtt("50ms");
        footPrint.setPageUrl("https://example.com");
        footPrint.setEventType("click");
        footPrint.setCity("New York");
        footPrint.setRegion("NY");
        footPrint.setCountry("USA");
        footPrint.setPostal("10001");

        assertEquals("test@example.com", footPrint.getEmail());
        assertEquals("u123", footPrint.getUserId());
        assertEquals("Chrome", footPrint.getBrowser());
        assertEquals("112.0", footPrint.getBrowserVersion());
        assertEquals("Windows 10", footPrint.getOs());
        assertEquals("Desktop", footPrint.getPlatform());
        assertEquals("Laptop", footPrint.getDeviceType());
        assertEquals("en-US", footPrint.getLanguage());
        assertEquals("GMT+0", footPrint.getTimeZone());
        assertEquals("1920x1080", footPrint.getScreenResolution());
        assertEquals("1920x1040", footPrint.getViewport());
        assertEquals("24", footPrint.getColorDepth());
        assertEquals("false", footPrint.getTouchSupport());
        assertEquals("wifi", footPrint.getConnectionType());
        assertEquals("100Mbps", footPrint.getDownLink());
        assertEquals("50ms", footPrint.getRtt());
        assertEquals("https://example.com", footPrint.getPageUrl());
        assertEquals("click", footPrint.getEventType());
        assertEquals("New York", footPrint.getCity());
        assertEquals("NY", footPrint.getRegion());
        assertEquals("USA", footPrint.getCountry());
        assertEquals("10001", footPrint.getPostal());
    }

    @Test
    void testEqualsHashCodeAndToString() {
        UserFootPrint fp1 = new UserFootPrint();
        fp1.setUserId("u123");

        UserFootPrint fp2 = new UserFootPrint();
        fp2.setUserId("u123");

        // equals and hashCode
        assertEquals(fp1, fp2);
        assertEquals(fp1.hashCode(), fp2.hashCode());

        // toString
        assertNotNull(fp1.toString());
    }
}