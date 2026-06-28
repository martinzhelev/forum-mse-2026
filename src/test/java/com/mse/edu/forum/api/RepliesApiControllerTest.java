package com.mse.edu.forum.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mse.edu.forum.domain.PostEntity;
import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.maintenance.RestoreMaintenanceState;
import com.mse.edu.forum.repo.PostRepository;
import com.mse.edu.forum.repo.ReplyRepository;
import com.mse.edu.forum.repo.UserRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RepliesApiControllerTest {

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
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private ReplyRepository replyRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RestoreMaintenanceState restoreMaintenanceState;

	private long postId;

	@BeforeEach
	void setUp() {
		restoreMaintenanceState.finishRestore();
		replyRepository.deleteAll();
		postRepository.deleteAll();

		UserEntity admin = userRepository
				.findByUsername("admin")
				.orElseThrow(() -> new IllegalStateException("Admin user missing"));
		PostEntity post = new PostEntity();
		post.setTitle("Reply target");
		post.setContent("A post that accepts replies");
		post.setUser(admin);
		postId = postRepository.save(post).getId();
	}

	@Test
	void createListAndGetReply() throws Exception {
		String token = loginAndGetToken();

		MvcResult createResult = mockMvc.perform(post("/posts/{postId}/replies", postId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "  First reply  "
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.postId").value(postId))
				.andExpect(jsonPath("$.content").value("First reply"))
				.andExpect(jsonPath("$.createdAt").exists())
				.andReturn();

		long replyId = extractLongField(createResult.getResponse().getContentAsString(), "id");

		mockMvc.perform(get("/posts/{postId}/replies", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(replyId))
				.andExpect(jsonPath("$[0].postId").value(postId));

		mockMvc.perform(get("/replies/{id}", replyId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(replyId))
				.andExpect(jsonPath("$.postId").value(postId));
	}

	@Test
	void createReplyRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/posts/{postId}/replies", postId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "No token"}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void replyEndpointsReturn404ForMissingResources() throws Exception {
		String token = loginAndGetToken();

		mockMvc.perform(get("/posts/{postId}/replies", 999999L))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/replies/{id}", 999999L))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/posts/{postId}/replies", 999999L)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "Missing post"}
								"""))
				.andExpect(status().isNotFound());
	}

	private String loginAndGetToken() throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "admin",
								  "password": "admin"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		return extractStringField(loginResult.getResponse().getContentAsString(), "accessToken");
	}

	private long extractLongField(String json, String fieldName) {
		Matcher matcher = Pattern.compile("\"" + fieldName + "\":(\\d+)").matcher(json);
		if (!matcher.find()) {
			throw new IllegalStateException("Field not found: " + fieldName + " in " + json);
		}
		return Long.parseLong(matcher.group(1));
	}

	private String extractStringField(String json, String fieldName) {
		Matcher matcher = Pattern.compile("\"" + fieldName + "\":\"([^\"]+)\"").matcher(json);
		if (!matcher.find()) {
			throw new IllegalStateException("Field not found: " + fieldName + " in " + json);
		}
		return matcher.group(1);
	}
}
