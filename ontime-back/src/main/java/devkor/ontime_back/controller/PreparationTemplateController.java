package devkor.ontime_back.controller;

import devkor.ontime_back.dto.PreparationTemplateRequestDto;
import devkor.ontime_back.dto.PreparationTemplateResponseDto;
import devkor.ontime_back.dto.PreparationTemplateUpdateDto;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.PreparationTemplateService;
import devkor.ontime_back.service.UserAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/preparation-templates")
@RequiredArgsConstructor
@Validated
public class PreparationTemplateController {
    private final PreparationTemplateService preparationTemplateService;
    private final UserAuthService userAuthService;

    @GetMapping
    public ResponseEntity<ApiResponseForm<List<PreparationTemplateResponseDto>>> listTemplates(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.ok(ApiResponseForm.success(preparationTemplateService.listTemplates(userId)));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<ApiResponseForm<PreparationTemplateResponseDto>> getTemplate(
            HttpServletRequest request,
            @PathVariable UUID templateId) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.ok(ApiResponseForm.success(preparationTemplateService.getTemplate(userId, templateId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponseForm<PreparationTemplateResponseDto>> createTemplate(
            HttpServletRequest request,
            @Valid @RequestBody PreparationTemplateRequestDto requestDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseForm.success(preparationTemplateService.createTemplate(userId, requestDto)));
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<ApiResponseForm<PreparationTemplateResponseDto>> updateTemplate(
            HttpServletRequest request,
            @PathVariable UUID templateId,
            @Valid @RequestBody PreparationTemplateUpdateDto requestDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.ok(ApiResponseForm.success(preparationTemplateService.updateTemplate(userId, templateId, requestDto)));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<ApiResponseForm<Void>> deleteTemplate(
            HttpServletRequest request,
            @PathVariable UUID templateId) {
        Long userId = userAuthService.getUserIdFromToken(request);
        preparationTemplateService.deleteTemplate(userId, templateId);
        return ResponseEntity.ok(ApiResponseForm.success(null));
    }
}
