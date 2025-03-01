package devkor.ontime_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.ControllerTestSupport;
import devkor.ontime_back.TestSecurityConfig;
import devkor.ontime_back.dto.PreparationDto;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
public class PreparationUserControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("준비과정 수정에 성공한다.")
    void modifyPreparationUser_success() throws Exception {
        // given
        Long userId = 1L;
        UUID preparationUser1Id = UUID.randomUUID();
        UUID preparationUser2Id = UUID.randomUUID();

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationUser1Id, "세면", 10, preparationUser2Id),
                new PreparationDto(preparationUser2Id, "옷입기", 15, null)
        );

        when(userAuthService.getUserIdFromToken(any(HttpServletRequest.class))).thenReturn(userId);

        // when & then
        mockMvc.perform(post("/preparationuser/modify")
                        .header("Authorization", "Bearer test-token")
                        .content(objectMapper.writeValueAsString(preparationDtoList))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("OK"));

        verify(userAuthService, times(1)).getUserIdFromToken(any(HttpServletRequest.class));
        verify(preparationUserService, times(1))
                .updatePreparationUsers(eq(userId), argThat(list -> list.size() == preparationDtoList.size()));
    }


    @Test
    @DisplayName("준비과정 조회 요청에 성공한다.")
    void getAllPreparationUser_success() throws Exception {
        // given
        Long userId = 1L;
        UUID preparationUser1Id = UUID.randomUUID();
        UUID preparationUser2Id = UUID.randomUUID();

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationUser1Id, "세면", 10, preparationUser2Id),
                new PreparationDto(preparationUser2Id, "옷입기", 15, null)
        );

        when(userAuthService.getUserIdFromToken(any(HttpServletRequest.class))).thenReturn(userId);
        when(preparationUserService.showAllPreparationUsers(userId)).thenReturn(preparationDtoList);

        // when & then
        mockMvc.perform(get("/preparationuser/show/all")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].preparationId").value(preparationUser1Id.toString()))
                .andExpect(jsonPath("$.data[0].preparationName").value("세면"))
                .andExpect(jsonPath("$.data[1].preparationId").value(preparationUser2Id.toString()))
                .andExpect(jsonPath("$.data[1].preparationName").value("옷입기"));

        verify(userAuthService, times(1)).getUserIdFromToken(any(HttpServletRequest.class));
        verify(preparationUserService, times(1)).showAllPreparationUsers(userId);
    }


}
