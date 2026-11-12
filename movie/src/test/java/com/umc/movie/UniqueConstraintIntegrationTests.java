package com.umc.movie;

import com.umc.movie.controller.GlobalExceptionHandler;
import com.umc.movie.entity.Member;
import com.umc.movie.entity.Rating;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UniqueConstraintIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired RatingRepository ratings;
    @Autowired GlobalExceptionHandler handler;
    @MockitoSpyBean PasswordEncoder encoder;

    @BeforeEach
    void clearDatabase() {
        ratings.deleteAll();
        members.deleteAll();
    }

    @Test
    void concurrentSignUpsPassingPrechecksStillRejectDuplicateEmailAtDatabase() throws Exception {
        race("email", "same@example.com", "first", "same@example.com", "second", "MEMBER4091");
    }

    @Test
    void concurrentSignUpsPassingPrechecksStillRejectDuplicateNicknameAtDatabase() throws Exception {
        race("nickname", "first@example.com", "same", "second@example.com", "same", "MEMBER409_1");
    }

    @Test
    void databaseAlsoRejectsDuplicateRatingAndHandlerMapsNamedConstraint() {
        Member member = members.saveAndFlush(new Member("rating@example.com", encoder.encode("Umc!1234"), "rating"));
        ratings.saveAndFlush(new Rating(member, 550L, 5, "first"));
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> ratings.saveAndFlush(new Rating(member, 550L, 1, "duplicate")));
        var response = handler.handleDataIntegrity(exception);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("RATING409_1");
        assertThat(ratings.count()).isEqualTo(1);
    }

    private void race(String name, String emailA, String nicknameA, String emailB, String nicknameB,
                      String expectedCode) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        // encode는 두 중복 조회가 모두 끝난 뒤 호출됩니다. 여기서 두 요청을 만나게 합니다.
        // 조회 결과나 INSERT는 모킹하지 않고 실제 H2 DB를 사용합니다.
        doAnswer(invocation -> {
            barrier.await(15, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(encoder).encode(eq("Race!1234"));

        List<MvcResult> results;
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<MvcResult> a = executor.submit(() -> signUp(emailA, nicknameA));
            Future<MvcResult> b = executor.submit(() -> signUp(emailB, nicknameB));
            results = List.of(a.get(25, TimeUnit.SECONDS), b.get(25, TimeUnit.SECONDS));
        }
        assertThat(results.stream().map(result -> result.getResponse().getStatus()).sorted().toList())
                .containsExactly(201, 409);
        MvcResult conflict = results.stream().filter(result -> result.getResponse().getStatus() == 409)
                .findFirst().orElseThrow();
        assertThat(conflict.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(expectedCode);
        assertThat(members.count()).isEqualTo(1);

        Path evidence = Path.of("build", "mission-evidence", "db-unique-" + name + ".txt");
        Files.createDirectories(evidence.getParent());
        StringBuilder record = new StringBuilder("Two real concurrent POST /members requests; both prechecks passed.\n");
        for (MvcResult result : results) {
            record.append("REQUEST ").append(result.getRequest().getContentAsString()).append("\n")
                    .append("HTTP ").append(result.getResponse().getStatus()).append("\n")
                    .append(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).append("\n\n");
        }
        record.append("Persisted member count: ").append(members.count()).append("\n");
        Files.writeString(evidence, record, StandardCharsets.UTF_8);
    }

    private MvcResult signUp(String email, String nickname) throws Exception {
        return mvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content(
                "{\"email\":\"" + email + "\",\"password\":\"Race!1234\",\"nickname\":\"" + nickname + "\"}"))
                .andReturn();
    }
}
