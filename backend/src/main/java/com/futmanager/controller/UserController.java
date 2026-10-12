package com.futmanager.controller;

import com.futmanager.domain.enums.Role;
import com.futmanager.domain.enums.UserStatus;
import com.futmanager.dto.AuthDtos.InviteUserRequest;
import com.futmanager.dto.UserDtos.UserResponse;
import com.futmanager.dto.UserDtos.UserUpdateRequest;
import com.futmanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Gerenciamento de usuários, professores e permissões")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar usuários com filtros e paginação")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(query, role, status, pageable));
    }

    @GetMapping("/teachers")
    @Operation(summary = "Listar professores ativos para seleção em turmas")
    public ResponseEntity<List<UserResponse>> listActiveTeachers() {
        return ResponseEntity.ok(userService.listActiveTeachers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Obter usuário por ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Convidar / provisionar novo usuário (Administrador ou Professor)")
    public ResponseEntity<UserResponse> inviteUser(@Valid @RequestBody InviteUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.inviteUser(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Atualizar usuário")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Ativar ou desativar usuário")
    public ResponseEntity<UserResponse> toggleStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }
}
