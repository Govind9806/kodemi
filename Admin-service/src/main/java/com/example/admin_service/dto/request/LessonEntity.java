package com.example.admin_service.dto.request;

import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
}
