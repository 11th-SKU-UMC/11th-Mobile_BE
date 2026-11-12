package com.umc.movie;

import com.umc.movie.entity.Member;
import com.umc.movie.entity.Rating;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import com.umc.movie.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MissionApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired RatingRepository ratings;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtil jwtUtil;

    @BeforeEach
    void clearDatabase() {
        ratings.deleteAll();
        members.deleteAll();
    }

    @Test
    void signUpStoresOnlyBcryptAndNeverReturnsPassword() throws Exception {
        postMember("umc@example.com", "Umc!1234", "moviefan")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result.memberId").isNumber())
                .andExpect(jsonPath("$.result.password").doesNotExist());
        Member saved = members.findAll().getFirst();
        assertThat(saved.getPassword()).hasSize(60).isNotEqualTo("Umc!1234");
        assertThat(encoder.matches("Umc!1234", saved.getPassword())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"email\":\"bad\",\"password\":\"Umc!1234\",\"nickname\":\"valid\"}",
            "{\"email\":\"\",\"password\":\"Umc!1234\",\"nickname\":\"valid\"}",
            "{\"password\":\"Umc!1234\",\"nickname\":\"valid\"}",
            "{\"email\":\"ok@example.com\",\"password\":\"umc12345\",\"nickname\":\"valid\"}",
            "{\"email\":\"ok@example.com\",\"nickname\":\"valid\"}",
            "{\"email\":\"ok@example.com\",\"password\":\"Umc!1234\",\"nickname\":\"a\"}",
            "{\"email\":\"ok@example.com\",\"password\":\"Umc!1234\",\"nickname\":\"1234567890123\"}",
            "{\"email\":\"ok@example.com\",\"password\":\"Umc!1234\",\"nickname\":\"  \"}"
    })
    void invalidMemberInputReturns400(String json) throws Exception {
        mvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON4001"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"result\":null")));
        assertThat(members.count()).isZero();
    }

    @Test
    void bcryptByteLimitIncludesMultibyteCharacters() throws Exception {
        postMember("long@example.com", "Aa1!" + "가".repeat(23), "longpw")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다."));
        postMember("ok@example.com", "Aa1!" + "x".repeat(68), "okpw")
                .andExpect(status().isCreated());
    }

    @Test
    void duplicateEmailAndNicknameHaveDifferentCodes() throws Exception {
        postMember("taken@example.com", "Umc!1234", "taken").andExpect(status().isCreated());
        postMember("taken@example.com", "Umc!1234", "other")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBER4091"));
        postMember("other@example.com", "Umc!1234", "taken")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBER409_1"));
        assertThat(members.count()).isEqualTo(1);
    }

    @Test
    void ratingLifecycleChecksPersistenceAnd200Envelope() throws Exception {
        Member owner = member("owner");
        String base = "/ratings";
        mvc.perform(post(base).header("Authorization", token(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":550,\"score\":5,\"comment\":\"좋아요\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("COMMON201"));
        Long ratingId = ratings.findAll().getFirst().getRatingId();
        mvc.perform(patch(base + "/" + ratingId).header("Authorization", token(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":3,\"comment\":\"수정한 후기\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("COMMON200"));
        mvc.perform(get("/members/{id}/ratings", owner.getMemberId())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(3))
                .andExpect(jsonPath("$[0].comment").value("수정한 후기"));
        mvc.perform(delete(base + "/" + ratingId).header("Authorization", token(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"result\":null")));
        mvc.perform(get("/members/{id}/ratings", owner.getMemberId()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        assertThat(ratings.count()).isZero();
    }

    @Test
    void duplicateRatingIs409AndNotStoredTwice() throws Exception {
        Member owner = member("owner");
        Rating rating = rating(owner);
        mvc.perform(post("/ratings").header("Authorization", token(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":550,\"score\":4}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RATING409_1"));
        assertThat(ratings.count()).isEqualTo(1);
        assertThat(ratings.findById(rating.getRatingId()).orElseThrow().getScore()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"movieId\":550}", "{\"movieId\":0,\"score\":3}",
            "{\"movieId\":-1,\"score\":3}", "{\"movieId\":550,\"score\":0}",
            "{\"movieId\":550,\"score\":6}", "{\"movieId\":550,\"score\":1.5}",
            "{\"movieId\":1.5,\"score\":3}"})
    void invalidRatingInputReturns400(String json) throws Exception {
        Member owner = member("owner");
        mvc.perform(post("/ratings").header("Authorization", token(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false));
        assertThat(ratings.count()).isZero();
    }

    @Test
    void commentLengthIsCheckedOnCreateAndUpdate() throws Exception {
        Member owner = member("owner");
        String base = "/ratings";
        mvc.perform(post(base).header("Authorization", token(owner)).contentType(MediaType.APPLICATION_JSON).content(
                        "{\"movieId\":550,\"score\":5,\"comment\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        Rating rating = rating(owner);
        mvc.perform(patch(base + "/" + rating.getRatingId()).header("Authorization", token(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":2,\"comment\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        assertThat(ratings.findById(rating.getRatingId()).orElseThrow().getScore()).isEqualTo(5);
    }

    @Test
    void patchRequiresScoreAndCanClearComment() throws Exception {
        Member owner = member("owner");
        Rating rating = rating(owner);
        String path = "/ratings/" + rating.getRatingId();
        mvc.perform(patch(path).header("Authorization", token(owner)).contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"only\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(path).header("Authorization", token(owner)).contentType(MediaType.APPLICATION_JSON).content("{\"score\":1}"))
                .andExpect(status().isOk());
        assertThat(ratings.findById(rating.getRatingId()).orElseThrow().getComment()).isNull();
    }

    @Test
    void anotherMembersUpdateAndDeleteAre403WithoutChangingData() throws Exception {
        Member owner = member("owner");
        Member other = member("other");
        Rating rating = rating(owner);
        String path = "/ratings/" + rating.getRatingId();
        mvc.perform(patch(path).header("Authorization", token(other)).contentType(MediaType.APPLICATION_JSON).content("{\"score\":1}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("RATING403_1"));
        mvc.perform(delete(path).header("Authorization", token(other))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RATING403_1"));
        assertThat(ratings.findById(rating.getRatingId()).orElseThrow().getScore()).isEqualTo(5);
    }

    @Test
    void missingRatingIs404ForBothUpdateAndDelete() throws Exception {
        Member owner = member("owner");
        String path = "/ratings/999999";
        mvc.perform(patch(path).header("Authorization", token(owner)).contentType(MediaType.APPLICATION_JSON).content("{\"score\":3}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RATING404_1"));
        mvc.perform(delete(path).header("Authorization", token(owner))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RATING404_1"));
    }

    @Test
    void missingMemberIs404() throws Exception {
        mvc.perform(post("/ratings").header("Authorization", "Bearer " + jwtUtil.createAccessToken(999999L, "missing@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":550,\"score\":3}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MEMBER404_1"));
        mvc.perform(get("/members/999999/ratings")).andExpect(status().isNotFound());
    }

    @Test
    void malformedJsonUnknownFieldAndPathTypeUseErrorEnvelope() throws Exception {
        for (String json : new String[]{"{", "{\"email\":\"x@example.com\",\"password\":\"Umc!1234\",\"nickname\":\"valid\",\"role\":\"ADMIN\"}"}) {
            mvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false));
        }
        mvc.perform(get("/members/abc/ratings")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    void previousWeekReadApisStillWork() throws Exception {
        member("existing");
        mvc.perform(get("/members/email/existing@example.com"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
        mvc.perform(get("/members/nickname/existing"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
    }

    private ResultActions postMember(String email, String password, String nickname) throws Exception {
        return mvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content(
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"nickname\":\"" + nickname + "\"}"));
    }

    private Member member(String nickname) {
        return members.saveAndFlush(new Member(nickname + "@example.com", encoder.encode("Umc!1234"), nickname));
    }

    private Rating rating(Member owner) {
        return ratings.saveAndFlush(new Rating(owner, 550L, 5, "original"));
    }

    private String token(Member member) {
        return "Bearer " + jwtUtil.createAccessToken(member.getMemberId(), member.getEmail());
    }
}
