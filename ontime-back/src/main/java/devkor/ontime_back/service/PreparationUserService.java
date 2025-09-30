package devkor.ontime_back.service;


import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.entity.PreparationUser;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.PreparationUserRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.ErrorCode;
import devkor.ontime_back.response.GeneralException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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

        PreparationUser firstPreparation = preparationUserRepository.findFirstPreparationUserByUserIdWithNextPreparation(userId)
                .orElseThrow(() -> new GeneralException(FIRST_PREPARATION_NOT_FOUND));

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
            preparationUserRepository.flush();
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
        preparationUserRepository.flush();

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