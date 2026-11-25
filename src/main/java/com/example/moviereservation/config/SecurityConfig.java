package com.example.moviereservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            )
            .authorizeHttpRequests(auth -> auth
                // Publicly accessible paths
                .requestMatchers(
                    "/",
                    "/movies/**",
                    "/login",
                    "/register",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/error-page",
                    "/verify/**"
                ).permitAll()
                // Admin-only management endpoints (metrics, H2 console in dev)
                .requestMatchers("/admin/**", "/actuator/**", "/h2-console/**").hasRole("ADMIN")
                // Admins may pull any ticket PDF (support), but booking itself is a customer job
                .requestMatchers("/reservations/*/ticket").authenticated()
                .requestMatchers(
                    "/showtimes/*/seats",
                    "/reservations/**",
                    "/my-bookings"
                ).hasRole("USER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin()) // H2 console frames itself; block other sites (clickjacking)
            );

        return http.build();
    }

    /** Admins land on the dashboard; customers go back to the page that asked them to sign in. */
    private org.springframework.security.web.authentication.AuthenticationSuccessHandler loginSuccessHandler() {
        var customerHandler = new org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler();
        customerHandler.setDefaultTargetUrl("/");
        return (request, response, authentication) -> {
            boolean admin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            if (admin) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
            } else {
                customerHandler.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }
}
