package com.example.moviereservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One cast or crew entry from TMDb: role is the character for cast, the job for crew. */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MovieCredit {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String role;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(nullable = false)
    private boolean crew;
}
