package com.livraison.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Securite applicative.
 *
 * Deux chaines de filtres :
 *  - /api/** : authentification requise, HTTP Basic autorise, CSRF desactive
 *    (les appels fetch de l'interface reutilisent la session deja ouverte) ;
 *  - le reste : formulaire de connexion, CSRF actif, restrictions par role.
 *
 * Regles de role :
 *  - ADMIN      : tout (baremes, gestion des comptes) ;
 *  - DISPATCHER : exploitation (clients, livreurs, colis, affectation, stats) ;
 *  - LIVREUR    : commandes, suivi, incidents (pas de configuration).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/utilisateurs/**", "/tarifs/**").hasRole("ADMIN")
                        .requestMatchers("/clients/**", "/livreurs/**", "/colis/**",
                                "/statistiques/**", "/incidents/delete/**",
                                "/commandes/new", "/commandes/save", "/commandes/edit/**",
                "/commandes/delete/**", "/commandes/recalculer-prix/**",
                "/commandes/affecter/**", "/commandes/affecter-auto/**")
                .hasAnyRole("ADMIN", "DISPATCHER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/commandes", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }
}
