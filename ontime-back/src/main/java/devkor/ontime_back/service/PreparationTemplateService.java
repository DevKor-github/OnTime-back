package devkor.ontime_back.service;

import devkor.ontime_back.dto.OrderedPreparationDto;
import devkor.ontime_back.dto.PreparationTemplateRequestDto;
import devkor.ontime_back.dto.PreparationTemplateResponseDto;
import devkor.ontime_back.dto.PreparationTemplateUpdateDto;
import devkor.ontime_back.entity.PreparationTemplate;
import devkor.ontime_back.entity.PreparationTemplateStep;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.PreparationTemplateRepository;
import devkor.ontime_back.repository.PreparationTemplateStepRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static devkor.ontime_back.response.ErrorCode.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PreparationTemplateService {
    private static final int ACTIVE_TEMPLATE_LIMIT = 20;

    private final PreparationTemplateRepository preparationTemplateRepository;
    private final PreparationTemplateStepRepository preparationTemplateStepRepository;
    private final UserRepository userRepository;
    private final PreparationStepService preparationStepService;
    private final ScheduleService scheduleService;

    public List<PreparationTemplateResponseDto> listTemplates(Long userId) {
        return preparationTemplateRepository.findActiveByUserId(userId).stream()
                .map(template -> toResponse(template, false))
                .toList();
    }

    public PreparationTemplateResponseDto getTemplate(Long userId, UUID templateId) {
        PreparationTemplate template = findOwnedTemplate(userId, templateId);
        return toResponse(template, true);
    }

    @Transactional
    public PreparationTemplateResponseDto createTemplate(Long userId, PreparationTemplateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(USER_NOT_FOUND));
        if (preparationTemplateRepository.existsById(request.getTemplateId())) {
            throw new GeneralException(RESOURCE_ALREADY_EXISTS);
        }
        if (preparationTemplateRepository.countByUser_IdAndDeletedAtIsNull(userId) >= ACTIVE_TEMPLATE_LIMIT) {
            throw new GeneralException(PREPARATION_TEMPLATE_LIMIT_EXCEEDED);
        }

        String templateName = normalizeDisplayName(request.getTemplateName());
        String normalizedName = normalizeLookupName(templateName);
        if (preparationTemplateRepository.existsByUser_IdAndNormalizedTemplateNameAndDeletedAtIsNull(userId, normalizedName)) {
            throw new GeneralException(PREPARATION_TEMPLATE_NAME_DUPLICATE);
        }

        List<OrderedPreparationDto> preparations = preparationStepService.normalizeOrdered(request.getPreparations());
        preparationStepService.assertStepIdsAvailableForNewTemplate(preparations);

        Instant now = now();
        PreparationTemplate template = PreparationTemplate.builder()
                .preparationTemplateId(request.getTemplateId())
                .user(user)
                .templateName(templateName)
                .normalizedTemplateName(normalizedName)
                .createdAt(now)
                .updatedAt(now)
                .build();
        preparationTemplateRepository.save(template);
        saveSteps(template, preparations);
        return toResponse(template, false);
    }

    @Transactional
    public PreparationTemplateResponseDto updateTemplate(Long userId, UUID templateId, PreparationTemplateUpdateDto request) {
        PreparationTemplate template = findOwnedTemplate(userId, templateId);
        if (template.isDeleted()) {
            throw new GeneralException(PREPARATION_TEMPLATE_DELETED);
        }

        String templateName = normalizeDisplayName(request.getTemplateName());
        String normalizedName = normalizeLookupName(templateName);
        if (preparationTemplateRepository.existsByUser_IdAndNormalizedTemplateNameAndDeletedAtIsNullAndPreparationTemplateIdNot(
                userId, normalizedName, templateId)) {
            throw new GeneralException(PREPARATION_TEMPLATE_NAME_DUPLICATE);
        }

        List<OrderedPreparationDto> preparations = preparationStepService.normalizeOrdered(request.getPreparations());
        preparationStepService.assertStepIdsAvailableForTemplate(preparations, template);

        template.update(templateName, normalizedName, now());
        preparationTemplateStepRepository.deleteByPreparationTemplate(template);
        preparationTemplateStepRepository.flush();
        saveSteps(template, preparations);
        preparationTemplateRepository.save(template);
        scheduleService.refreshNotStartedTemplateModeSchedules(template.getPreparationTemplateId());
        return toResponse(template, false);
    }

    @Transactional
    public void deleteTemplate(Long userId, UUID templateId) {
        PreparationTemplate template = findOwnedTemplate(userId, templateId);
        if (template.isDeleted()) {
            return;
        }
        template.softDelete(now());
        preparationTemplateRepository.save(template);
    }

    public PreparationTemplate findActiveTemplateForSchedule(Long userId, UUID templateId) {
        PreparationTemplate template = findOwnedTemplate(userId, templateId);
        if (template.isDeleted()) {
            throw new GeneralException(PREPARATION_TEMPLATE_DELETED);
        }
        return template;
    }

    private PreparationTemplate findOwnedTemplate(Long userId, UUID templateId) {
        return preparationTemplateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new GeneralException(PREPARATION_TEMPLATE_NOT_FOUND));
    }

    private void saveSteps(PreparationTemplate template, List<OrderedPreparationDto> preparations) {
        List<PreparationTemplateStep> steps = preparations.stream()
                .map(preparation -> PreparationTemplateStep.builder()
                        .preparationTemplateStepId(preparation.getPreparationId())
                        .preparationTemplate(template)
                        .preparationName(preparation.getPreparationName())
                        .preparationTime(preparation.getPreparationTime())
                        .orderIndex(preparation.getOrderIndex())
                        .build())
                .toList();
        preparationTemplateStepRepository.saveAll(steps);
    }

    public PreparationTemplateResponseDto toResponse(PreparationTemplate template, boolean includeDeletedAt) {
        List<OrderedPreparationDto> preparations = preparationTemplateStepRepository.findByPreparationTemplateOrdered(template).stream()
                .map(step -> OrderedPreparationDto.builder()
                        .preparationId(step.getPreparationTemplateStepId())
                        .preparationName(step.getPreparationName())
                        .preparationTime(step.getPreparationTime())
                        .orderIndex(step.getOrderIndex())
                        .build())
                .toList();

        return PreparationTemplateResponseDto.builder()
                .templateId(template.getPreparationTemplateId())
                .templateName(template.getTemplateName())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .deletedAt(includeDeletedAt ? template.getDeletedAt() : null)
                .preparations(preparations)
                .build();
    }

    private String normalizeDisplayName(String value) {
        if (value == null) {
            throw new GeneralException(INVALID_INPUT);
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 30) {
            throw new GeneralException(INVALID_INPUT);
        }
        return trimmed;
    }

    private String normalizeLookupName(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
