package devkor.ontime_back.service;

import devkor.ontime_back.dto.UserSettingUpdateDto;
import devkor.ontime_back.entity.UserSetting;
import devkor.ontime_back.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSettingService {
    private final UserSettingRepository userSettingRepository;

    @Transactional
    public void updateSetting(Long userId, UserSettingUpdateDto userSettingUpdateDto) {
        UserSetting userSetting = userSettingRepository.findByUserId(userId)
                .orElseThrow(()-> new IllegalArgumentException("UserSetting not found with given userId"));

        userSetting.updateUserSetting(
                userSettingUpdateDto.getIsNotificationsEnabled(),
                userSettingUpdateDto.getSoundVolume(),
                userSettingUpdateDto.getIsPlayOnSpeaker(),
                userSettingUpdateDto.getIs24HourFormat()
        );

        userSettingRepository.save(userSetting);
    }

    @Transactional
    public void resetSetting(Long userId) {
        UserSetting userSetting = userSettingRepository.findByUserId(userId)
                .orElseThrow(()-> new IllegalArgumentException("UserSetting not found with given userId"));

        userSetting.updateUserSetting(
                true,
                50,
                true,
                true
        );

        userSettingRepository.save(userSetting);
    }
}
