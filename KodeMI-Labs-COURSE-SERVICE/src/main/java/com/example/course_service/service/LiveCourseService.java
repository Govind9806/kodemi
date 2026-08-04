package com.example.course_service.service;

import com.example.course_service.dto.request.AttachRecordingRequest;
import com.example.course_service.dto.request.LinkLiveSessionRequest;
import com.example.course_service.dto.response.LiveCourseDetailResponseDTO;

public interface LiveCourseService {

    /**
     * Link a live session to a LIVE course
     */
    String linkLiveSession(String courseId, LinkLiveSessionRequest request);

    /**
     * Attach recording to a live course after session ends
     */
    String attachRecording(AttachRecordingRequest request);

    /**
     * Get detailed info about a LIVE course with session details
     */
    LiveCourseDetailResponseDTO getLiveCourseDetail(String courseId);
}

