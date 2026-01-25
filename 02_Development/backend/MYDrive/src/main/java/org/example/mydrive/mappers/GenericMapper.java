package org.example.mydrive.mappers;

public interface GenericMapper<E, D> {
    D toDto(E entity);
    E toEntity(D dto);
}
