package devkor.ontime_back.service;


import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.entity.PreparationUser;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.PreparationUserRepository;
import devkor.ontime_back.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PreparationUserService {
    private final PreparationUserRepository preparationUserRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    // 회원가입 시 디폴트 준비과정 세팅
    public void setFirstPreparationUser(Long userId, List<PreparationDto> preparationDtoList) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new EntityNotFoundException("사용자 ID " + userId + "에 해당하는 사용자를 찾을 수 없습니다.")
        );
        handlePreparationUsers(user, preparationDtoList, false);

    }

    // 준비과정 수정
    @Transactional
    public void updatePreparationUsers(Long userId, List<PreparationDto> preparationDtoList) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new EntityNotFoundException("사용자 ID " + userId + "에 해당하는 사용자를 찾을 수 없습니다.")
        );
        handlePreparationUsers(user, preparationDtoList, true);

    }

    // 준비과정 불러오기
    public List<PreparationDto> showAllPreparationUsers(Long userId) {

        PreparationUser firstPreparation = preparationUserRepository.findFirstPreparationUserByUserIdWithNextPreparation(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자 ID " + userId + "에 대한 시작 준비 단계를 찾을 수 없습니다."));

        List<PreparationDto> preparationDtos = new ArrayList<>();
        PreparationUser current = firstPreparation;

        while (current != null) {
            PreparationDto dto = new PreparationDto(
                    current.getPreparationUserId(),
                    current.getPreparationName(),
                    current.getPreparationTime(),
                    current.getNextPreparation() != null ? current.getNextPreparation().getPreparationUserId() : null
            );
            preparationDtos.add(dto);
            current = current.getNextPreparation();
        }

        return preparationDtos;
    }

    @Transactional
    protected void handlePreparationUsers(User user, List<PreparationDto> preparationDtoList, boolean shouldDeleteExisting) {
        if (shouldDeleteExisting) {
            preparationUserRepository.deleteByUser(user);
        }

        Map<UUID, PreparationUser> preparationMap = new HashMap<>();

        List<PreparationUser> preparationUsers = preparationDtoList.stream()
                .map(dto -> {
                    PreparationUser preparation = new PreparationUser(
                            dto.getPreparationId(),
                            user,
                            dto.getPreparationName(),
                            dto.getPreparationTime(),
                            null // nextPreparation 설정은 나중에
                    );
                    preparationMap.put(dto.getPreparationId(), preparation);
                    return preparation;
                })
                .collect(Collectors.toList());

        preparationUserRepository.saveAll(preparationUsers);

        preparationDtoList.stream()
                .filter(dto -> dto.getNextPreparationId() != null)
                .forEach(dto -> {
                    PreparationUser current = preparationMap.get(dto.getPreparationId());
                    PreparationUser nextPreparation = preparationMap.get(dto.getNextPreparationId());
                    if (nextPreparation != null) {
                        current.updateNextPreparation(nextPreparation);
                    }
                });

        preparationUserRepository.saveAll(preparationUsers);
    }


}