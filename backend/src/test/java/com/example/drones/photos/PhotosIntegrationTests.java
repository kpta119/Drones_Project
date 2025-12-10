package com.example.drones.photos;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.operators.PortfolioEntity;
import com.example.drones.operators.PortfolioRepository;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRepository;
import com.example.drones.user.UserRole;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class PhotosIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PhotosRepository photosRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String jwtToken;
    private PortfolioEntity testPortfolio;

    @BeforeEach
    void setUp() {
        UserEntity testOperator = UserEntity.builder()
                .displayName("testOperator")
                .name("Test")
                .surname("Operator")
                .email("operator@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("123456789")
                .role(UserRole.OPERATOR)
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .build();
        testOperator = userRepository.save(testOperator);

        testPortfolio = PortfolioEntity.builder()
                .operator(testOperator)
                .operatorId(testOperator.getId())
                .title("My Portfolio")
                .description("Test portfolio description")
                .build();
        testPortfolio = portfolioRepository.save(testPortfolio);

        jwtToken = jwtService.generateToken(testOperator.getId());
    }

    @Test
    void givenValidPhotosAndNames_whenAddPhotos_thenReturnsCreatedAndPersistsToDatabase() throws Exception {
        MockMultipartFile image1 = new MockMultipartFile(
                "images",
                "photo1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "photo1 content".getBytes()
        );

        MockMultipartFile image2 = new MockMultipartFile(
                "images",
                "photo2.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "photo2 content".getBytes()
        );

        mockMvc.perform(multipart("/api/photos/addPortfolioPhotos")
                        .file(image1)
                        .file(image2)
                        .param("names", "Sunset Photo", "Landscape Photo")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photos", hasSize(2)))
                .andExpect(jsonPath("$.photos[0].name").value("Sunset Photo"))
                .andExpect(jsonPath("$.photos[0].url").exists())
                .andExpect(jsonPath("$.photos[1].name").value("Landscape Photo"))
                .andExpect(jsonPath("$.photos[1].url").exists());

        List<PhotoEntity> savedPhotos = photosRepository.findAll();
        assertThat(savedPhotos).hasSize(2);
        assertThat(savedPhotos.getFirst().getName()).isEqualTo("Sunset Photo");
        assertThat(savedPhotos.getFirst().getUrl()).contains("fakeStorage.com");
        assertThat(savedPhotos.getFirst().getPortfolio().getId()).isEqualTo(testPortfolio.getId());
        assertThat(savedPhotos.get(1).getName()).isEqualTo("Landscape Photo");
        assertThat(savedPhotos.get(1).getUrl()).contains("fakeStorage.com");
        assertThat(savedPhotos.get(1).getPortfolio().getId()).isEqualTo(testPortfolio.getId());
    }

    @Test
    void givenMultiplePhotosWithExistingPhotos_whenAddPhotos_thenReturnsAllPhotos() throws Exception {
        PhotoEntity existingPhoto = PhotoEntity.builder()
                .name("Existing Photo")
                .url("https://fakeStorage.com/existing.jpg")
                .portfolio(testPortfolio)
                .build();
        photosRepository.save(existingPhoto);

        MockMultipartFile newImage = new MockMultipartFile(
                "images",
                "new.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "new photo content".getBytes()
        );

        mockMvc.perform(multipart("/api/photos/addPortfolioPhotos")
                        .file(newImage)
                        .param("names", "New Photo")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photos", hasSize(2)))
                .andExpect(jsonPath("$.photos[0].name").value("Existing Photo"))
                .andExpect(jsonPath("$.photos[1].name").value("New Photo"));

        List<PhotoEntity> allPhotos = photosRepository.findAll();
        assertThat(allPhotos).hasSize(2);
    }

    @Test
    void givenNoAuthToken_whenAddPhotos_thenReturnsForbidden() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "images",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "photo content".getBytes()
        );

        mockMvc.perform(multipart("/api/photos/addPortfolioPhotos")
                        .file(image)
                        .param("names", "Photo Name"))
                .andExpect(status().isForbidden());

        List<PhotoEntity> savedPhotos = photosRepository.findAll();
        assertThat(savedPhotos).isEmpty();
    }

    @Test
    void givenInvalidToken_whenAddPhotos_thenReturnsUnauthorized() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "images",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "photo content".getBytes()
        );

        mockMvc.perform(multipart("/api/photos/addPortfolioPhotos")
                        .file(image)
                        .param("names", "Photo Name")
                        .header("X-USER-TOKEN", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());

        List<PhotoEntity> savedPhotos = photosRepository.findAll();
        assertThat(savedPhotos).isEmpty();
    }

    @Test
    void givenTwoUploadRequests_whenAddPhotos_thenSecondResponseContainsAllPhotos() throws Exception {
        MockMultipartFile firstImage1 = new MockMultipartFile(
                "images",
                "first1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "first photo 1 content".getBytes()
        );

        MockMultipartFile firstImage2 = new MockMultipartFile(
                "images",
                "first2.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "first photo 2 content".getBytes()
        );

        mockMvc.perform(multipart("/api/photos/addPortfolioPhotos")
                        .file(firstImage1)
                        .file(firstImage2)
                        .param("names", "First Photo 1", "First Photo 2")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photos", hasSize(2)))
                .andExpect(jsonPath("$.photos[0].name").value("First Photo 1"))
                .andExpect(jsonPath("$.photos[1].name").value("First Photo 2"));

        MockMultipartFile secondImage1 = new MockMultipartFile(
                "images",
                "second1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "second photo 1 content".getBytes()
        );

        MockMultipartFile secondImage2 = new MockMultipartFile(
                "images",
                "second2.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "second photo 2 content".getBytes()
        );

        MockMultipartFile secondImage3 = new MockMultipartFile(
                "images",
                "second3.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "second photo 3 content".getBytes()
        );
        mockMvc.perform(multipart("/api/photos/addPortfolioPhotos")
                        .file(secondImage1)
                        .file(secondImage2)
                        .file(secondImage3)
                        .param("names", "Second Photo 1", "Second Photo 2", "Second Photo 3")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photos", hasSize(5)))
                .andExpect(jsonPath("$.photos[0].name").value("First Photo 1"))
                .andExpect(jsonPath("$.photos[1].name").value("First Photo 2"))
                .andExpect(jsonPath("$.photos[2].name").value("Second Photo 1"))
                .andExpect(jsonPath("$.photos[3].name").value("Second Photo 2"))
                .andExpect(jsonPath("$.photos[4].name").value("Second Photo 3"));

        List<PhotoEntity> allPhotos = photosRepository.findAll();
        assertThat(allPhotos).hasSize(5);
        assertThat(allPhotos.get(0).getName()).isEqualTo("First Photo 1");
        assertThat(allPhotos.get(1).getName()).isEqualTo("First Photo 2");
        assertThat(allPhotos.get(2).getName()).isEqualTo("Second Photo 1");
        assertThat(allPhotos.get(3).getName()).isEqualTo("Second Photo 2");
        assertThat(allPhotos.get(4).getName()).isEqualTo("Second Photo 3");

        assertThat(allPhotos).allMatch(photo -> photo.getPortfolio().getId().equals(testPortfolio.getId()));
    }

    @Test
    void givenValidPhotoIds_whenDeletePhotos_thenReturnsNoContentAndDeletesPhotos() throws Exception {
        PhotoEntity photo1 = PhotoEntity.builder()
                .name("Photo 1")
                .url("https://fakeStorage.com/photo1.jpg")
                .portfolio(testPortfolio)
                .build();
        PhotoEntity photo2 = PhotoEntity.builder()
                .name("Photo 2")
                .url("https://fakeStorage.com/photo2.jpg")
                .portfolio(testPortfolio)
                .build();
        PhotoEntity photo3 = PhotoEntity.builder()
                .name("Photo 3")
                .url("https://fakeStorage.com/photo3.jpg")
                .portfolio(testPortfolio)
                .build();
        photo1 = photosRepository.save(photo1);
        photo2 = photosRepository.save(photo2);
        photo3 = photosRepository.save(photo3);

        List<Integer> photoIdsToDelete = List.of(photo1.getId(), photo3.getId());

        mockMvc.perform(delete("/api/photos/deletePhotos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(photoIdsToDelete.toString())
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        List<PhotoEntity> remainingPhotos = photosRepository.findAll();
        assertThat(remainingPhotos).hasSize(1);
        assertThat(remainingPhotos.getFirst().getId()).isEqualTo(photo2.getId());
        assertThat(remainingPhotos.getFirst().getName()).isEqualTo("Photo 2");
    }


    @Test
    void givenAllPhotoIds_whenDeletePhotos_thenDeletesAllPhotos() throws Exception {
        PhotoEntity photo1 = PhotoEntity.builder()
                .name("Photo 1")
                .url("https://fakeStorage.com/photo1.jpg")
                .portfolio(testPortfolio)
                .build();
        PhotoEntity photo2 = PhotoEntity.builder()
                .name("Photo 2")
                .url("https://fakeStorage.com/photo2.jpg")
                .portfolio(testPortfolio)
                .build();
        photo1 = photosRepository.save(photo1);
        photo2 = photosRepository.save(photo2);
        List<Integer> photoIdsToDelete = List.of(photo1.getId(), photo2.getId());

        mockMvc.perform(delete("/api/photos/deletePhotos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(photoIdsToDelete.toString())
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        List<PhotoEntity> remainingPhotos = photosRepository.findAll();
        assertThat(remainingPhotos).isEmpty();
    }

    @Test
    void givenNoAuthToken_whenDeletePhotos_thenReturnsForbidden() throws Exception {
        PhotoEntity photo = PhotoEntity.builder()
                .name("Photo 1")
                .url("https://fakeStorage.com/photo1.jpg")
                .portfolio(testPortfolio)
                .build();
        photo = photosRepository.save(photo);

        mockMvc.perform(delete("/api/photos/deletePhotos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + photo.getId() + "]"))
                .andExpect(status().isForbidden());

        List<PhotoEntity> remainingPhotos = photosRepository.findAll();
        assertThat(remainingPhotos).hasSize(1);
    }

    @Test
    void givenInvalidToken_whenDeletePhotos_thenReturnsUnauthorized() throws Exception {
        PhotoEntity photo = PhotoEntity.builder()
                .name("Photo 1")
                .url("https://fakeStorage.com/photo1.jpg")
                .portfolio(testPortfolio)
                .build();
        photo = photosRepository.save(photo);

        mockMvc.perform(delete("/api/photos/deletePhotos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + photo.getId() + "]")
                        .header("X-USER-TOKEN", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());

        List<PhotoEntity> remainingPhotos = photosRepository.findAll();
        assertThat(remainingPhotos).hasSize(1);
    }

    @Test
    void givenOtherUserPhotos_whenDeletePhotos_thenDoesNotDeleteThosePhotos() throws Exception {
        UserEntity otherOperator = UserEntity.builder()
                .displayName("otherOperator")
                .name("Other")
                .surname("Operator")
                .email("other@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("987654321")
                .role(UserRole.OPERATOR)
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .build();
        otherOperator = userRepository.save(otherOperator);

        PortfolioEntity otherPortfolio = PortfolioEntity.builder()
                .operator(otherOperator)
                .operatorId(otherOperator.getId())
                .title("Other Portfolio")
                .description("Other portfolio description")
                .build();
        otherPortfolio = portfolioRepository.save(otherPortfolio);

        PhotoEntity myPhoto = PhotoEntity.builder()
                .name("My Photo")
                .url("https://fakeStorage.com/my.jpg")
                .portfolio(testPortfolio)
                .build();
        PhotoEntity otherPhoto = PhotoEntity.builder()
                .name("Other Photo")
                .url("https://fakeStorage.com/other.jpg")
                .portfolio(otherPortfolio)
                .build();
        PhotoEntity savedMyPhoto = photosRepository.save(myPhoto);
        PhotoEntity savedOtherPhoto = photosRepository.save(otherPhoto);

        mockMvc.perform(delete("/api/photos/deletePhotos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + savedOtherPhoto.getId() + "]")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        List<PhotoEntity> allPhotos = photosRepository.findAll();
        assertThat(allPhotos).hasSize(2);
        assertThat(allPhotos).anyMatch(p -> p.getId().equals(savedMyPhoto.getId()));
        assertThat(allPhotos).anyMatch(p -> p.getId().equals(savedOtherPhoto.getId()));
    }

    @Test
    void givenMixedPhotoIds_whenDeletePhotos_thenDeletesOnlyOwnPhotos() throws Exception {
        UserEntity otherOperator = UserEntity.builder()
                .displayName("anotherOperator")
                .name("Another")
                .surname("Operator")
                .email("another@test.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("987654321")
                .role(UserRole.OPERATOR)
                .coordinates("52.2297,21.0122")
                .radius(50)
                .certificates(List.of("UAV License"))
                .build();
        otherOperator = userRepository.save(otherOperator);

        PortfolioEntity otherPortfolio = PortfolioEntity.builder()
                .operator(otherOperator)
                .operatorId(otherOperator.getId())
                .title("Another Portfolio")
                .description("Another portfolio description")
                .build();
        otherPortfolio = portfolioRepository.save(otherPortfolio);

        PhotoEntity myPhoto1 = PhotoEntity.builder()
                .name("My Photo 1")
                .url("https://fakeStorage.com/my1.jpg")
                .portfolio(testPortfolio)
                .build();
        PhotoEntity myPhoto2 = PhotoEntity.builder()
                .name("My Photo 2")
                .url("https://fakeStorage.com/my2.jpg")
                .portfolio(testPortfolio)
                .build();
        PhotoEntity otherPhoto = PhotoEntity.builder()
                .name("Other Photo")
                .url("https://fakeStorage.com/other.jpg")
                .portfolio(otherPortfolio)
                .build();
        PhotoEntity savedMyPhoto1 = photosRepository.save(myPhoto1);
        PhotoEntity savedMyPhoto2 = photosRepository.save(myPhoto2);
        PhotoEntity savedOtherPhoto = photosRepository.save(otherPhoto);
        List<Integer> photoIdsToDelete = List.of(savedMyPhoto1.getId(), savedOtherPhoto.getId());

        mockMvc.perform(delete("/api/photos/deletePhotos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(photoIdsToDelete.toString())
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        List<PhotoEntity> allPhotos = photosRepository.findAll();
        assertThat(allPhotos).hasSize(2);
        assertThat(allPhotos).anyMatch(p -> p.getId().equals(savedMyPhoto2.getId()));
        assertThat(allPhotos).anyMatch(p -> p.getId().equals(savedOtherPhoto.getId()));
        assertThat(allPhotos).noneMatch(p -> p.getId().equals(savedMyPhoto1.getId()));
    }

    @Test
    void givenEmptyPhotoIdsList_whenDeletePhotos_thenDoesNotDeleteAnyPhotos() throws Exception {
        PhotoEntity photo = PhotoEntity.builder()
                .name("Photo 1")
                .url("https://fakeStorage.com/photo1.jpg")
                .portfolio(testPortfolio)
                .build();
        photosRepository.save(photo);

        mockMvc.perform(delete("/api/photos/deletePhotos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        List<PhotoEntity> remainingPhotos = photosRepository.findAll();
        assertThat(remainingPhotos).hasSize(1);
    }

    @Test
    void givenNonExistentPhotoIds_whenDeletePhotos_thenReturnsNoContentWithoutError() throws Exception {
        PhotoEntity photo = PhotoEntity.builder()
                .name("Photo 1")
                .url("https://fakeStorage.com/photo1.jpg")
                .portfolio(testPortfolio)
                .build();
        photosRepository.save(photo);

        mockMvc.perform(delete("/api/photos/deletePhotos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[99999, 88888]")
                        .header("X-USER-TOKEN", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        List<PhotoEntity> remainingPhotos = photosRepository.findAll();
        assertThat(remainingPhotos).hasSize(1);
    }


}
