package com.example.course_service.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LessonEntityTest {

    @Test
    void contentItemListConverter_ConvertAndUnconvert_RoundTrip() {
        LessonEntity.ContentItemListConverter converter = new LessonEntity.ContentItemListConverter();

        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setType("VIDEO");
        item.setKey("videos/test.mp4");
        item.setStatus("READY");
        item.setLabel("Intro");
        item.setOrder(1);

        String json = converter.convert(List.of(item));
        assertNotNull(json);
        assertTrue(json.contains("VIDEO"));

        List<LessonEntity.ContentItem> result = converter.unconvert(json);
        assertEquals(1, result.size());
        assertEquals("VIDEO", result.get(0).getType());
        assertEquals("videos/test.mp4", result.get(0).getKey());
    }

    @Test
    void contentItemListConverter_Convert_EmptyList() {
        LessonEntity.ContentItemListConverter converter = new LessonEntity.ContentItemListConverter();
        String json = converter.convert(List.of());
        assertEquals("[]", json);
    }

    @Test
    void contentItemListConverter_Unconvert_InvalidJson_ThrowsException() {
        LessonEntity.ContentItemListConverter converter = new LessonEntity.ContentItemListConverter();
        assertThrows(IllegalStateException.class, () -> converter.unconvert("invalid-json"));
    }

    @Test
    void contentItem_AllArgsConstructor_SetsFields() {
        LessonEntity.ContentItem item = new LessonEntity.ContentItem(
                "VIDEO", "key/path", "processed/key", "READY", "Intro", 1);

        assertEquals("VIDEO", item.getType());
        assertEquals("key/path", item.getKey());
        assertEquals("processed/key", item.getProcessedKey());
        assertEquals("READY", item.getStatus());
        assertEquals("Intro", item.getLabel());
        assertEquals(1, item.getOrder());
    }

    @Test
    void lessonEntity_SettersAndGetters_WorkCorrectly() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("L1");
        lesson.setModuleId("M1");
        lesson.setTitle("Test Lesson");
        lesson.setDescription("Description");
        lesson.setDuration(30);
        lesson.setOrderIndex(1);
        lesson.setLessonType("RECORDED");
        lesson.setLiveSessionId("session-1");
        lesson.setVideoKey("videos/test.mp4");

        assertEquals("L1", lesson.getLessonId());
        assertEquals("M1", lesson.getModuleId());
        assertEquals("Test Lesson", lesson.getTitle());
        assertEquals(30, lesson.getDuration());
        assertEquals("RECORDED", lesson.getLessonType());
        assertEquals("session-1", lesson.getLiveSessionId());
        assertEquals("videos/test.mp4", lesson.getVideoKey());
    }
}
