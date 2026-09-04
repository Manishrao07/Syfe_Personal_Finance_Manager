package com.syfe.financemanager.repository;

import com.syfe.financemanager.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data access for {@link Category} — both global defaults and per-user custom categories. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Default (global) categories plus the given user's own custom categories. */
    List<Category> findByOwnerIsNullOrOwnerId(Long ownerId);

    Optional<Category> findByNameAndOwnerIsNull(String name);

    Optional<Category> findByNameAndOwnerId(String name, Long ownerId);

    boolean existsByNameAndOwnerId(String name, Long ownerId);

    boolean existsByNameAndOwnerIsNotNull(String name);
}
