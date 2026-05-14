package devkor.ontime_back.service;

import devkor.ontime_back.dto.OrderedPreparationDto;
import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.entity.PreparationSchedule;
import devkor.ontime_back.entity.PreparationTemplate;
import devkor.ontime_back.entity.PreparationTemplateStep;
import devkor.ontime_back.entity.PreparationUser;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.repository.PreparationScheduleRepository;
import devkor.ontime_back.repository.PreparationTemplateStepRepository;
import devkor.ontime_back.repository.PreparationUserRepository;
import devkor.ontime_back.response.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static devkor.ontime_back.response.ErrorCode.INVALID_INPUT;
import static devkor.ontime_back.response.ErrorCode.PREPARATION_STEP_ID_CONFLICT;

@Service
@RequiredArgsConstructor
public class PreparationStepService {
    public static final int MAX_STEP_COUNT = 50;
    public static final int MAX_TOTAL_MINUTES = 1440;

    private final PreparationUserRepository preparationUserRepository;
    private final PreparationScheduleRepository preparationScheduleRepository;
    private final PreparationTemplateStepRepository preparationTemplateStepRepository;

    public List<OrderedPreparationDto> normalizeOrdered(List<OrderedPreparationDto> preparations) {
        if (preparations == null || preparations.isEmpty() || preparations.size() > MAX_STEP_COUNT) {
            throw new GeneralException(INVALID_INPUT);
        }
        Set<UUID> ids = new HashSet<>();
        Set<Integer> indexes = new HashSet<>();
        int total = 0;
        for (OrderedPreparationDto preparation : preparations) {
            if (preparation.getPreparationId() == null
                    || !ids.add(preparation.getPreparationId())
                    || preparation.getOrderIndex() == null
                    || !indexes.add(preparation.getOrderIndex())
                    || preparation.getPreparationName() == null
                    || preparation.getPreparationName().trim().isEmpty()
                    || preparation.getPreparationName().trim().length() > 50
                    || preparation.getPreparationTime() == null
                    || preparation.getPreparationTime() < 1
                    || preparation.getPreparationTime() > 1440) {
                throw new GeneralException(INVALID_INPUT);
            }
            total += preparation.getPreparationTime();
        }
        if (total > MAX_TOTAL_MINUTES) {
            throw new GeneralException(INVALID_INPUT);
        }
        for (int i = 0; i < preparations.size(); i++) {
            if (!indexes.contains(i)) {
                throw new GeneralException(INVALID_INPUT);
            }
        }
        return preparations.stream()
                .map(preparation -> OrderedPreparationDto.builder()
                        .preparationId(preparation.getPreparationId())
                        .preparationName(preparation.getPreparationName().trim())
                        .preparationTime(preparation.getPreparationTime())
                        .orderIndex(preparation.getOrderIndex())
                        .build())
                .sorted(Comparator.comparing(OrderedPreparationDto::getOrderIndex))
                .collect(Collectors.toList());
    }

    public List<OrderedPreparationDto> normalizeLinked(List<PreparationDto> preparations) {
        if (preparations == null || preparations.isEmpty() || preparations.size() > MAX_STEP_COUNT) {
            throw new GeneralException(INVALID_INPUT);
        }

        Map<UUID, PreparationDto> byId = new HashMap<>();
        Set<UUID> referenced = new HashSet<>();
        int total = 0;
        for (PreparationDto preparation : preparations) {
            if (preparation.getPreparationId() == null
                    || byId.put(preparation.getPreparationId(), preparation) != null
                    || preparation.getPreparationName() == null
                    || preparation.getPreparationName().trim().isEmpty()
                    || preparation.getPreparationName().trim().length() > 50
                    || preparation.getPreparationTime() == null
                    || preparation.getPreparationTime() < 1
                    || preparation.getPreparationTime() > 1440) {
                throw new GeneralException(INVALID_INPUT);
            }
            total += preparation.getPreparationTime();
            if (preparation.getNextPreparationId() != null) {
                referenced.add(preparation.getNextPreparationId());
            }
        }
        if (total > MAX_TOTAL_MINUTES || !byId.keySet().containsAll(referenced)) {
            throw new GeneralException(INVALID_INPUT);
        }

        List<UUID> heads = byId.keySet().stream()
                .filter(id -> !referenced.contains(id))
                .collect(Collectors.toList());
        if (heads.size() != 1) {
            throw new GeneralException(INVALID_INPUT);
        }

        List<OrderedPreparationDto> ordered = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        UUID currentId = heads.get(0);
        while (currentId != null) {
            if (!seen.add(currentId)) {
                throw new GeneralException(INVALID_INPUT);
            }
            PreparationDto current = byId.get(currentId);
            if (current == null) {
                throw new GeneralException(INVALID_INPUT);
            }
            ordered.add(OrderedPreparationDto.builder()
                    .preparationId(current.getPreparationId())
                    .preparationName(current.getPreparationName().trim())
                    .preparationTime(current.getPreparationTime())
                    .orderIndex(ordered.size())
                    .build());
            currentId = current.getNextPreparationId();
        }
        if (seen.size() != preparations.size()) {
            throw new GeneralException(INVALID_INPUT);
        }
        return ordered;
    }

