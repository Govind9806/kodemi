package com.example.enrollment_progress_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.enrollment_progress_service.feign.CourseClient;
import com.example.enrollment_progress_service.feign.LiveClassClient;
import com.example.enrollment_progress_service.feign.PaymentClient;

@SpringBootTest(properties = {
		"SERVER_PORT=8080",
		"REDIS_HOST=localhost",
		"REDIS_PORT=6379",
		"EUREKA_URL=http://localhost:8761/eureka",
		"JWT_SECRET=01234567890123456789012345678901234567890123456789",
		"AWS_ACCESS_KEY_ID=dummy",
		"AWS_SECRET_ACCESS_KEY=dummy",
		"AWS_REGION=us-east-1",
		"DYNAMODB_REGION=us-east-1",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false"
})
class EnrollmentProgressServiceApplicationTests {

	@MockBean
	private AmazonDynamoDB amazonDynamoDB;

	@MockBean
	private DynamoDBMapper dynamoDBMapper;

	@MockBean
	private RedisConnectionFactory redisConnectionFactory;

	@MockBean
	private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

	@MockBean
	private RedisTemplate<String, Object> redisTemplate;

	@MockBean
	private StringRedisTemplate stringRedisTemplate;

	@MockBean
	private CourseClient courseClient;

	@MockBean
	private LiveClassClient liveClassClient;

	@MockBean
	private PaymentClient paymentClient;

	@Test
	void contextLoads() {
	}
}
