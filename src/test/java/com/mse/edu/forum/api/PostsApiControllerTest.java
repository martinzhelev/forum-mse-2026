package com.mse.edu.forum.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mse.edu.forum.repo.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PostsApiControllerTest {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("forum")
			.withUsername("admin")
			.withPassword("admin");

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
		registry.add("app.jwt.secret", () -> "test-jwt-secret-at-least-32-characters");
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostRepository postRepository;

	@BeforeEach
	void setUp() {
		postRepository.deleteAll();
	}

	@Test
	void createPostAndGetPosts() throws Exception {
		String token = loginAndGetToken("admin", "admin");

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "My first post",
								  "content": "Hello from MockMvc + Testcontainers"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.title").value("My first post"))
				.andExpect(jsonPath("$.content").value("Hello from MockMvc + Testcontainers"))
				.andExpect(jsonPath("$.author.username").value("admin"))
				.andExpect(jsonPath("$.modifiedAt").exists())
				.andExpect(jsonPath("$.viewCount").value(0))
				.andExpect(jsonPath("$.createdAt").exists());

		mockMvc.perform(get("/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title", hasItem("My first post")))
				.andExpect(jsonPath("$[*].content", hasItem("Hello from MockMvc + Testcontainers")));
	}

	@Test
	void createPost_requiresAuthentication() throws Exception {
		mockMvc.perform(post("/posts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "No token",
								  "content": "Should fail"
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void getPostById_returnsCreatedPost() throws Exception {
		String token = loginAndGetToken("admin", "admin");

		MvcResult createResult = mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Single post",
								  "content": "For get by id"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		long id = extractLongField(createResult.getResponse().getContentAsString(), "id");

		mockMvc.perform(get("/posts/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.title").value("Single post"))
				.andExpect(jsonPath("$.content").value("For get by id"))
				.andExpect(jsonPath("$.viewCount").value(1));
	}

	@Test
	void createPost_rejectsDuplicateTitle() throws Exception {
		String token = loginAndGetToken("admin", "admin");
		String body = """
				{
				  "title": "Unique title",
				  "content": "First"
				}
				""";

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void registerCreatesRegularUser() throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "student_user",
								  "email": "student-user@example.com",
								  "password": "password123"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("student_user"))
				.andExpect(jsonPath("$.role").value("USER"));
	}

	@Test
	void getPostById_returns404WhenMissing() throws Exception {
		mockMvc.perform(get("/posts/{id}", 999999L))
				.andExpect(status().isNotFound());
	}

	@Test
	void listPosts_isPublic() throws Exception {
		String token = loginAndGetToken("admin", "admin");
		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Visible post",
								  "content": "Public read"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title", hasItem("Visible post")))
				.andExpect(jsonPath("$[*].title", not(hasItem("No token"))));
	}

	@Test
	void listPosts_returnsInsertionOrder() throws Exception {
		String token = loginAndGetToken("admin", "admin");

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Post A",
								  "content": "A"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Post B",
								  "content": "B"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Post A"))
				.andExpect(jsonPath("$[1].title").value("Post B"));
	}

	@Test
	void restoreMode_blocksRegularEndpointsWith503AndRetryAfter() throws Exception {
		String token = loginAndGetToken("admin", "admin");

		mockMvc.perform(post("/admin/maintenance/restore/start")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "estimatedDurationSeconds": 180
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.restoreInProgress").value(true))
				.andExpect(jsonPath("$.retryAfterSeconds", greaterThanOrEqualTo(1)));

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Blocked during restore",
								  "content": "Should return 503"
								}
								"""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.error").value("service_unavailable"));

		mockMvc.perform(post("/admin/maintenance/restore/finish")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.restoreInProgress").value(false));
	}

	@Test
	void restoreMode_keepsHealthEndpointAccessible() throws Exception {
		String token = loginAndGetToken("admin", "admin");

		mockMvc.perform(post("/admin/maintenance/restore/start")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));

		mockMvc.perform(post("/admin/maintenance/restore/finish")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void maintenanceEndpoints_requireAdminAuthentication() throws Exception {
		mockMvc.perform(post("/admin/maintenance/restore/start"))
				.andExpect(status().isForbidden());
	}

	@Test
	void restoreFinish_reopensEndpoints() throws Exception {
		String token = loginAndGetToken("admin", "admin");

		mockMvc.perform(post("/admin/maintenance/restore/start")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(post("/admin/maintenance/restore/finish")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "After restore",
								  "content": "Back online"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("After restore"));
	}

	private String loginAndGetToken(String username, String password) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "password": "%s"
								}
								""".formatted(username, password)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andReturn();

		String body = loginResult.getResponse().getContentAsString();
		String marker = "\"accessToken\":\"";
		int start = body.indexOf(marker);
		if (start < 0) {
			throw new IllegalStateException("accessToken not found in login response: " + body);
		}
		start += marker.length();
		int end = body.indexOf('"', start);
		if (end < 0) {
			throw new IllegalStateException("Invalid login response: " + body);
		}
		return body.substring(start, end);
	}

	private long extractLongField(String json, String fieldName) {
		Pattern pattern = Pattern.compile("\"" + fieldName + "\":(\\d+)");
		Matcher matcher = pattern.matcher(json);
		if (!matcher.find()) {
			throw new IllegalStateException("Field not found: " + fieldName + " in " + json);
		}
		return Long.parseLong(matcher.group(1));
	}
}