    public List<PreparationDto> toLinkedDtoFromUser(List<PreparationUser> preparations) {
        if (preparations.stream().anyMatch(preparation -> preparation.getOrderIndex() == null)) {
            Map<UUID, PreparationUser> byId = preparations.stream()
                    .collect(Collectors.toMap(PreparationUser::getPreparationUserId, preparation -> preparation));
            Set<UUID> referenced = preparations.stream()
                    .map(PreparationUser::getNextPreparation)
                    .filter(Objects::nonNull)
                    .map(PreparationUser::getPreparationUserId)
                    .collect(Collectors.toSet());
            Optional<PreparationUser> head = preparations.stream()
                    .filter(preparation -> !referenced.contains(preparation.getPreparationUserId()))
                    .findFirst();
            if (head.isPresent()) {
                List<PreparationDto> result = new ArrayList<>();
                Set<UUID> seen = new HashSet<>();
                PreparationUser current = head.get();
                while (current != null && seen.add(current.getPreparationUserId())) {
                    UUID nextId = current.getNextPreparation() != null ? current.getNextPreparation().getPreparationUserId() : null;
                    result.add(new PreparationDto(
                            current.getPreparationUserId(),
                            current.getPreparationName(),
                            current.getPreparationTime(),
                            nextId
                    ));
                    current = nextId != null ? byId.get(nextId) : null;
                }
                if (result.size() == preparations.size()) {
                    return result;
                }
            }
        }
        return toLinkedDto(preparations.stream()
                .map(preparation -> OrderedPreparationDto.builder()
                        .preparationId(preparation.getPreparationUserId())
                        .preparationName(preparation.getPreparationName())
                        .preparationTime(preparation.getPreparationTime())
                        .orderIndex(preparation.getOrderIndex())
                        .build())
                .collect(Collectors.toList()));
    }

    public List<PreparationDto> toLinkedDtoFromSchedule(List<PreparationSchedule> preparations) {
        if (preparations.stream().anyMatch(preparation -> preparation.getOrderIndex() == null)) {
            Map<UUID, PreparationSchedule> byId = preparations.stream()
                    .collect(Collectors.toMap(PreparationSchedule::getPreparationScheduleId, preparation -> preparation));
            Set<UUID> referenced = preparations.stream()
                    .map(PreparationSchedule::getNextPreparation)
                    .filter(Objects::nonNull)
                    .map(PreparationSchedule::getPreparationScheduleId)
                    .collect(Collectors.toSet());
            Optional<PreparationSchedule> head = preparations.stream()
                    .filter(preparation -> !referenced.contains(preparation.getPreparationScheduleId()))
                    .findFirst();
            if (head.isPresent()) {
                List<PreparationDto> result = new ArrayList<>();
                Set<UUID> seen = new HashSet<>();
                PreparationSchedule current = head.get();
                while (current != null && seen.add(current.getPreparationScheduleId())) {
                    UUID nextId = current.getNextPreparation() != null ? current.getNextPreparation().getPreparationScheduleId() : null;
                    result.add(new PreparationDto(
                            current.getPreparationScheduleId(),
                            current.getPreparationName(),
                            current.getPreparationTime(),
                            nextId
                    ));
                    current = nextId != null ? byId.get(nextId) : null;
                }
                if (result.size() == preparations.size()) {
                    return result;
                }
            }
        }
        return toLinkedDto(preparations.stream()
                .map(preparation -> OrderedPreparationDto.builder()
                        .preparationId(preparation.getPreparationScheduleId())
                        .preparationName(preparation.getPreparationName())
                        .preparationTime(preparation.getPreparationTime())
                        .orderIndex(preparation.getOrderIndex())
                        .build())
                .collect(Collectors.toList()));
    }

