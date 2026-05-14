package devkor.ontime_back.service;


import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.dto.OrderedPreparationDto;
import devkor.ontime_back.entity.PreparationUser;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.PreparationUserRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static devkor.ontime_back.response.ErrorCode.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PreparationUserService {
    private final PreparationUserRepository preparationUserRepository;
    private final UserRepository userRepository;
    private final PreparationStepService preparationStepService;
    private final ObjectProvider<ScheduleService> scheduleServiceProvider;

    @Transactional
    // 회원가입 시 디폴트 준비과정 세팅
    public void setFirstPreparationUser(Long userId, List<PreparationDto> preparationDtoList) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new GeneralException(USER_NOT_FOUND)
        );
        boolean exists = preparationUserRepository.existsByUser(user);
        if (exists) {
            throw new GeneralException(PREPARATION_ALREADY_EXISTS);
        }
        handlePreparationUsers(user, preparationDtoList, false);

    }

    // 준비과정 수정
    @Transactional
    public void updatePreparationUsers(Long userId, List<PreparationDto> preparationDtoList) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new GeneralException(USER_NOT_FOUND)
        );
        handlePreparationUsers(user, preparationDtoList, true);

    }

    // 준비과정 불러오기
    public List<PreparationDto> showAllPreparationUsers(Long userId) {

        List<PreparationUser> preparations = preparationUserRepository.findByUserIdWithNextPreparation(userId);
        if (preparations.isEmpty()) {
            throw new GeneralException(FIRST_PREPARATION_NOT_FOUND);
        }
        return preparationStepService.toLinkedDtoFromUser(preparations);
    }

    @Transactional
    protected void handlePreparationUsers(User user, List<PreparationDto> preparationDtoList, boolean shouldDeleteExisting) {
        List<OrderedPreparationDto> orderedPreparations = preparationStepService.normalizeLinked(preparationDtoList);
        preparationStepService.assertStepIdsAvailableForDefault(orderedPreparations, user.getId());

        if (shouldDeleteExisting) {
            preparationUserRepository.deleteByUser(user);
            preparationUserRepository.flush();
        }

        Map<UUID, PreparationUser> preparationMap = new HashMap<>();

        List<PreparationUser> preparationUsers = orderedPreparations.stream()
                .map(dto -> {
                    PreparationUser preparation = new PreparationUser(
                            dto.getPreparationId(),
                            user,
                            dto.getPreparationName(),
                            dto.getPreparationTime(),
                            dto.getOrderIndex(),
                            null // nextPreparation 설정은 나중에
                    );
                    preparationMap.put(dto.getPreparationId(), preparation);
                    return preparation;
                })
                .collect(Collectors.toList());

        preparationUserRepository.saveAll(preparationUsers);
        preparationUserRepository.flush();

        for (int i = 0; i < orderedPreparations.size() - 1; i++) {
            PreparationUser current = preparationMap.get(orderedPreparations.get(i).getPreparationId());
            PreparationUser nextPreparation = preparationMap.get(orderedPreparations.get(i + 1).getPreparationId());
            current.updateNextPreparation(nextPreparation);
        }

        preparationUserRepository.saveAll(preparationUsers);
        if (shouldDeleteExisting) {
            scheduleServiceProvider.getObject().refreshNotStartedDefaultModeSchedules(user.getId());
        }
    }


}
