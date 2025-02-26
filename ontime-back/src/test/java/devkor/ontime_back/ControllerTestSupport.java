package devkor.ontime_back;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.controller.*;
import devkor.ontime_back.global.generallogin.handler.LoginSuccessHandler;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = {
                UserAuthController.class,
                UserController.class,
                ScheduleController.class,
                FriendShipController.class,
                PreparationUserController.class
        }
)
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected UserAuthService userAuthService;

    @MockBean
    protected UserService userService;

    @MockBean
    protected ScheduleService scheduleService;

    @MockBean
    protected FriendshipService friendshipService;

    @MockBean
    protected PreparationUserService preparationUserService;

    @MockBean
    protected UserRepository userRepository;

    @MockBean
    protected AuthenticationManager authenticationManager;

    @MockBean
    protected LoginSuccessHandler loginSuccessHandler;

}
