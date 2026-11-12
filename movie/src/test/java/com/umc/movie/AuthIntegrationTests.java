package com.umc.movie;

import com.umc.movie.entity.Member;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import com.umc.movie.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired RatingRepository ratings;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtil jwtUtil;
    @Autowired ObjectMapper mapper;
    @Value("${jwt.secret}") String secret;

    @BeforeEach
    void clearDatabase() {
        ratings.deleteAll();
        members.deleteAll();
    }

    @Test
    void loginIssuesVerifiableTokenAndMeUsesItsSubject() throws Exception {
        Member member = member("owner", "Umc!1234");
        String json = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner@example.com\",\"password\":\"Umc!1234\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.nickname").value("owner"))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(json).path("result").path("accessToken").asText();
        assertThat(jwtUtil.getMemberId(token)).isEqualTo(member.getMemberId());
        var claims = mapper.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertThat(claims.path("sub").asText()).isEqualTo(member.getMemberId().toString());
        assertThat(claims.path("exp").asLong() - claims.path("iat").asLong()).isEqualTo(3600);
        assertThat(claims.has("password")).isFalse();
        mvc.perform(get("/members/me").queryParam("memberId", "99999").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.email").value(member.getEmail()))
                .andExpect(jsonPath("$.result.createdAt").exists())
                .andExpect(jsonPath("$.result.password").doesNotExist());
    }

    @Test
    void missingEmailAndWrongPasswordHaveExactlyTheSameResponse() throws Exception {
        member("owner", "Umc!1234");
        String wrongPassword = loginFailure("owner@example.com", "wrong");
        assertThat(loginFailure("missing@example.com", "wrong")).isEqualTo(wrongPassword);
    }

    @Test
    void loginDoesNotApplyNewSignUpPasswordPolicyToExistingAccounts() throws Exception {
        member("legacy", "old");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"legacy@example.com\",\"password\":\"old\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.accessToken").isString());
    }

    @Test
    void everyProtectedOperationRejectsMissingToken() throws Exception {
        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[]{
                get("/members/me"), patch("/members/me"), post("/ratings"), patch("/ratings/1"), delete("/ratings/1")}) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH401_2"))
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
        assertThat(ratings.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic abc", "Bearer", "Token abc"})
    void wrongAuthenticationSchemeIsMissingCredentials(String header) throws Exception {
        mvc.perform(get("/members/me").header("Authorization", header))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH401_2"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not.a.jwt", "", "broken"})
    void malformedTokenIsRejected(String token) throws Exception {
        mvc.perform(get("/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH401_3"));
    }

    @Test
    void changingPayloadOrSigningWithAnotherKeyIsRejected() throws Exception {
        Member member = member("owner", "Umc!1234");
        String token = jwtUtil.createAccessToken(member.getMemberId(), member.getEmail());
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        parts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString(
                payload.replace("owner@example.com", "other@example.com").getBytes(StandardCharsets.UTF_8));
        String foreignToken = new JwtUtil("another-test-only-signing-key-at-least-32-bytes", 3600, 0)
                .createAccessToken(member.getMemberId(), member.getEmail());
        for (String invalid : new String[]{String.join(".", parts), foreignToken}) {
            mvc.perform(get("/members/me").header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH401_3"));
        }
    }

    @Test
    void expiredTokenHasDedicatedCode() throws Exception {
        String expired = Jwts.builder().subject("1").expiration(Date.from(Instant.now().minusSeconds(1)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/members/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH401_4"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
    void invalidSubjectDoesNotBecomeAServerError(String subject) throws Exception {
        String token = Jwts.builder().subject(subject).expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH401_3"));
    }

    @Test
    void nicknameUpdateAllowsOwnNameAndRejectsOtherMembersName() throws Exception {
        Member owner = member("owner", "Umc!1234");
        member("other", "Umc!1234");
        String auth = "Bearer " + jwtUtil.createAccessToken(owner.getMemberId(), owner.getEmail());
        for (String name : new String[]{"owner", "changed"}) {
            mvc.perform(patch("/members/me").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"" + name + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result.nickname").value(name));
            assertThat(members.findById(owner.getMemberId()).orElseThrow().getNickname()).isEqualTo(name);
        }
        mvc.perform(patch("/members/me").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"other\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBER409_1"));
        assertThat(members.findById(owner.getMemberId()).orElseThrow().getNickname()).isEqualTo("changed");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"nickname\":\"\"}", "{\"nickname\":\" \"}",
            "{\"nickname\":\"a\"}", "{\"nickname\":\"1234567890123\"}"})
    void invalidNicknameCannotChangeMember(String json) throws Exception {
        Member owner = member("owner", "Umc!1234");
        mvc.perform(patch("/members/me").header("Authorization", "Bearer " + jwtUtil.createAccessToken(owner.getMemberId(), owner.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        assertThat(members.findById(owner.getMemberId()).orElseThrow().getNickname()).isEqualTo("owner");
    }

    @Test
    void clientCannotChooseRatingAuthorThroughBody() throws Exception {
        Member owner = member("owner", "Umc!1234");
        String token = "Bearer " + jwtUtil.createAccessToken(owner.getMemberId(), owner.getEmail());
        mvc.perform(post("/ratings").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":999,\"movieId\":550,\"score\":5}"))
                .andExpect(status().isBadRequest());
        assertThat(ratings.count()).isZero();
        mvc.perform(post("/ratings").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":550,\"score\":5}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.result.ratingId").isNumber());
        assertThat(ratings.findAll().getFirst().getMember().getMemberId()).isEqualTo(owner.getMemberId());
    }

    @Test
    void corsPreflightDoesNotRequireAuthentication() throws Exception {
        mvc.perform(options("/ratings").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private String loginFailure(String email, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH401_1"))
                .andReturn().getResponse().getContentAsString();
    }

    private Member member(String nickname, String password) {
        return members.saveAndFlush(new Member(nickname + "@example.com", encoder.encode(password), nickname));
    }
}
