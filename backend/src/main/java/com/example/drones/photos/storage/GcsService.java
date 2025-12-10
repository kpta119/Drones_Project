package com.example.drones.photos.storage;

import com.example.drones.common.config.BucketConfiguration;
import com.google.cloud.storage.Blob;
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

    @Override
    public String uploadFile(MultipartFile file, String userSubdirectory) throws IOException {
        String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String path = bucketConfig.getSubdirectory() + "/" + userSubdirectory + "/" + filename;
        BlobId blobId = BlobId.of(bucketConfig.getBucketName(), path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        Blob blob = storage.createFrom(blobInfo, file.getInputStream());
        return blob.getMediaLink();
    }

}
