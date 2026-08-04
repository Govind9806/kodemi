package com.example.user_service.model;

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

