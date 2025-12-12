package com.example.drones.photos.storage;

import com.example.drones.common.config.BucketConfiguration;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Profile("!test")
public class GcsService implements FileStorage {

    private final Storage storage;
    private final BucketConfiguration bucketConfig;
    private static final String publicUrlPrefix = "https://storage.googleapis.com/";

    @Override
    public String uploadFile(MultipartFile file, String userSubdirectory) throws IOException {
        String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String path = bucketConfig.getSubdirectory() + "/" + userSubdirectory + "/" + filename;
        BlobId blobId = BlobId.of(bucketConfig.getBucketName(), path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        storage.createFrom(blobInfo, file.getInputStream());
        return publicUrlPrefix + bucketConfig.getBucketName() + "/" + path;
    }

    @Override
    public void deleteFile(String url) {
        String path = url.substring(publicUrlPrefix.length() + bucketConfig.getBucketName().length() + 1);
        BlobId blobId = BlobId.of(bucketConfig.getBucketName(), path);
        storage.delete(blobId);
    }

}
