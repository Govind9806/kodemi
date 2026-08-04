package com.example.kodemilabs.model;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

@DynamoDBTable(tableName = "userfootprint")
@Data
@NoArgsConstructor
public class UserFootPrint {
    @DynamoDBIgnore
    private String email;
    private String userId;
    private String  browser;
    private String browserVersion;
    private String os;
    private String platform;
    private String deviceType;
    private String language;
    private String timeZone;
    private String screenResolution;
    private String viewport;
    private String colorDepth;
    private String touchSupport;
    private String connectionType;
    private String downLink;
    private String rtt;
    private String pageUrl;
    private String eventType;
    private String city;
    private String region;
    private String country;
    private String postal;


    @DynamoDBHashKey(attributeName = "userId")
    public String getUserId() {
        return userId;
    }

    @DynamoDBAttribute(attributeName ="downlink")
    public String getDownLink() {
        return downLink;
    }

    @DynamoDBAttribute(attributeName ="browser")
    public String getBrowser() {
        return browser;
    }

    @DynamoDBAttribute(attributeName ="browserversion")
    public String getBrowserVersion() {
        return browserVersion;
    }

    @DynamoDBAttribute(attributeName ="os")
    public String getOs() {
        return os;
    }

    @DynamoDBAttribute(attributeName ="platform")
    public String getPlatform() {
        return platform;
    }

    @DynamoDBAttribute(attributeName ="language")
    public String getLanguage() {
        return language;
    }

    @DynamoDBAttribute(attributeName ="devicetype")
    public String getDeviceType() {
        return deviceType;
    }

    @DynamoDBAttribute(attributeName ="timezone")
    public String getTimeZone() {
        return timeZone;
    }

    @DynamoDBAttribute(attributeName ="screenresolution")
    public String getScreenResolution() {
        return screenResolution;
    }

    @DynamoDBAttribute(attributeName ="viewport")
    public String getViewport() {
        return viewport;
    }

    @DynamoDBAttribute(attributeName ="colordepth")
    public String getColorDepth() {
        return colorDepth;
    }

    @DynamoDBAttribute(attributeName ="touchsupport")
    public String getTouchSupport() {
        return touchSupport;
    }

    @DynamoDBAttribute(attributeName ="connectiontype")
    public String getConnectionType() {
        return connectionType;
    }

    @DynamoDBAttribute(attributeName ="rtt")
    public String getRtt() {
        return rtt;
    }

    @DynamoDBAttribute(attributeName ="pageurl")
    public String getPageUrl() {
        return pageUrl;
    }

    @DynamoDBAttribute(attributeName ="eventtype")
    public String getEventType() {
        return eventType;
    }

    @DynamoDBAttribute(attributeName ="city")
    public String getCity() {
        return city;
    }

    @DynamoDBAttribute(attributeName ="region")
    public String getRegion() {
        return region;
    }

    @DynamoDBAttribute(attributeName ="country")
    public String getCountry() {
        return country;
    }

    @DynamoDBAttribute(attributeName ="postal")
    public String getPostal() {
        return postal;
    }
}