package com.example.course_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import java.util.Date;
import java.util.List;

@Data
@DynamoDBTable(tableName = "Lesson")
public class LessonEntity {

    private String lessonId;
    private String moduleId;
    private String title;
    private String description;
    private Integer duration;
    private Integer orderIndex;
    private List<ContentItem> contentKey;
    private Date createdAt;
    private Date updatedAt;
    private String lessonType;
    private String liveSessionId;
    private Date scheduledAt;
    private String videoKey;

    @DynamoDBHashKey(attributeName = "lessonId")
    public String getLessonId() { return lessonId; }
    public void setLessonId(String lessonId) { this.lessonId = lessonId; }
 
    @DynamoDBAttribute(attributeName = "videoKey")
    public String getVideoKey() { return videoKey; }
    public void setVideoKey(String videoKey) { this.videoKey = videoKey; }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "module-index", attributeName = "moduleId")
    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }

    @DynamoDBAttribute(attributeName = "title")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @DynamoDBAttribute(attributeName = "description")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @DynamoDBAttribute(attributeName = "duration")
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    @DynamoDBAttribute(attributeName = "orderIndex")
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    @DynamoDBAttribute(attributeName = "contentKey")
    @DynamoDBTypeConverted(converter = ContentItemListConverter.class)
    public List<ContentItem> getContentKey() { return contentKey; }
    public void setContentKey(List<ContentItem> contentKey) { this.contentKey = contentKey; }

    @DynamoDBAttribute(attributeName = "createdAt")
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @DynamoDBAttribute(attributeName = "updatedAt")
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public static class ContentItem {
        private String type;
        private String key;
        private String processedKey;
        private String status;
        private String label;
        private Integer order;

        public ContentItem() {
        }

        public ContentItem(String type,
                           String key,
                           String processedKey,
                           String status,
                           String label,
                           Integer order) {
            this.type = type;
            this.key = key;
            this.processedKey = processedKey;
            this.status = status;
            this.label = label;
            this.order = order;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getProcessedKey() {
            return processedKey;
        }

        public void setProcessedKey(String processedKey) {
            this.processedKey = processedKey;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }
    }

    public static class ContentItemListConverter
            implements DynamoDBTypeConverter<String, List<ContentItem>> {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        @Override
        public String convert(List<ContentItem> items) {
            try {
                return MAPPER.writeValueAsString(items);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize contentKey", e);
            }
        }

        @Override
        public List<ContentItem> unconvert(String json) {
            try {
                return MAPPER.readValue(json, new TypeReference<List<ContentItem>>() {
                });
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize contentKey", e);
            }
        }
    }
}