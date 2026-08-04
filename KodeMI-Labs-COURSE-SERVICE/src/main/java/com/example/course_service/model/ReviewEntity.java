package com.example.course_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
@DynamoDBTable(tableName = "Review")
public class ReviewEntity {

    private String reviewId;
    private String courseId;
    private String userId;
    private String reviewerName;
    private String reviewerPhoto;
    private Integer rating;
    private String reviewText;
    private Boolean isVerified;
    private Integer likes;
    private Date createdAt;
    private Date updatedAt;

    @DynamoDBHashKey(attributeName = "reviewId")
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "course-index", attributeName = "courseId")
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    @DynamoDBAttribute(attributeName = "userId")
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @DynamoDBAttribute(attributeName = "reviewerName")
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    @DynamoDBAttribute(attributeName = "reviewerPhoto")
    public String getReviewerPhoto() { return reviewerPhoto; }
    public void setReviewerPhoto(String reviewerPhoto) { this.reviewerPhoto = reviewerPhoto; }

    @DynamoDBAttribute(attributeName = "rating")
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    @DynamoDBAttribute(attributeName = "reviewText")
    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    @DynamoDBAttribute(attributeName = "isVerified")
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    @DynamoDBAttribute(attributeName = "likes")
    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }

    @DynamoDBAttribute(attributeName = "createdAt")
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @DynamoDBAttribute(attributeName = "updatedAt")
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}