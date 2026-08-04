package com.example.kodemilabs.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.kodemilabs.exceptions.login.UserNotFoundException;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.model.UserFootPrint;
import com.example.kodemilabs.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserFootPrintService {
    private final DynamoDBMapper dynamoDBMapper;
    private final UserRepo userRepo;

    public UserFootPrintService(DynamoDBMapper dynamoDBMapper, UserRepo userRepo) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.userRepo = userRepo;
    }
    
    public void save(UserFootPrint footPrint) {
        if (footPrint == null || footPrint.getEmail() == null) {
            log.warn("Attempted to save footprint, but footprint or email is null");
            throw new IllegalArgumentException("FootPrint and email cannot be null");
        }
        
        String email = footPrint.getEmail();
        log.info("Capturing user footprint for email: {}", email);
        User user = userRepo.getUserByEmail(email);
        
        if (user == null) {
            log.warn("Failed to capture footprint – user not found for email: {}", email);
            throw new UserNotFoundException("User not found with email: " + email);
        }
        
        footPrint.setUserId(user.getUserId());
        
        // Sanitize all string fields
        footPrint.setBrowser(sanitize(footPrint.getBrowser()));
        footPrint.setBrowserVersion(sanitize(footPrint.getBrowserVersion()));
        footPrint.setOs(sanitize(footPrint.getOs()));
        footPrint.setPlatform(sanitize(footPrint.getPlatform()));
        footPrint.setDeviceType(sanitize(footPrint.getDeviceType()));
        footPrint.setLanguage(sanitize(footPrint.getLanguage()));
        footPrint.setTimeZone(sanitize(footPrint.getTimeZone()));
        footPrint.setScreenResolution(sanitize(footPrint.getScreenResolution()));
        footPrint.setViewport(sanitize(footPrint.getViewport()));
        footPrint.setColorDepth(sanitize(footPrint.getColorDepth()));
        footPrint.setTouchSupport(sanitize(footPrint.getTouchSupport()));
        footPrint.setConnectionType(sanitize(footPrint.getConnectionType()));
        footPrint.setDownLink(sanitize(footPrint.getDownLink()));
        footPrint.setRtt(sanitize(footPrint.getRtt()));
        footPrint.setPageUrl(sanitize(footPrint.getPageUrl()));
        footPrint.setEventType(sanitize(footPrint.getEventType()));
        footPrint.setCity(sanitize(footPrint.getCity()));
        footPrint.setRegion(sanitize(footPrint.getRegion()));
        footPrint.setCountry(sanitize(footPrint.getCountry()));
        footPrint.setPostal(sanitize(footPrint.getPostal()));
        dynamoDBMapper.save(footPrint);
        log.info("Successfully saved user footprint for user ID: {}", footPrint.getUserId());
    }
    
    private String sanitize(String input) {
        if (input == null) return null;
        return Jsoup.clean(input, Safelist.none());
    }
}
