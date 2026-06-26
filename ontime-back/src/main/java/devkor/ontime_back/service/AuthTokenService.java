package devkor.ontime_back.service;

import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserRefreshToken;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.UserRefreshTokenRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.InvalidRefreshTokenException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public AuthTokens issueLoginTokens(User user, HttpServletResponse response) {
        userRefreshTokenRepository.deleteByUser(user);

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken();

        jwtTokenProvider.sendAccessAndRefreshToken(response, accessToken, refreshToken);
        user.updateAccessToken(accessToken);
        user.updateRefreshToken(refreshToken);
        userRefreshTokenRepository.save(UserRefreshToken.create(user, refreshToken));

        return new AuthTokens(accessToken, refreshToken);
    }

    @Transactional
    public AuthTokens rotateRefreshToken(String refreshToken, HttpServletResponse response) {
        UserRefreshToken storedToken = userRefreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid Refresh token!~!"));
        User user = storedToken.getUser();

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId());
        String newRefreshToken = jwtTokenProvider.createRefreshToken();

        jwtTokenProvider.sendAccessAndRefreshToken(response, accessToken, newRefreshToken);
        user.updateAccessToken(accessToken);
        user.updateRefreshToken(newRefreshToken);
        storedToken.rotate(newRefreshToken);
        userRepository.saveAndFlush(user);

        return new AuthTokens(accessToken, newRefreshToken);
    }

    public record AuthTokens(String accessToken, String refreshToken) {
    }
}