    public List<PreparationDto> toLinkedDtoFromTemplate(List<PreparationTemplateStep> preparations) {
        return toLinkedDto(preparations.stream()
                .map(preparation -> OrderedPreparationDto.builder()
                        .preparationId(preparation.getPreparationTemplateStepId())
                        .preparationName(preparation.getPreparationName())
                        .preparationTime(preparation.getPreparationTime())
                        .orderIndex(preparation.getOrderIndex())
                        .build())
                .collect(Collectors.toList()));
    }

    public List<PreparationDto> toLinkedDto(List<OrderedPreparationDto> orderedPreparations) {
        List<OrderedPreparationDto> sorted = orderedPreparations.stream()
                .sorted(Comparator.comparing(OrderedPreparationDto::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        List<PreparationDto> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            OrderedPreparationDto current = sorted.get(i);
            UUID nextId = (i + 1 < sorted.size()) ? sorted.get(i + 1).getPreparationId() : null;
            result.add(new PreparationDto(
                    current.getPreparationId(),
                    current.getPreparationName(),
                    current.getPreparationTime(),
                    nextId
            ));
        }
        return result;
    }

    public void assertStepIdsAvailableForDefault(List<OrderedPreparationDto> preparations, Long userId) {
        for (OrderedPreparationDto preparation : preparations) {
            UUID id = preparation.getPreparationId();
            if ((preparationUserRepository.existsById(id) && !preparationUserRepository.existsByPreparationUserIdAndUser_Id(id, userId))
                    || preparationScheduleRepository.existsById(id)
                    || preparationTemplateStepRepository.existsById(id)) {
                throw new GeneralException(PREPARATION_STEP_ID_CONFLICT);
            }
        }
    }

    public void assertStepIdsAvailableForSchedule(List<OrderedPreparationDto> preparations, Schedule schedule) {
        for (OrderedPreparationDto preparation : preparations) {
            UUID id = preparation.getPreparationId();
            if (preparationUserRepository.existsById(id)
                    || (preparationScheduleRepository.existsById(id) && !preparationScheduleRepository.existsByPreparationScheduleIdAndSchedule(id, schedule))
                    || preparationTemplateStepRepository.existsById(id)) {
                throw new GeneralException(PREPARATION_STEP_ID_CONFLICT);
            }
        }
    }

    public void assertStepIdsAvailableForTemplate(List<OrderedPreparationDto> preparations, PreparationTemplate template) {
        for (OrderedPreparationDto preparation : preparations) {
            UUID id = preparation.getPreparationId();
            if (preparationUserRepository.existsById(id)
                    || preparationScheduleRepository.existsById(id)
                    || (preparationTemplateStepRepository.existsById(id) && !preparationTemplateStepRepository.existsByPreparationTemplateStepIdAndPreparationTemplate(id, template))) {
                throw new GeneralException(PREPARATION_STEP_ID_CONFLICT);
            }
        }
    }

    public void assertStepIdsAvailableForNewTemplate(List<OrderedPreparationDto> preparations) {
        for (OrderedPreparationDto preparation : preparations) {
            UUID id = preparation.getPreparationId();
            if (preparationUserRepository.existsById(id)
                    || preparationScheduleRepository.existsById(id)
                    || preparationTemplateStepRepository.existsById(id)) {
                throw new GeneralException(PREPARATION_STEP_ID_CONFLICT);
            }
        }
    }
}
