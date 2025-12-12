package com.example.drones.photos;

import com.example.drones.common.config.BucketConfiguration;
import com.example.drones.photos.storage.GcsService;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GcsServiceTests {

    @Mock
    private Storage storage;

    @Mock
    private BucketConfiguration bucketConfig;

    @InjectMocks
    private GcsService gcsService;

    private MultipartFile mockFile;
    private Blob mockBlob;
    private String testBucketName;
    private String testSubdirectory;
    private String testUserSubdirectory;

    @BeforeEach
    public void setUp() {
        testBucketName = "test-bucket";
        testSubdirectory = "photos";
        testUserSubdirectory = "user-123";

        mockFile = mock(MultipartFile.class);
        mockBlob = mock(Blob.class);

        lenient().when(bucketConfig.getBucketName()).thenReturn(testBucketName);
        lenient().when(bucketConfig.getSubdirectory()).thenReturn(testSubdirectory);
    }

    @Test
    public void givenValidFile_whenUploadFile_thenReturnsMediaLink() throws IOException {
        UUID fixedUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        String originalFilename = "test-image.jpg";
        String contentType = "image/jpeg";
        String expectedMediaLink = "https://storage.googleapis.com/test-bucket/photos/user-123/00000000-0000-0000-0000-000000000000-test-image.jpg";
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());

        when(mockFile.getOriginalFilename()).thenReturn(originalFilename);
        when(mockFile.getContentType()).thenReturn(contentType);
        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(storage.createFrom(any(BlobInfo.class), any(InputStream.class))).thenReturn(mockBlob);

        String result;
        try (MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
            uuidMock.when(UUID::randomUUID).thenReturn(fixedUuid);
            result = gcsService.uploadFile(mockFile, testUserSubdirectory);
        }

        assertThat(result).isEqualTo(expectedMediaLink);
        verify(storage).createFrom(any(BlobInfo.class), eq(inputStream));
        verify(mockFile).getOriginalFilename();
        verify(mockFile).getContentType();
        verify(mockFile).getInputStream();
    }

    @Test
    public void givenValidFile_whenUploadFile_thenCreatesBlobWithCorrectPath() throws IOException {
        String originalFilename = "photo.png";
        String contentType = "image/png";
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);

        when(mockFile.getOriginalFilename()).thenReturn(originalFilename);
        when(mockFile.getContentType()).thenReturn(contentType);
        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(storage.createFrom(any(BlobInfo.class), any(InputStream.class))).thenReturn(mockBlob);

        gcsService.uploadFile(mockFile, testUserSubdirectory);

        verify(storage).createFrom(blobInfoCaptor.capture(), any(InputStream.class));
        BlobInfo capturedBlobInfo = blobInfoCaptor.getValue();

        assertThat(capturedBlobInfo.getBucket()).isEqualTo(testBucketName);
        assertThat(capturedBlobInfo.getName()).startsWith(testSubdirectory + "/" + testUserSubdirectory + "/");
        assertThat(capturedBlobInfo.getName()).endsWith(originalFilename);
        assertThat(capturedBlobInfo.getContentType()).isEqualTo(contentType);
    }

    @Test
    public void givenFileWithIOException_whenUploadFile_thenThrowsIOException() throws IOException {
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream()).thenThrow(new IOException("Failed to read file"));

        assertThatThrownBy(() -> gcsService.uploadFile(mockFile, testUserSubdirectory))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to read file");

        verify(storage, never()).createFrom(any(BlobInfo.class), any(InputStream.class));
    }

    @Test
    public void givenStorageException_whenUploadFile_thenThrowsException() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());

        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(storage.createFrom(any(BlobInfo.class), any(InputStream.class)))
                .thenThrow(new RuntimeException("Storage service unavailable"));

        assertThatThrownBy(() -> gcsService.uploadFile(mockFile, testUserSubdirectory))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Storage service unavailable");
    }

    @Test
    public void givenMultipleFiles_whenUploadFiles_thenReturnsAllMediaLinks() throws IOException {
        UUID fixedUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        MultipartFile file3 = mock(MultipartFile.class);

        InputStream inputStream1 = new ByteArrayInputStream("content1".getBytes());
        InputStream inputStream2 = new ByteArrayInputStream("content2".getBytes());
        InputStream inputStream3 = new ByteArrayInputStream("content3".getBytes());

        when(file1.getOriginalFilename()).thenReturn("image1.jpg");
        when(file1.getContentType()).thenReturn("image/jpeg");
        when(file1.getInputStream()).thenReturn(inputStream1);

        when(file2.getOriginalFilename()).thenReturn("image2.png");
        when(file2.getContentType()).thenReturn("image/png");
        when(file2.getInputStream()).thenReturn(inputStream2);

        when(file3.getOriginalFilename()).thenReturn("image3.gif");
        when(file3.getContentType()).thenReturn("image/gif");
        when(file3.getInputStream()).thenReturn(inputStream3);

        Blob blob1 = mock(Blob.class);
        Blob blob2 = mock(Blob.class);
        Blob blob3 = mock(Blob.class);

        when(storage.createFrom(any(BlobInfo.class), any(InputStream.class)))
                .thenReturn(blob1, blob2, blob3);

        List<MultipartFile> files = List.of(file1, file2, file3);

        List<String> results;
        try (MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
            uuidMock.when(UUID::randomUUID).thenReturn(fixedUuid);
            results = gcsService.uploadFiles(files, testUserSubdirectory);
        }

        assertThat(results).hasSize(3);
        assertThat(results.get(0)).isEqualTo("https://storage.googleapis.com/test-bucket/photos/user-123/00000000-0000-0000-0000-000000000000-image1.jpg");
        assertThat(results.get(1)).isEqualTo("https://storage.googleapis.com/test-bucket/photos/user-123/00000000-0000-0000-0000-000000000000-image2.png");
        assertThat(results.get(2)).isEqualTo("https://storage.googleapis.com/test-bucket/photos/user-123/00000000-0000-0000-0000-000000000000-image3.gif");

        verify(storage, times(3)).createFrom(any(BlobInfo.class), any(InputStream.class));
    }

    @Test
    public void givenEmptyList_whenUploadFiles_thenReturnsEmptyList() throws IOException {
        List<MultipartFile> emptyFiles = List.of();

        List<String> result = gcsService.uploadFiles(emptyFiles, testUserSubdirectory);

        assertThat(result).isEmpty();
        verify(storage, never()).createFrom(any(BlobInfo.class), any(InputStream.class));
    }

    @Test
    public void givenValidUrl_whenDeleteFile_thenDeletesBlobFromStorage() {
        String fileUrl = "https://storage.googleapis.com/test-bucket/photos/user-123/test-photo.jpg";
        String expectedPath = "photos/user-123/test-photo.jpg";

        when(storage.delete(any(BlobId.class))).thenReturn(true);

        gcsService.deleteFile(fileUrl);

        ArgumentCaptor<BlobId> blobIdCaptor = ArgumentCaptor.forClass(BlobId.class);
        verify(storage).delete(blobIdCaptor.capture());

        BlobId capturedBlobId = blobIdCaptor.getValue();
        assertThat(capturedBlobId.getBucket()).isEqualTo(testBucketName);
        assertThat(capturedBlobId.getName()).isEqualTo(expectedPath);
    }

}
