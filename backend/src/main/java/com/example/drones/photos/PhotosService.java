package com.example.drones.photos;

import com.example.drones.operators.PortfolioEntity;
import com.example.drones.operators.PortfolioRepository;
import com.example.drones.operators.exceptions.NoSuchPortfolioException;
import com.example.drones.photos.dto.PhotosDto;
import com.example.drones.photos.exceptions.PhotosUploadException;
import com.example.drones.photos.exceptions.PhotosUploadValidationException;
import com.example.drones.photos.storage.FileStorage;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PhotosService {

    private final FileStorage fileStorage;
    private final PhotosRepository photosRepository;
    private final PortfolioRepository portfolioRepository;
    private final PhotosMapper photosMapper;

    @CacheEvict(value = "operators", key = "#userId")
    @Transactional
    public PhotosDto addPhotos(UUID userId, List<MultipartFile> images, List<String> names) {
        if (images.size() != names.size()) {
            throw new PhotosUploadValidationException();
        }

        if (images.isEmpty()) {
            return new PhotosDto(List.of());
        }
        PortfolioEntity portfolio = portfolioRepository.findByOperatorId(userId)
                .orElseThrow(NoSuchPortfolioException::new);

        List<String> urls;
        try {
            urls = fileStorage.uploadFiles(images, userId.toString());
        } catch (Exception e) {
            throw new PhotosUploadException(e.getMessage());
        }
        List<PhotoEntity> photos = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            PhotoEntity photo = PhotoEntity.builder()
                    .name(names.get(i))
                    .url(urls.get(i))
                    .portfolio(portfolio)
                    .build();
            photos.add(photo);
        }
        photosRepository.saveAll(photos);
        List<PhotoEntity> allPhotos = photosRepository.findAllByPortfolio(portfolio);

        return new PhotosDto(photosMapper.toDto(allPhotos));
    }

    @Transactional
    @CacheEvict(value = "operators", key = "#userId")
    public void deletePhotos(UUID userId, List<Integer> photoIds) {
        List<PhotoEntity> photosToDelete = photosRepository.findMyPhotos(photoIds, userId);
        for (PhotoEntity photo : photosToDelete) {
            fileStorage.deleteFile(photo.getUrl());
        }
        photosRepository.deleteAll(photosToDelete);
    }
}
