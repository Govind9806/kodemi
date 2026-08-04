package com.example.course_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;

import java.util.Date;
import java.util.Set;

@Data
@DynamoDBTable(tableName = "Course")
public class CourseEntity {

    private String courseId;
    private String title;
    private String categoryId;
    private Set<String> category;
    private Set<String> subCategory;
    private Set<String> topic;
    private String creatorId;
    private String creatorName;
    private String description;
    private String thumbnailKey;
    private String demoVideoKey;
    private Double price;
    private String language;
    private String skillLevel;
    private String status;
    private Integer lessonCount;
    private String durationLabel;
    private Double averageRating;
    private Integer totalReviews;
    private String welcomeMessage;
    private String categoryName;
    private Boolean isVerified;
    private Date createdAt;
    private Date updatedAt;
    private String courseType;
    private String verifierId;

    @DynamoDBHashKey(attributeName = "courseId")
    public String getCourseId() {
        return courseId;
    }

    @DynamoDBAttribute(attributeName = "title")
    public String getTitle() {
        return title;
    }

    @DynamoDBAttribute(attributeName = "categoryId")
    public String getCategoryId() {
        return categoryId;
    }

    @DynamoDBAttribute(attributeName = "category")
    public Set<String> getCategory() {
        return category;
    }

    @DynamoDBAttribute(attributeName = "subCategory")
    public Set<String> getSubCategory() {
        return subCategory;
    }

    @DynamoDBAttribute(attributeName = "topic")
    public Set<String> getTopic() {
        return topic;
    }

    @DynamoDBAttribute(attributeName = "creatorId")
    public String getCreatorId() {
        return creatorId;
    }

    @DynamoDBAttribute(attributeName = "creatorName")
    public String getCreatorName() {
        return creatorName;
    }

    @DynamoDBAttribute(attributeName = "description")
    public String getDescription() {
        return description;
    }

    @DynamoDBAttribute(attributeName = "thumbnailKey")
    public String getThumbnailKey() {
        return thumbnailKey;
    }

    @DynamoDBAttribute(attributeName = "demoVideoKey")
    public String getDemoVideoKey() {
        return demoVideoKey;
    }

    @DynamoDBAttribute(attributeName = "price")
    public Double getPrice() {
        return price;
    }

    @DynamoDBAttribute(attributeName = "language")
    public String getLanguage() {
        return language;
    }

    @DynamoDBAttribute(attributeName = "skillLevel")
    public String getSkillLevel() {
        return skillLevel;
    }

    @DynamoDBAttribute(attributeName = "status")
    public String getStatus() {
        return status;
    }

    @DynamoDBAttribute(attributeName = "lessonCount")
    public Integer getLessonCount() {
        return lessonCount;
    }

    @DynamoDBAttribute(attributeName = "durationLabel")
    public String getDurationLabel() {
        return durationLabel;
    }

    @DynamoDBAttribute(attributeName = "averageRating")
    public Double getAverageRating() {
        return averageRating;
    }

    @DynamoDBAttribute(attributeName = "totalReviews")
    public Integer getTotalReviews() {
        return totalReviews;
    }

    @DynamoDBAttribute(attributeName = "welcomeMessage")
    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    @DynamoDBAttribute(attributeName = "categoryName")
    public String getCategoryName() {
        return categoryName;
    }

    @DynamoDBAttribute(attributeName = "isVerified")
    public Boolean getIsVerified() {
        return isVerified;
    }

    @DynamoDBAttribute(attributeName = "createdAt")
    public Date getCreatedAt() {
        return createdAt;
    }

    @DynamoDBAttribute(attributeName = "updatedAt")
    public Date getUpdatedAt() {
        return updatedAt;
    }

    @DynamoDBAttribute(attributeName = "courseType")
    public String getCourseType() {
        return courseType;
    }

    @DynamoDBAttribute(attributeName = "verifierId")
    public String getVerifierId() {
        return verifierId;
    }
}