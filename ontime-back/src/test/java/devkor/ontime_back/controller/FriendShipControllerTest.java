package devkor.ontime_back.controller;

import devkor.ontime_back.ControllerTestSupport;
import devkor.ontime_back.TestSecurityConfig;
import devkor.ontime_back.dto.FriendDto;
import devkor.ontime_back.dto.UpdateSpareTimeDto;
import devkor.ontime_back.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
class FriendShipControllerTest extends ControllerTestSupport {

    @DisplayName("친구추가 링크 생성에 성공한다.")
    @Test
    void createFriendShipLink() throws Exception {
        // given
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);

        UUID uuid = UUID.randomUUID();
        when(friendshipService.createFriendShipLink(any())).thenReturn(String.valueOf(uuid));


        // when // then
        mockMvc.perform(
                        post("/friends/links")
                                .content("")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("친구추가 링크 생성 및 반환 성공. 반환되는 UUID로 친구추가 요청 데이터를 조회하고, 친구추가 요청을 수락하거나 거절할 수 있음."))
                .andExpect(jsonPath("$.data.friendShipId").value(uuid.toString()));
    }

    @DisplayName("친구추가 요청자 조회에 성공한다")
    @Test
    void getFriendShipRequester() throws Exception {
        // given
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);

        User addedUser = User.builder()
                .id(1L)
                .name("junbeom")
                .email("user@example.com")
                .build();
        when(friendshipService.getFriendShipRequester(any(Long.class), any(UUID.class))).thenReturn(addedUser);


        // when // then
        mockMvc.perform(
                        get("/friends/" + UUID.randomUUID() + "/requests")
                                .content("")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("친구추가 요청자 조회 성공"))
                .andExpect(jsonPath("$.data.requesterId").value(1))
                .andExpect(jsonPath("$.data.requesterName").value("junbeom"))
                .andExpect(jsonPath("$.data.requesterEmail").value("user@example.com"));
    }

    @DisplayName("친구추가 요청 수락에 성공한다")
    @Test
    void updateAcceptStatusToAccepted() throws Exception {
        // given
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        doNothing().when(friendshipService).updateAcceptStatus(any(Long.class), any(UUID.class), any(String.class));

        // when // then
        mockMvc.perform(
                        post("/friends/" + UUID.randomUUID() + "/approve")
                                .content("{\"acceptStatus\": \"ACCEPTED\"}")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("친구추가 요청 수락 성공"));
    }

    @DisplayName("친구추가 요청 거절에 성공한다")
    @Test
    void updateAcceptStatusToREJECTED() throws Exception {
        // given
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        doNothing().when(friendshipService).updateAcceptStatus(any(Long.class), any(UUID.class), any(String.class));

        // when // then
        mockMvc.perform(
                        post("/friends/" + UUID.randomUUID() + "/approve")
                                .content("{\"acceptStatus\": \"REJECTED\"}")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("친구추가 요청 거절 성공"));
    }

    @DisplayName("친구 목록 조회에 성공한다")
    @Test
    void getFriendList() throws Exception {
        // given
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);

        List<FriendDto> mockFriendList = List.of(
                FriendDto.builder()
                        .friendId(2L)
                        .friendName("friend1")
                        .friendEmail("friend1@example.com")
                        .build(),

                FriendDto.builder()
                        .friendId(3L)
                        .friendName("friend2")
                        .friendEmail("friend2@example.com")
                        .build()
        );
        when(friendshipService.getFriendList(any(Long.class))).thenReturn(mockFriendList);

        // when // then
        mockMvc.perform(
                        get("/friends")
                                .content("")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("친구 목록 조회 성공"))
                .andExpect(jsonPath("$.data.friendsList").isArray())
                .andExpect(jsonPath("$.data.friendsList").isNotEmpty())
                .andExpect(jsonPath("$.data.friendsList[0].friendId").value(2))
                .andExpect(jsonPath("$.data.friendsList[0].friendName").value("friend1"))
                .andExpect(jsonPath("$.data.friendsList[0].friendEmail").value("friend1@example.com"))
                .andExpect(jsonPath("$.data.friendsList[1].friendId").value(3))
                .andExpect(jsonPath("$.data.friendsList[1].friendName").value("friend2"))
                .andExpect(jsonPath("$.data.friendsList[1].friendEmail").value("friend2@example.com"));
    }
}