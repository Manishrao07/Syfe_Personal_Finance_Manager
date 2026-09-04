package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.request.CategoryRequest;
import com.syfe.financemanager.dto.response.CategoryListResponse;
import com.syfe.financemanager.dto.response.CategoryResponse;
import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.exception.BadRequestException;
import com.syfe.financemanager.exception.ConflictException;
import com.syfe.financemanager.exception.ForbiddenException;
import com.syfe.financemanager.exception.ResourceNotFoundException;
import com.syfe.financemanager.repository.CategoryRepository;
import com.syfe.financemanager.repository.TransactionRepository;
import com.syfe.financemanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    private static final Long USER_ID = 1L;

    @Test
    void getVisibleCategories_returnsDefaultsAndOwnCustomOnes() {
        Category salary = Category.builder().id(1L).name("Salary").type(TransactionType.INCOME).custom(false).build();
        Category custom = Category.builder().id(2L).name("SideGig").type(TransactionType.INCOME).custom(true).build();
        when(categoryRepository.findByOwnerIsNullOrOwnerId(USER_ID)).thenReturn(List.of(salary, custom));

        CategoryListResponse response = categoryService.getVisibleCategories(USER_ID);

        assertThat(response.getCategories()).extracting(CategoryResponse::getName)
                .containsExactly("Salary", "SideGig");
    }

    @Test
    void createCustomCategory_savesCategory_whenNameIsUniqueForUser() {
        CategoryRequest request = new CategoryRequest();
        request.setName("SideGig");
        request.setType(TransactionType.INCOME);

        when(categoryRepository.existsByNameAndOwnerId("SideGig", USER_ID)).thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.createCustomCategory(USER_ID, request);

        assertThat(response.getName()).isEqualTo("SideGig");
        assertThat(response.isCustom()).isTrue();
    }

    @Test
    void createCustomCategory_throwsConflict_whenNameAlreadyUsedByUser() {
        CategoryRequest request = new CategoryRequest();
        request.setName("SideGig");
        request.setType(TransactionType.INCOME);

        when(categoryRepository.existsByNameAndOwnerId("SideGig", USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCustomCategory(USER_ID, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteCategory_deletesOwnUnreferencedCategory() {
        Category custom = Category.builder().id(2L).name("SideGig").type(TransactionType.INCOME).custom(true).build();
        when(categoryRepository.findByNameAndOwnerId("SideGig", USER_ID)).thenReturn(Optional.of(custom));
        when(transactionRepository.existsByCategoryId(2L)).thenReturn(false);

        categoryService.deleteCategory(USER_ID, "SideGig");

        verify(categoryRepository).delete(custom);
    }

    @Test
    void deleteCategory_throwsConflict_whenCategoryReferencedByTransactions() {
        Category custom = Category.builder().id(2L).name("SideGig").type(TransactionType.INCOME).custom(true).build();
        when(categoryRepository.findByNameAndOwnerId("SideGig", USER_ID)).thenReturn(Optional.of(custom));
        when(transactionRepository.existsByCategoryId(2L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(USER_ID, "SideGig"))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_throwsBadRequest_whenDefaultCategory() {
        when(categoryRepository.findByNameAndOwnerId("Salary", USER_ID)).thenReturn(Optional.empty());
        when(categoryRepository.findByNameAndOwnerIsNull("Salary"))
                .thenReturn(Optional.of(Category.builder().id(1L).name("Salary").custom(false).build()));

        assertThatThrownBy(() -> categoryService.deleteCategory(USER_ID, "Salary"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteCategory_throwsForbidden_whenCategoryBelongsToAnotherUser() {
        when(categoryRepository.findByNameAndOwnerId("OtherUsersCategory", USER_ID)).thenReturn(Optional.empty());
        when(categoryRepository.findByNameAndOwnerIsNull("OtherUsersCategory")).thenReturn(Optional.empty());
        when(categoryRepository.existsByNameAndOwnerIsNotNull("OtherUsersCategory")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(USER_ID, "OtherUsersCategory"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteCategory_throwsNotFound_whenCategoryDoesNotExist() {
        when(categoryRepository.findByNameAndOwnerId("Ghost", USER_ID)).thenReturn(Optional.empty());
        when(categoryRepository.findByNameAndOwnerIsNull("Ghost")).thenReturn(Optional.empty());
        when(categoryRepository.existsByNameAndOwnerIsNotNull("Ghost")).thenReturn(false);

        assertThatThrownBy(() -> categoryService.deleteCategory(USER_ID, "Ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveVisibleCategory_returnsOwnCategory_whenPresent() {
        Category custom = Category.builder().id(2L).name("SideGig").custom(true).build();
        when(categoryRepository.findByNameAndOwnerId("SideGig", USER_ID)).thenReturn(Optional.of(custom));

        Category resolved = categoryService.resolveVisibleCategory(USER_ID, "SideGig");

        assertThat(resolved).isEqualTo(custom);
    }

    @Test
    void resolveVisibleCategory_fallsBackToDefault_whenNoOwnCategoryMatches() {
        Category salary = Category.builder().id(1L).name("Salary").custom(false).build();
        when(categoryRepository.findByNameAndOwnerId("Salary", USER_ID)).thenReturn(Optional.empty());
        when(categoryRepository.findByNameAndOwnerIsNull("Salary")).thenReturn(Optional.of(salary));

        Category resolved = categoryService.resolveVisibleCategory(USER_ID, "Salary");

        assertThat(resolved).isEqualTo(salary);
    }

    @Test
    void resolveVisibleCategory_throwsBadRequest_whenCategoryNotVisible() {
        when(categoryRepository.findByNameAndOwnerId("Nope", USER_ID)).thenReturn(Optional.empty());
        when(categoryRepository.findByNameAndOwnerIsNull("Nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.resolveVisibleCategory(USER_ID, "Nope"))
                .isInstanceOf(BadRequestException.class);
    }
}
