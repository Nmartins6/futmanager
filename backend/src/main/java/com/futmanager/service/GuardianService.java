package com.futmanager.service;

import com.futmanager.domain.entity.Guardian;
import com.futmanager.domain.entity.User;
import com.futmanager.dto.GuardianDtos.GuardianRequest;
import com.futmanager.dto.GuardianDtos.GuardianResponse;
import com.futmanager.exception.BusinessException;
import com.futmanager.exception.ConflictException;
import com.futmanager.exception.ResourceNotFoundException;
import com.futmanager.repository.GuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<GuardianResponse> listGuardians(String query, Pageable pageable) {
        UUID tenantId = userService.getCurrentUserEntity().getTenant().getId();
        String search = (query != null && !query.isBlank()) ? "%" + query.trim().toLowerCase() + "%" : null;
        return guardianRepository.searchGuardians(tenantId, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public GuardianResponse getGuardianById(UUID id) {
        UUID tenantId = userService.getCurrentUserEntity().getTenant().getId();
        Guardian guardian = guardianRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado com ID: " + id));
        return toResponse(guardian);
    }

    @Transactional
    public GuardianResponse createGuardian(GuardianRequest request) {
        if (request.phone() == null || request.phone().isBlank()) {
            throw new BusinessException("O telefone/WhatsApp do responsável é obrigatório.");
        }

        User currentUser = userService.getCurrentUserEntity();
        UUID tenantId = currentUser.getTenant().getId();
        String phone = request.phone().trim();

        if (guardianRepository.existsByTenantIdAndPhone(tenantId, phone)) {
            throw new ConflictException("Já existe um responsável cadastrado com o telefone: " + phone);
        }

        com.futmanager.util.ValidationUtils.validateOptionalEmail(request.email(), "O e-mail do responsável");

        Guardian guardian = Guardian.builder()
                .tenant(currentUser.getTenant())
                .fullName(request.fullName().trim())
                .phone(phone)
                .email(request.email() != null && !request.email().isBlank() ? request.email().trim().toLowerCase() : null)
                .build();

        guardian = guardianRepository.save(guardian);
        return toResponse(guardian);
    }

    @Transactional
    public GuardianResponse updateGuardian(UUID id, GuardianRequest request) {
        if (request.phone() == null || request.phone().isBlank()) {
            throw new BusinessException("O telefone/WhatsApp do responsável é obrigatório.");
        }

        UUID tenantId = userService.getCurrentUserEntity().getTenant().getId();
        Guardian guardian = guardianRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado com ID: " + id));

        String phone = request.phone().trim();
        if (guardianRepository.existsByTenantIdAndPhoneAndIdNot(tenantId, phone, id)) {
            throw new ConflictException("Já existe outro responsável cadastrado com o telefone: " + phone);
        }

        com.futmanager.util.ValidationUtils.validateOptionalEmail(request.email(), "O e-mail do responsável");

        guardian.setFullName(request.fullName().trim());
        guardian.setPhone(phone);
        guardian.setEmail(request.email() != null && !request.email().isBlank() ? request.email().trim().toLowerCase() : null);

        guardian = guardianRepository.save(guardian);
        return toResponse(guardian);
    }

    public GuardianResponse toResponse(Guardian g) {
        return new GuardianResponse(
                g.getId(),
                g.getFullName(),
                g.getPhone(),
                g.getEmail(),
                g.getStudentLinks() != null ? g.getStudentLinks().size() : 0,
                g.getCreatedAt()
        );
    }
}
