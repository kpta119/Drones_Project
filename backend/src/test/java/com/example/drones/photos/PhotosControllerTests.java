package com.example.drones.photos;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.operators.exceptions.NoSuchPortfolioException;
import com.example.drones.photos.dto.PhotoDto;
import com.example.drones.photos.dto.PhotosDto;
import com.example.drones.photos.exceptions.PhotosUploadException;
import com.example.drones.photos.exceptions.PhotosUploadValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PhotosControllerTests {

    @Mock
    private PhotosService photosService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private PhotosController photosController;

    private UUID testUserId;
    private List<MultipartFile> mockImages;
    private List<String> photoNames;
    private PhotosDto photosDto;

    @BeforeEach
    public void setUp() {
        testUserId = UUID.randomUUID();

        MultipartFile mockImage1 = mock(MultipartFile.class);
        MultipartFile mockImage2 = mock(MultipartFile.class);
        mockImages = List.of(mockImage1, mockImage2);

        photoNames = List.of("photo1.jpg", "photo2.jpg");

        photosDto = buildPhotosDto();
    }

    private PhotosDto buildPhotosDto() {
        PhotoDto photo1 = PhotoDto.builder()
                .id(1)
                .name("photo1.jpg")
                .url("https://storage.googleapis.com/bucket/photo1.jpg")
                .build();

        PhotoDto photo2 = PhotoDto.builder()
                .id(2)
                .name("photo2.jpg")
                .url("https://storage.googleapis.com/bucket/photo2.jpg")
                .build();

        return PhotosDto.builder()
                .photos(List.of(photo1, photo2))
                .build();
    }

    @Test
    public void givenValidImagesAndNames_whenAddPhotos_thenReturnsCreatedWithPhotos() {
        when(jwtService.extractUserId()).thenReturn(testUserId);
        when(photosService.addPhotos(eq(testUserId), eq(mockImages), eq(photoNames)))
                .thenReturn(photosDto);

        ResponseEntity<PhotosDto> response = photosController.addPhotos(mockImages, photoNames);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(photosDto);
        assertThat(response.getBody().photos()).hasSize(2);
        assertThat(response.getBody().photos().get(0).name()).isEqualTo("photo1.jpg");
        assertThat(response.getBody().photos().get(1).name()).isEqualTo("photo2.jpg");

        verify(jwtService).extractUserId();
        verify(photosService).addPhotos(eq(testUserId), eq(mockImages), eq(photoNames));
    }

    @Test
    public void givenMismatchedImagesAndNames_whenAddPhotos_thenThrowsPhotosUploadValidationException() {
        List<String> mismatchedNames = List.of("photo1.jpg"); // Only one name for two images

        when(jwtService.extractUserId()).thenReturn(testUserId);
        when(photosService.addPhotos(eq(testUserId), eq(mockImages), eq(mismatchedNames)))
                .thenThrow(new PhotosUploadValidationException());

        assertThatThrownBy(() -> photosController.addPhotos(mockImages, mismatchedNames))
                .isInstanceOf(PhotosUploadValidationException.class)
                .hasMessageContaining("Number of images and names must be equal");

        verify(jwtService).extractUserId();
        verify(photosService).addPhotos(eq(testUserId), eq(mockImages), eq(mismatchedNames));
    }

    @Test
    public void givenPortfolioNotFound_whenAddPhotos_thenThrowsNoSuchPortfolioException() {
        when(jwtService.extractUserId()).thenReturn(testUserId);
        when(photosService.addPhotos(eq(testUserId), eq(mockImages), eq(photoNames)))
                .thenThrow(new NoSuchPortfolioException());

        assertThatThrownBy(() -> photosController.addPhotos(mockImages, photoNames))
                .isInstanceOf(NoSuchPortfolioException.class)
                .hasMessageContaining("Operator portfolio not found");

        verify(jwtService).extractUserId();
        verify(photosService).addPhotos(eq(testUserId), eq(mockImages), eq(photoNames));
    }

    @Test
    public void givenUploadFailure_whenAddPhotos_thenThrowsPhotosUploadException() {
        when(jwtService.extractUserId()).thenReturn(testUserId);
        when(photosService.addPhotos(eq(testUserId), eq(mockImages), eq(photoNames)))
                .thenThrow(new PhotosUploadException("Failed to upload to cloud storage"));

        assertThatThrownBy(() -> photosController.addPhotos(mockImages, photoNames))
                .isInstanceOf(PhotosUploadException.class)
                .hasMessageContaining("Photos upload failed")
                .hasMessageContaining("Failed to upload to cloud storage");

        verify(jwtService).extractUserId();
        verify(photosService).addPhotos(eq(testUserId), eq(mockImages), eq(photoNames));
    }

    @Test
    public void givenEmptyLists_whenAddPhotos_thenReturnsCreatedWithEmptyPhotos() {
        List<MultipartFile> emptyImages = List.of();
        List<String> emptyNames = List.of();

        PhotosDto emptyPhotosDto = PhotosDto.builder()
                .photos(List.of())
                .build();

        when(jwtService.extractUserId()).thenReturn(testUserId);
        when(photosService.addPhotos(eq(testUserId), eq(emptyImages), eq(emptyNames)))
                .thenReturn(emptyPhotosDto);

        ResponseEntity<PhotosDto> response = photosController.addPhotos(emptyImages, emptyNames);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().photos()).isEmpty();

        verify(jwtService).extractUserId();
        verify(photosService).addPhotos(eq(testUserId), eq(emptyImages), eq(emptyNames));
    }
}
