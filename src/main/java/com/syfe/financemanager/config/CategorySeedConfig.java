package com.syfe.financemanager.config;

import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Seeds the system default categories once at startup. Defaults are global
 * ({@code owner == null}) and visible to every user; they are seeded idempotently
 * so restarts against the persisted H2 file never duplicate them.
 */
@Component
@RequiredArgsConstructor
public class CategorySeedConfig implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    private static final Map<TransactionType, List<String>> DEFAULTS = Map.of(
            TransactionType.INCOME, List.of("Salary"),
            TransactionType.EXPENSE, List.of("Food", "Rent", "Transportation", "Entertainment", "Healthcare", "Utilities")
    );

    @Override
    public void run(String... args) {
        DEFAULTS.forEach((type, names) -> names.forEach(name -> {
            if (categoryRepository.findByNameAndOwnerIsNull(name).isEmpty()) {
                categoryRepository.save(Category.builder()
                        .name(name)
                        .type(type)
                        .custom(false)
                        .owner(null)
                        .build());
            }
        }));
    }
}
