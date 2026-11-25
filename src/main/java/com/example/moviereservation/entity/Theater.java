package com.example.moviereservation.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "theaters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Theater extends BaseEntity {

    @NotBlank(message = "Theater name is required")
    @Size(max = 150, message = "Theater name must not exceed 150 characters")
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    @Column(length = 255)
    private String location;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String city;

    @OneToMany(mappedBy = "theater", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Screen> screens = new ArrayList<>();

    public void addScreen(Screen screen) {
        screens.add(screen);
        screen.setTheater(this);
    }

    public void removeScreen(Screen screen) {
        screens.remove(screen);
        screen.setTheater(null);
    }
}
