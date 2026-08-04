package com.example.course_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "LiveCourseMapping")
public class LiveCourseMapping {

    private String id;
    private String courseId;
    private String liveSessionId;
    private String recordingUrl;
    private Date createdAt;
    private Date updatedAt;

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
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @DynamoDBAttribute(attributeName = "updatedAt")
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
