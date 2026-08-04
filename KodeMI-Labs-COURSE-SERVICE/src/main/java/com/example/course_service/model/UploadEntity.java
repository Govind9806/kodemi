package com.example.course_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;

import java.util.Date;

@Data
@DynamoDBTable(tableName = "Uploads")
public class UploadEntity {

    private String uploadId;
    private String ownerId;
    private String fileKey;
    private String fileType;
    private String uploadStatus;
    private String linkedEntityId;
    private Boolean isUsed;
    private Date createdAt;
    private Date updatedAt;
    private Long expiresAt;

    @DynamoDBHashKey(attributeName = "uploadId")
    public String getUploadId() {
        return uploadId;
    }

    @DynamoDBAttribute(attributeName = "ownerId")
    public String getOwnerId() {
        return ownerId;
    }

    @DynamoDBAttribute(attributeName = "fileKey")
    public String getFileKey() {
        return fileKey;
    }

    @DynamoDBAttribute(attributeName = "fileType")
    public String getFileType() {
        return fileType;
    }

    @DynamoDBAttribute(attributeName = "uploadStatus")
    public String getUploadStatus() {
        return uploadStatus;
    }

    @DynamoDBAttribute(attributeName = "linkedEntityId")
    public String getLinkedEntityId() {
        return linkedEntityId;
    }

    @DynamoDBAttribute(attributeName = "isUsed")
    public Boolean getIsUsed() {
        return isUsed;
    }

    @DynamoDBAttribute(attributeName = "createdAt")
    public Date getCreatedAt() {
        return createdAt;
    }

    @DynamoDBAttribute(attributeName = "updatedAt")
    public Date getUpdatedAt() {
        return updatedAt;
    }

    @DynamoDBAttribute(attributeName = "expiresAt")
    public Long getExpiresAt() {
        return expiresAt;
    }
}
