        package com.example.kodemilabs.repository;

        import com.amazonaws.services.dynamodbv2.datamodeling.*;
        import com.amazonaws.services.dynamodbv2.model.AttributeValue;
        import com.example.kodemilabs.enums.Role;
        import com.example.kodemilabs.model.User;
        import org.springframework.stereotype.Repository;

        import java.util.*;

        @Repository
        public class UserRepo {

            private final DynamoDBMapper dynamoDBMapper;

            public UserRepo(DynamoDBMapper dynamoDBMapper) {
                this.dynamoDBMapper = dynamoDBMapper;
            }

            public void save(User user) {
                dynamoDBMapper.save(user);
            }

            // used by getUserById and findById both
            public User getUserById(String userId) {
                return dynamoDBMapper.load(User.class, userId);
            }

            // findById used in AuthController for trainer activate/reject/submitted
            // just wraps getUserById and throws if not found
            public Optional<User> findById(String userId) {
                User user = dynamoDBMapper.load(User.class, userId);
                return Optional.ofNullable(user);
            }

            public List<User> findByIds(List<String> userIds) {
                if (userIds == null || userIds.isEmpty()) {
                    return new java.util.ArrayList<>();
                }
                List<User> usersToGet = new java.util.ArrayList<>();
                for (String id : userIds) {
                    User u = new User();
                    u.setUserId(id);
                    usersToGet.add(u);
                }
                java.util.Map<String, List<Object>> results = dynamoDBMapper.batchLoad(usersToGet);
                List<User> users = new java.util.ArrayList<>();
                for (List<Object> list : results.values()) {
                    for (Object obj : list) {
                        if (obj instanceof User user) {
                            users.add(user);
                        }
                    }
                }
                return users;
            }

            public User getUserByEmail(String email) {
                Map<String, AttributeValue> values = new HashMap<>();
                values.put(":email", new AttributeValue().withS(email));

                DynamoDBQueryExpression<User> query =
                        new DynamoDBQueryExpression<User>()
                                .withIndexName("email-index")
                                .withConsistentRead(false)
                                .withKeyConditionExpression("email = :email")
                                .withExpressionAttributeValues(values);

                List<User> users = dynamoDBMapper.query(User.class, query);
                return users.isEmpty() ? null : users.get(0);
            }

            public String getFullNameByEmail(String email) {
                User user = getUserByEmail(email);
                return user != null ? user.getName() : null;
            }

            public String getUsernameByEmail(String email) {
                User user = getUserByEmail(email);
                return user != null ? user.getUsername() : null;
            }
            public List<User> getPendingTrainers() {

                Map<String, AttributeValue> eav = new HashMap<>();
                eav.put(":role", new AttributeValue().withS(Role.TRAINER.name()));
                eav.put(":status", new AttributeValue().withS("APPROVAL_PENDING"));

                Map<String, String> ean = new HashMap<>();
                ean.put("#role", "role");
                ean.put("#status", "status");

                DynamoDBQueryExpression<User> query = new DynamoDBQueryExpression<User>()
                        .withIndexName("role-status-index")
                        .withKeyConditionExpression("#role = :role AND #status = :status")
                        .withExpressionAttributeNames(ean)
                        .withExpressionAttributeValues(eav)
                        .withConsistentRead(false);

                return dynamoDBMapper.query(User.class, query);
            }
        }