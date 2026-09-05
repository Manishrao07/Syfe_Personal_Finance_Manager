package com.syfe.financemanager.controller;

import com.syfe.financemanager.dto.request.GoalRequest;
import com.syfe.financemanager.dto.request.GoalUpdateRequest;
import com.syfe.financemanager.dto.response.GoalListResponse;
import com.syfe.financemanager.dto.response.GoalResponse;
import com.syfe.financemanager.dto.response.MessageResponse;
import com.syfe.financemanager.security.SecurityUtils;
import com.syfe.financemanager.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** CRUD for the authenticated user's savings goals. */
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    /** Creates a savings goal for the caller, with progress computed from their existing transactions. */
    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody GoalRequest request) {
        GoalResponse response = goalService.create(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Lists the caller's savings goals, each with progress recomputed fresh. */
    @GetMapping
    public ResponseEntity<GoalListResponse> getAll() {
        return ResponseEntity.ok(goalService.getAll(SecurityUtils.getCurrentUserId()));
    }

    /** Returns one of the caller's own savings goals; another user's goal is reported as 403. */
    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getOne(SecurityUtils.getCurrentUserId(), id));
    }

    /** Updates the target amount and/or target date of one of the caller's own goals. */
    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(@PathVariable Long id, @Valid @RequestBody GoalUpdateRequest request) {
        return ResponseEntity.ok(goalService.update(SecurityUtils.getCurrentUserId(), id, request));
    }

    /** Deletes one of the caller's own savings goals. */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.delete(SecurityUtils.getCurrentUserId(), id));
    }
}
