package com.example.drones.photos;

import com.example.drones.photos.dto.PhotoDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PhotosMapper {

    List<PhotoDto> toDto(List<PhotoEntity> entities);

}
