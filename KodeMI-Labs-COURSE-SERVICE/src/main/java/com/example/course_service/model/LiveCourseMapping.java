package com.example.course_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.course_service.util.InstantConverter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "LiveCourseMapping")
public class LiveCourseMapping {

    private String id;
    private String courseId;
    private String liveSessionId;
    private String recordingUrl;
    private Instant createdAt;
    private Instant updatedAt;

    @DynamoDBHashKey(attributeName = "Id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDBAttribute(attributeName = "courseId")
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    @DynamoDBAttribute(attributeName = "liveSessionId")
    public String getLiveSessionId() { return liveSessionId; }
    public void setLiveSessionId(String liveSessionId) { this.liveSessionId = liveSessionId; }

    @DynamoDBAttribute(attributeName = "recordingUrl")
    public String getRecordingUrl() { return recordingUrl; }
    public void setRecordingUrl(String recordingUrl) { this.recordingUrl = recordingUrl; }

    @DynamoDBAttribute(attributeName = "createdAt")
    @DynamoDBTypeConverted(converter = InstantConverter.class)
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @DynamoDBAttribute(attributeName = "updatedAt")
    @DynamoDBTypeConverted(converter = InstantConverter.class)
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
