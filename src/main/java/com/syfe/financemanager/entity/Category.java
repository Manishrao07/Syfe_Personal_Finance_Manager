package com.syfe.financemanager.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A transaction category. Default categories are seeded once at startup with
 * {@code owner == null} and are visible to every user; custom categories belong to
 * exactly one user and must have a name that is unique within that user's scope.
 */
@Entity
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "owner_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /** true for user-created categories, false for the seeded system defaults. */
    @Column(nullable = false)
    private boolean custom;

    /** Null for default/global categories; set to the owning user for custom ones. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
}
