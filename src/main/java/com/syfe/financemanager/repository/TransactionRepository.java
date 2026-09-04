package com.syfe.financemanager.repository;

import com.syfe.financemanager.entity.Transaction;
import com.syfe.financemanager.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access for {@link Transaction}, including the filtered search and reporting aggregate queries. */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndDeletedFalse(Long id);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            select t from Transaction t
            where t.user.id = :userId
              and t.deleted = false
              and (:startDate is null or t.date >= :startDate)
              and (:endDate is null or t.date <= :endDate)
              and (:categoryId is null or t.category.id = :categoryId)
              and (:type is null or t.type = :type)
            order by t.date desc, t.id desc
            """)
    List<Transaction> search(@Param("userId") Long userId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("categoryId") Long categoryId,
                              @Param("type") TransactionType type);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.user.id = :userId
              and t.deleted = false
              and t.type = :type
              and t.date >= :fromDate
            """)
    BigDecimal sumByUserAndTypeSince(@Param("userId") Long userId,
                                      @Param("type") TransactionType type,
                                      @Param("fromDate") LocalDate fromDate);

    @Query("""
            select t.category.name as categoryName, sum(t.amount) as total
            from Transaction t
            where t.user.id = :userId
              and t.deleted = false
              and t.type = :type
              and t.date >= :startDate and t.date <= :endDate
            group by t.category.name
            """)
    List<CategoryTotal> sumByUserAndTypeBetweenGroupedByCategory(@Param("userId") Long userId,
                                                                  @Param("type") TransactionType type,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);

    interface CategoryTotal {
        String getCategoryName();
        BigDecimal getTotal();
    }
}
