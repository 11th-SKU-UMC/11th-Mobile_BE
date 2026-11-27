package com.umc.movie.api;

import com.umc.movie.entity.Member;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 평점 등록 API가 토큰 유무에 따라 인터셉터에서 막히거나 통과하는지 실제 요청으로 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RatingApiTest {

    private static final String RATING_BODY = "{\"movieId\": 100, \"score\": 5, \"comment\": \"최고의 영화\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(Member.builder()
                .email("rating@test.com")
                .password("hashed")
                .nickname("ratetester")
                .build());
        bearerToken = "Bearer " + jwtUtil.createAccessToken(member.getMemberId(), member.getEmail());
    }

    @Test
    @DisplayName("토큰 없이 평점을 등록하면 401(AUTH401_2)을 돌려준다")
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RATING_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH401_2"));
    }

    @Test
    @DisplayName("토큰을 담아 평점을 등록하면 201과 등록한 별점을 돌려준다")
    void create_withToken_returns201() throws Exception {
        mockMvc.perform(post("/ratings")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RATING_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result.movieId").value(100))
                .andExpect(jsonPath("$.result.score").value(5));
    }
}
