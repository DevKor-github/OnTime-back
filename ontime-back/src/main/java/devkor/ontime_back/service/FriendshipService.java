package devkor.ontime_back.service;

import devkor.ontime_back.dto.FriendDto;
import devkor.ontime_back.entity.FriendShip;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.FriendshipRepository;
import devkor.ontime_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Transactional
    public String createFriendShipLink(Long requesterId) {

        userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 id입니다."));

        UUID friendShipId = UUID.randomUUID();
        FriendShip friendShip = FriendShip.builder()
                .friendShipId(friendShipId)
                .requesterId(requesterId)
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        return String.valueOf(friendShipId);
    }


    @Transactional
    public User getFriendShipRequester(Long receiverId, UUID friendshipId) {

        userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 id입니다."));

        FriendShip friendShip = friendshipRepository.findByFriendShipId(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 친구 요청입니다."));


        // UUID로 조회한 FriendShip 데이터에 수신자 ID 세팅
        friendShip.updateReceiverId(receiverId);
        friendshipRepository.save(friendShip);

        // UUID로 조회한 FriendShip 데이터의 요청자 조회 및 반환
        return userRepository.findById(friendShip.getRequesterId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 친구추가 요청자 id입니다. 해당 유저가 탈퇴했을 수 있습니다."));
    }

    @Transactional
    public void updateAcceptStatus(Long receiverId, UUID friendshipId, String acceptStatus) {

        userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 id입니다."));

        FriendShip friendShip = friendshipRepository.findByFriendShipId(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 친구 요청입니다."));

        if (!friendShip.getReceiverId().equals(receiverId)) {
            throw new IllegalArgumentException("수신자 ID가 친구관계 ID와 매칭되지 않습니다.");
        }

        friendShip.updateAcceptStatus(acceptStatus);
        friendshipRepository.save(friendShip);
    }

    // 친구 목록 조회
    // 친구 요청자와 수신자에서 각각 ACCEPTED 상태인 친구관계를 조회하여 친구목록을 반환
    public List<FriendDto> getFriendList(Long userId) {

        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 id입니다."));

        List<FriendShip> friendShips = friendshipRepository.findByRequesterIdAndAcceptStatus(userId, "ACCEPTED");
        List<FriendDto> friendList = friendShips.stream()
                .map(friendShip -> {
                    User user = userRepository.findById(friendShip.getReceiverId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 친구 id입니다."));
                    return new FriendDto(user.getId(), user.getName(), user.getEmail());
                })
                .collect(Collectors.toList());

        List<FriendShip> friendShips2 = friendshipRepository.findByReceiverIdAndAcceptStatus(userId, "ACCEPTED");
        List<FriendDto> friendList2 = friendShips2.stream()
                .map(friendShip -> {
                    User user = userRepository.findById(friendShip.getRequesterId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 친구 id입니다."));
                    return new FriendDto(user.getId(), user.getName(), user.getEmail());
                })
                .collect(Collectors.toList());

        friendList.addAll(friendList2);

        return friendList;
    }

}
