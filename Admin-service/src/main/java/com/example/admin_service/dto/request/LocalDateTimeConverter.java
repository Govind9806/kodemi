package com.example.admin_service.dto.request;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

import java.time.LocalDateTime;

public class LocalDateTimeConverter implements DynamoDBTypeConverter<String, LocalDateTime> {

    @Override
    public String convert(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : null;
    }

    @Override
    public LocalDateTime unconvert(String value) {
        // Convert String -> LocalDateTime
        return value != null ? LocalDateTime.parse(value) : null;
    }
}

