package com.example.drones.photos;

import com.example.drones.operators.PortfolioEntity;
import com.example.drones.operators.PortfolioRepository;
import com.example.drones.operators.exceptions.NoSuchPortfolioException;
import com.example.drones.photos.dto.PhotoDto;
import com.example.drones.photos.dto.PhotosDto;
import com.example.drones.photos.exceptions.PhotosUploadException;
import com.example.drones.photos.exceptions.PhotosUploadValidationException;
import com.example.drones.photos.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PhotosServiceTests {

    @Mock
    private FileStorage fileStorage;

    @Mock
    private PhotosRepository photosRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PhotosMapper photosMapper;

    @InjectMocks
    private PhotosService photosService;

    @Captor
    private ArgumentCaptor<List<PhotoEntity>> photoCaptor;

    @Test
    public void givenValidImagesAndNames_whenAddPhotos_thenPhotosUploadedAndSaved() throws Exception {
        UUID userId = UUID.randomUUID();
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        List<MultipartFile> images = List.of(file1, file2);
        List<String> names = List.of("Photo 1", "Photo 2");
        List<String> urls = List.of("https://storage.example.com/photo1.jpg", "https://storage.example.com/photo2.jpg");

        PortfolioEntity portfolio = PortfolioEntity.builder()
                .id(1)
                .operatorId(userId)
                .build();

        PhotoEntity photo1 = PhotoEntity.builder()
                .id(1)
                .name("Photo 1")
                .url(urls.get(0))
                .portfolio(portfolio)
                .build();

        PhotoEntity photo2 = PhotoEntity.builder()
                .id(2)
                .name("Photo 2")
                .url(urls.get(1))
                .portfolio(portfolio)
                .build();

        List<PhotoEntity> allPhotos = List.of(photo1, photo2);

        PhotoDto photoDto1 = PhotoDto.builder()
                .id(1)
                .name("Photo 1")
                .url(urls.get(0))
                .build();

        PhotoDto photoDto2 = PhotoDto.builder()
                .id(2)
                .name("Photo 2")
                .url(urls.get(1))
                .build();

        List<PhotoDto> photoDtos = List.of(photoDto1, photoDto2);

        when(fileStorage.uploadFiles(images, userId.toString())).thenReturn(urls);
        when(portfolioRepository.findByOperatorId(userId)).thenReturn(Optional.of(portfolio));
        when(photosRepository.findAllByPortfolio(portfolio)).thenReturn(allPhotos);
        when(photosMapper.toDto(allPhotos)).thenReturn(photoDtos);

        PhotosDto result = photosService.addPhotos(userId, images, names);

        verify(fileStorage).uploadFiles(images, userId.toString());
        verify(portfolioRepository).findByOperatorId(userId);

        verify(photosRepository).saveAll(photoCaptor.capture());

        List<PhotoEntity> savedPhotos = photoCaptor.getValue();
        assertThat(savedPhotos).hasSize(2);
        assertThat(savedPhotos.getFirst().getName()).isEqualTo("Photo 1");
        assertThat(savedPhotos.get(0).getUrl()).isEqualTo(urls.getFirst());
        assertThat(savedPhotos.get(0).getPortfolio()).isEqualTo(portfolio);
        assertThat(savedPhotos.get(1).getName()).isEqualTo("Photo 2");
        assertThat(savedPhotos.get(1).getUrl()).isEqualTo(urls.get(1));
        assertThat(savedPhotos.get(1).getPortfolio()).isEqualTo(portfolio);

        verify(photosRepository).findAllByPortfolio(portfolio);
        verify(photosMapper).toDto(allPhotos);

        assertThat(result).isNotNull();
        assertThat(result.photos()).hasSize(2);
        assertThat(result.photos()).isEqualTo(photoDtos);
    }

    @Test
    public void givenMismatchedImagesAndNames_whenAddPhotos_thenThrowsPhotosUploadValidationException() throws Exception {
        UUID userId = UUID.randomUUID();
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        List<MultipartFile> images = List.of(file1, file2);
        List<String> names = List.of("Photo 1"); // Only one name for two images

        assertThatThrownBy(() -> photosService.addPhotos(userId, images, names))
                .isInstanceOf(PhotosUploadValidationException.class)
                .hasMessageContaining("Number of images and names must be equal");

        verify(fileStorage, never()).uploadFiles(anyList(), anyString());
        verify(portfolioRepository, never()).findByOperatorId(any());
        verify(photosRepository, never()).saveAll(anyList());
    }

    @Test
    public void givenEmptyImagesAndNames_whenAddPhotos_thenReturnsEmptyPhotosDto() throws Exception {
        UUID userId = UUID.randomUUID();
        List<MultipartFile> images = List.of();
        List<String> names = List.of();

        PhotosDto result = photosService.addPhotos(userId, images, names);

        assertThat(result).isNotNull();
        assertThat(result.photos()).isEmpty();

        verify(fileStorage, never()).uploadFiles(anyList(), anyString());
        verify(portfolioRepository, never()).findByOperatorId(any());
        verify(photosRepository, never()).saveAll(anyList());
    }

    @Test
    public void givenNonExistentPortfolio_whenAddPhotos_thenThrowsNoSuchPortfolioException() throws Exception {
        UUID userId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> images = List.of(file);
        List<String> names = List.of("Photo");
        List<String> urls = List.of("https://storage.example.com/photo.jpg");

        when(fileStorage.uploadFiles(images, userId.toString())).thenReturn(urls);
        when(portfolioRepository.findByOperatorId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photosService.addPhotos(userId, images, names))
                .isInstanceOf(NoSuchPortfolioException.class)
                .hasMessageContaining("Operator portfolio not found");

        verify(fileStorage).uploadFiles(images, userId.toString());
        verify(portfolioRepository).findByOperatorId(userId);
        verify(photosRepository, never()).saveAll(anyList());
    }

    @Test
    public void givenfileStorageThrowsIOException_whenAddPhotos_thenThrowsPhotosUploadException() throws Exception {
        UUID userId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> images = List.of(file);
        List<String> names = List.of("Photo");

        when(fileStorage.uploadFiles(images, userId.toString()))
                .thenThrow(new IOException("Network error"));

        assertThatThrownBy(() -> photosService.addPhotos(userId, images, names))
                .isInstanceOf(PhotosUploadException.class)
                .hasMessageContaining("Photos upload failed")
                .hasMessageContaining("Network error");

        verify(fileStorage).uploadFiles(images, userId.toString());
        verify(portfolioRepository, never()).findByOperatorId(any());
        verify(photosRepository, never()).saveAll(anyList());
    }

    @Test
    public void givenMultipleExistingPhotos_whenAddPhotos_thenReturnsAllPhotosIncludingNew() throws Exception {
        UUID userId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> images = List.of(file);
        List<String> names = List.of("New Photo");
        List<String> urls = List.of("https://storage.example.com/new.jpg");

        PortfolioEntity portfolio = PortfolioEntity.builder()
                .id(1)
                .operatorId(userId)
                .build();

        PhotoEntity existingPhoto1 = PhotoEntity.builder()
                .id(1)
                .name("Old Photo 1")
                .url("https://storage.example.com/old1.jpg")
                .portfolio(portfolio)
                .build();

        PhotoEntity existingPhoto2 = PhotoEntity.builder()
                .id(2)
                .name("Old Photo 2")
                .url("https://storage.example.com/old2.jpg")
                .portfolio(portfolio)
                .build();

        PhotoEntity newPhoto = PhotoEntity.builder()
                .id(3)
                .name("New Photo")
                .url(urls.getFirst())
                .portfolio(portfolio)
                .build();

        List<PhotoEntity> allPhotos = List.of(existingPhoto1, existingPhoto2, newPhoto);

        PhotoDto dto1 = PhotoDto.builder().id(1).name("Old Photo 1").url("https://storage.example.com/old1.jpg").build();
        PhotoDto dto2 = PhotoDto.builder().id(2).name("Old Photo 2").url("https://storage.example.com/old2.jpg").build();
        PhotoDto dto3 = PhotoDto.builder().id(3).name("New Photo").url(urls.getFirst()).build();
        List<PhotoDto> photoDtos = List.of(dto1, dto2, dto3);

        when(fileStorage.uploadFiles(images, userId.toString())).thenReturn(urls);
        when(portfolioRepository.findByOperatorId(userId)).thenReturn(Optional.of(portfolio));
        when(photosRepository.findAllByPortfolio(portfolio)).thenReturn(allPhotos);
        when(photosMapper.toDto(allPhotos)).thenReturn(photoDtos);

        PhotosDto result = photosService.addPhotos(userId, images, names);

        verify(fileStorage).uploadFiles(images, userId.toString());
        verify(portfolioRepository).findByOperatorId(userId);
        verify(photosRepository).saveAll(anyList());
        verify(photosRepository).findAllByPortfolio(portfolio);
        verify(photosMapper).toDto(allPhotos);

        assertThat(result).isNotNull();
        assertThat(result.photos()).hasSize(3);
        assertThat(result.photos()).isEqualTo(photoDtos);
    }

    @Test
    public void givenMoreNamesThanImages_whenAddPhotos_thenThrowsPhotosUploadValidationException() throws Exception {
        UUID userId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> images = List.of(file);
        List<String> names = List.of("Photo 1", "Photo 2"); // Two names for one image

        assertThatThrownBy(() -> photosService.addPhotos(userId, images, names))
                .isInstanceOf(PhotosUploadValidationException.class)
                .hasMessageContaining("Number of images and names must be equal");

        verify(fileStorage, never()).uploadFiles(anyList(), anyString());
        verify(portfolioRepository, never()).findByOperatorId(any());
        verify(photosRepository, never()).saveAll(anyList());
    }

}
