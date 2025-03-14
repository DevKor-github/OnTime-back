package devkor.ontime_back.service;

import devkor.ontime_back.dto.FriendDto;
import devkor.ontime_back.entity.FriendShip;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.FriendshipRepository;
import devkor.ontime_back.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class FriendshipServiceTest {

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        friendshipRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("친구추가 링크 생성에 성공한다")
    @Test
    void createFriendshipLink(){
        // given
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedUser);

        // when
        String returnedLink = friendshipService.createFriendShipLink(addedUser.getId());

        // then
        assertThat(returnedLink).isNotNull();
        assertThat(friendshipRepository.findByFriendShipId(UUID.fromString(returnedLink))).isNotNull();
    }

    @DisplayName("친구추가 링크 생성시 잘못된 유저가 전달되는 경우 에외가 발생한다")
    @Test
    void createFriendshipLinkWithWrongUser(){
        // given
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedUser);

        // when // then
        assertThatThrownBy(() -> friendshipService.createFriendShipLink(addedUser.getId() + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");
    }


    @DisplayName("친구추가 요청자 정보 조회에 성공한다." +
            "(Friendship 데이터의 receiverId를 세팅하고 해당 데이터의 수신자 User 정보를 반환에 성공한다.)")
    @Test
    void getFriendShipRequester(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        // when
        User returnedRequester = friendshipService.getFriendShipRequester(addedReceiver.getId(), friendShip.getFriendShipId());

        // then
        // 1. 수신자 ID 세팅 확인 2. 요청자 정보 반환 확인
        FriendShip updatedFriendship = friendshipRepository.findByFriendShipId(friendShip.getFriendShipId()).orElseThrow();
        assertThat(updatedFriendship.getReceiverId()).isEqualTo(addedReceiver.getId());

        assertThat(returnedRequester).isNotNull();
        assertThat(returnedRequester.getId()).isEqualTo(addedRequester.getId());
    }

    @DisplayName("친구추가 요청자 정보 조회 시 잘못된 유저id가 전달될 때 예외가 발생한다.")
    @Test
    void getFriendShipRequesterWithWrongUserId(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        // when // then
        assertThatThrownBy(() -> friendshipService.getFriendShipRequester(addedReceiver.getId() + 123456789, friendShip.getFriendShipId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");
    }

    @DisplayName("친구추가 요청자 정보 조회 시 잘못된 친구관계id(UUID)가 전달될 때 예외가 발생한다.")
    @Test
    void getFriendRequesterWithWrongFriendShipShipId(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        // when // then
        assertThatThrownBy(() -> friendshipService.getFriendShipRequester(addedReceiver.getId(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 친구 요청입니다. 친구 추가를 신청한 유저가 탈퇴했을 수 있습니다.");
    }

    @DisplayName("친구추가 요청자 정보 조회 시 잘못된 친구추가 요청자id가 전달될 때 예외가 발생한다.")
    @Test
    void getFriendShipRequesterWithWrongRequeseterId(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        // 친구추가 요청자 삭제(친구추가 요청한 사람이 Ontime 탈퇴)
        userRepository.delete(addedRequester);

        // when // then
        assertThatThrownBy(() -> friendshipService.getFriendShipRequester(addedReceiver.getId(), friendShip.getFriendShipId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 친구 요청입니다. 친구 추가를 신청한 유저가 탈퇴했을 수 있습니다.");
    }


    @DisplayName("친구요청 '수락'에 성공한다.")
    @Test
    void updateAcceptStatusWithAccept(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .receiverId(addedReceiver.getId())
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        // when
        friendshipService.updateAcceptStatus(addedReceiver.getId(), friendShip.getFriendShipId(), "ACCEPTED");

        // then
        assertThat(friendshipRepository.findByFriendShipId(friendShip.getFriendShipId()).orElseThrow().getAcceptStatus()).isEqualTo("ACCEPTED");
    }

    @DisplayName("친구요청 '거절'에 성공한다.")
    @Test
    void updateAcceptStatusWithReject(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .receiverId(addedReceiver.getId())
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        // when
        friendshipService.updateAcceptStatus(addedReceiver.getId(), friendShip.getFriendShipId(), "REJECTED");

        // then
        assertThat(friendshipRepository.findByFriendShipId(friendShip.getFriendShipId()).orElseThrow().getAcceptStatus()).isEqualTo("REJECTED");
    }

    @DisplayName("친구요청 acceptStatus 변경 시 잘못된 유저id가 전달될 때 예외가 발생한다.")
    @Test
    void updateAcceptStatusWithWrongUserId(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .receiverId(addedReceiver.getId())
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        // when // then
        assertThatThrownBy(() -> friendshipService.updateAcceptStatus(addedReceiver.getId() + 123456789, friendShip.getFriendShipId(), "ACCEPTED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");
    }

    @DisplayName("친구요청 acceptStatus 변경 시 잘못된 친구관계id(UUID)가 전달될 때 예외가 발생한다.")
    @Test
    void updateAcceptStatusWithWrongFrienShipId(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .receiverId(addedReceiver.getId())
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        // when // then
        assertThatThrownBy(() -> friendshipService.updateAcceptStatus(addedReceiver.getId(), UUID.randomUUID(), "ACCEPTED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 친구 요청입니다.");
    }

    @DisplayName("친구요청 acceptStatus 변경 시 수신자 ID가 친구관계 ID와 매칭되지 않을 때 예외가 발생한다.")
    @Test
    void updateAcceptStatusWithMismatchedUserIdFriendShipId(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .receiverId(addedRequester.getId())
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        // when // then
        assertThatThrownBy(() -> friendshipService.updateAcceptStatus(addedReceiver.getId(), friendShip.getFriendShipId(), "ACCEPTED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수신자 ID가 친구관계 ID와 매칭되지 않습니다.");
    }


    @DisplayName("친구 목록 조회에 성공한다")
    @Test
    void getAcceptedFriendList(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .receiverId(addedReceiver.getId())
                .acceptStatus("ACCEPTED")
                .build();
        friendshipRepository.save(friendShip);

        // when
        List<FriendDto> friendListWithRequesterId = friendshipService.getFriendList(addedRequester.getId());
        List<FriendDto> friendListWithReceiverId = friendshipService.getFriendList(addedReceiver.getId());

        // then
        assertThat(friendListWithRequesterId).isNotEmpty();
        assertThat(friendListWithRequesterId).hasSize(1);
        assertThat(friendListWithRequesterId.get(0).getFriendId()).isEqualTo(addedReceiver.getId());

        assertThat(friendListWithReceiverId).isNotEmpty();
        assertThat(friendListWithReceiverId).hasSize(1);
        assertThat(friendListWithReceiverId.get(0).getFriendId()).isEqualTo(addedRequester.getId());
    }

    @DisplayName("친구 목록 조회 시 아직 친구요청을 받지 않았을 때 해당 관계의 친구는 반환되지 않는다.")
    @Test
    void getFriendListButPending(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .receiverId(addedReceiver.getId())
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        // when
        List<FriendDto> friendListWithRequesterId = friendshipService.getFriendList(addedRequester.getId());
        List<FriendDto> friendListWithReceiverId = friendshipService.getFriendList(addedReceiver.getId());

        // then
        assertThat(friendListWithRequesterId).isEmpty();
        assertThat(friendListWithReceiverId).isEmpty();
    }

    @DisplayName("친구 목록 조회할 때 친구가 없으면(FriendShip이 없으면) 빈 리스트를 반환한다.")
    @Test
    void getFriendListButEmpty(){
        // given
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedUser);

        // when
        List<FriendDto> friendListWithRequesterId = friendshipService.getFriendList(addedUser.getId());

        // then
        assertThat(friendListWithRequesterId).isEmpty();
    }

    @DisplayName("친구 목록 조회할 때 잘못된 유저id를 전달하면 예외가 발생한다.")
    @Test
    void getFriendListWithWorngUserId(){
        // given
        User addedRequester = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        User addedReceiver = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinseo")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedRequester);
        userRepository.save(addedReceiver);

        FriendShip friendShip = FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(addedRequester.getId())
                .receiverId(addedReceiver.getId())
                .acceptStatus("PENDING")
                .build();
        friendshipRepository.save(friendShip);

        // when // then
        assertThatThrownBy(() -> friendshipService.getFriendList(addedRequester.getId() + 123456789))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");
    }

}