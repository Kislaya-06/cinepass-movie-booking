package com.example.moviereservation;

import com.example.moviereservation.entity.Movie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
class HomePageTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @SuppressWarnings("unchecked")
    void noMovieIsListedInBothRows() throws Exception {
        Map<String, Object> model = mockMvc.perform(get("/")).andReturn().getModelAndView().getModel();
        List<Movie> nowShowing = (List<Movie>) model.get("nowShowing");
        List<Movie> comingSoon = (List<Movie>) model.get("comingSoon");

        assertThat(nowShowing).isNotEmpty();
        assertThat(Collections.disjoint(nowShowing, comingSoon)).isTrue();
    }
}
