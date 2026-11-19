package com.umc.movie;

import com.umc.movie.controller.GlobalExceptionHandler;
import com.umc.movie.entity.Favorite;
import com.umc.movie.entity.Member;
import com.umc.movie.repository.FavoriteRepository;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import com.umc.movie.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:week08-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MovieRecordIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired RatingRepository ratings;
    @Autowired FavoriteRepository favorites;
    @Autowired GlobalExceptionHandler exceptionHandler;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtil jwtUtil;

    private Member owner;
    private String ownerHeader;

    @BeforeEach
    void setUp() {
        favorites.deleteAll();
        ratings.deleteAll();
        members.deleteAll();
        owner = createMember("owner");
        ownerHeader = bearer(owner);
    }

    @Test
    void favoritesArePrivateAndListedNewestFirst() throws Exception {
        String otherHeader = bearer(createMember("other"));
        mvc.perform(get("/favorites/550").header(HttpHeaders.AUTHORIZATION, ownerHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isFavorite").value(false));

        createFavorite(550, ownerHeader).andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.favoriteId").isNumber())
                .andExpect(jsonPath("$.result.createdAt").isString());
        createFavorite(238, ownerHeader).andExpect(status().isCreated());
        createFavorite(550, ownerHeader).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FAVORITE409_1"));

        mvc.perform(get("/favorites").header(HttpHeaders.AUTHORIZATION, ownerHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[*].movieId").value(org.hamcrest.Matchers.contains(238, 550)));
        mvc.perform(get("/favorites").queryParam("memberId", owner.getMemberId().toString())
                        .header(HttpHeaders.AUTHORIZATION, otherHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isEmpty());
        mvc.perform(delete("/favorites/550").header(HttpHeaders.AUTHORIZATION, otherHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAVORITE404_1"));
        createFavorite(550, otherHeader).andExpect(status().isCreated());

        mvc.perform(delete("/favorites/550").header(HttpHeaders.AUTHORIZATION, ownerHeader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result").hasJsonPath())
                .andExpect(jsonPath("$.result").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/favorites/550").header(HttpHeaders.AUTHORIZATION, otherHeader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.isFavorite").value(true));
    }

    @Test
    void favoriteAndMovieRecordEndpointsRequireTokenAndValidateInput() throws Exception {
        for (var request : java.util.List.of(
                get("/favorites"), get("/favorites/550"), delete("/favorites/550"),
                post("/favorites"), post("/movie-records"))) {
            mvc.perform(request).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH401_2"));
        }

        createFavorite(0, ownerHeader).andExpect(status().isBadRequest());
        mvc.perform(get("/favorites/0").header(HttpHeaders.AUTHORIZATION, ownerHeader))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/favorites").header(HttpHeaders.AUTHORIZATION, ownerHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"movieId":550,"memberId":999}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/movie-records").header(HttpHeaders.AUTHORIZATION, ownerHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"movieId":550,"score":5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void movieRecordSavesBothOnlyWhenRequested() throws Exception {
        recordMovie(550, true).andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.rating.movieId").value(550))
                .andExpect(jsonPath("$.result.favorite.movieId").value(550));
        recordMovie(550, true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING409_1"));

        recordMovie(238, false).andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.rating.movieId").value(238))
                .andExpect(jsonPath("$.result.favorite").hasJsonPath())
                .andExpect(jsonPath("$.result.favorite").value(org.hamcrest.Matchers.nullValue()));

        assertThat(ratings.count()).isEqualTo(2);
        assertThat(favorites.count()).isEqualTo(1);
    }

    @Test
    void existingFavoriteRollsBackAlreadyFlushedRating() throws Exception {
        createFavorite(550, ownerHeader).andExpect(status().isCreated());

        recordMovie(550, true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FAVORITE409_1"));

        assertThat(ratings.existsByMember_MemberIdAndMovieId(owner.getMemberId(), 550L)).isFalse();
        assertThat(favorites.count()).isEqualTo(1);
        recordMovie(550, false).andExpect(status().isCreated());
    }

    @Test
    void databaseUniqueConstraintMapsToFavoriteConflict() {
        favorites.saveAndFlush(new Favorite(owner, 550L));
        var violation = assertThrows(DataIntegrityViolationException.class,
                () -> favorites.saveAndFlush(new Favorite(owner, 550L)));

        var response = exceptionHandler.handleDataIntegrity(violation);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("FAVORITE409_1");
        assertThat(favorites.count()).isEqualTo(1);
    }

    private ResultActions createFavorite(long movieId, String header) throws Exception {
        return mvc.perform(post("/favorites").header(HttpHeaders.AUTHORIZATION, header)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"movieId":%d}
                        """.formatted(movieId)));
    }

    private ResultActions recordMovie(long movieId, boolean addToFavorites) throws Exception {
        return mvc.perform(post("/movie-records").header(HttpHeaders.AUTHORIZATION, ownerHeader)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"movieId":%d,"score":5,"comment":"다시 보고 싶은 영화입니다.","addToFavorites":%b}
                        """.formatted(movieId, addToFavorites)));
    }

    private Member createMember(String nickname) {
        return members.saveAndFlush(new Member(nickname + "@example.com", encoder.encode("OnlyTest!123"), nickname));
    }

    private String bearer(Member member) {
        return "Bearer " + jwtUtil.createAccessToken(member.getMemberId(), member.getEmail());
    }
}
