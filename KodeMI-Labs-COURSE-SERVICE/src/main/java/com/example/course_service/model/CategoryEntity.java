package com.example.course_service.model;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter

@DynamoDBTable(tableName="Category")
public class CategoryEntity {
    private String categoryId;
    private String name;
    private String slug;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;
    private Map<String, List<String>> subCategories;

    @DynamoDBHashKey(attributeName = "categoryId")
    public String getCategoryId(){
        return categoryId;
    }

    @DynamoDBIndexHashKey(
            globalSecondaryIndexName = "name-index",
            attributeName = "name"
    )
    public String getName(){
        return name;
    }

    @DynamoDBAttribute(attributeName = "slug")
    public String getSlug(){
        return slug;
    }

    @DynamoDBAttribute(attributeName = "description")
    public String getDescription(){
        return description;
    }

    @DynamoDBAttribute(attributeName = "displayOrder")
    public Integer getDisplayOrder(){
        return displayOrder;
    }

    @DynamoDBAttribute(attributeName = "isActive")
    public Boolean getIsActive(){
        return isActive;
    }

    @DynamoDBAttribute(attributeName = "subCategories")
    public Map<String, List<String>> getSubCategories() {
        return subCategories;
    }

}
