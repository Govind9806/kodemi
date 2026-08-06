package com.example.course_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.course_service.util.InstantConverter;
import lombok.Data;
import java.time.Instant;

@Data
@DynamoDBTable(tableName = "Module")
public class ModuleEntity {

    private String moduleId;
    private String courseId;
    private String userId;
    private String title;
    private String description;
    private Integer orderIndex;
    private Instant createdAt;
    private Instant updatedAt;

    @DynamoDBHashKey(attributeName = "moduleId")
    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "course-index", attributeName = "courseId")
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    @DynamoDBAttribute(attributeName = "userId")
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @DynamoDBAttribute(attributeName = "title")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @DynamoDBAttribute(attributeName = "description")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @DynamoDBAttribute(attributeName = "orderIndex")
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    @DynamoDBAttribute(attributeName = "createdAt")
    @DynamoDBTypeConverted(converter = InstantConverter.class)
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @DynamoDBAttribute(attributeName = "updatedAt")
    @DynamoDBTypeConverted(converter = InstantConverter.class)
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}