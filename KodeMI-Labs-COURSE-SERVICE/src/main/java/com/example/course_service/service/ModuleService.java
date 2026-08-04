package com.example.course_service.service;

import com.example.course_service.dto.response.ModuleResponseDTO;
import com.example.course_service.model.ModuleEntity;

import java.util.List;

import java.util.Map;

public interface ModuleService {
    Map<String, Object> createModule(ModuleEntity module, String userId);
    ModuleResponseDTO getModuleById(String moduleId);
    List<ModuleResponseDTO> getAllModules();
    String updateModule(String userId, String moduleId, ModuleEntity module);
    List<ModuleResponseDTO> getModulesByCourse(String courseId);
}
