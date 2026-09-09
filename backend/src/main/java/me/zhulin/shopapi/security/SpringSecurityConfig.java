package me.zhulin.shopapi.security;

import me.zhulin.shopapi.security.JWT.JwtEntryPoint;
import me.zhulin.shopapi.security.JWT.JwtFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.sql.DataSource;

/**
 * Created By Zhu Lin on 1/1/2019.
 *
 * Migrado a Spring Security 6/7: WebSecurityConfigurerAdapter fue eliminado,
 * la configuracion se expone ahora como beans (SecurityFilterChain,
 * UserDetailsService y AuthenticationManager).
 */
@Configuration
@EnableWebSecurity
@DependsOn("passwordEncoder")
public class SpringSecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtEntryPoint accessDenyHandler;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    @Value("${spring.queries.users-query}")
    private String usersQuery;

    @Value("${spring.queries.roles-query}")
    private String rolesQuery;

    public SpringSecurityConfig(JwtFilter jwtFilter,
                                JwtEntryPoint accessDenyHandler,
                                PasswordEncoder passwordEncoder,
                                DataSource dataSource) {
        this.jwtFilter = jwtFilter;
        this.accessDenyHandler = accessDenyHandler;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
    }

    /** Reemplazo de auth.jdbcAuthentication() */
    @Bean
    public UserDetailsService userDetailsService() {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
        manager.setUsersByUsernameQuery(usersQuery);
        manager.setAuthoritiesByUsernameQuery(rolesQuery);
        return manager;
    }

    /** Reemplazo de authenticationManagerBean() */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {
                })
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/profile/**").authenticated()
                        .requestMatchers("/cart/**").hasAnyRole("CUSTOMER")
                        .requestMatchers("/order/finish/**").hasAnyRole("EMPLOYEE", "MANAGER")
                        .requestMatchers("/order/**").authenticated()
                        .requestMatchers("/profiles/**").authenticated()
                        .requestMatchers("/seller/product/new").hasAnyRole("MANAGER")
                        // PathPatternParser (Spring 6+) no admite ** en medio del patron:
                        // "/seller/**/delete" -> el unico endpoint es /seller/product/{id}/delete
                        .requestMatchers("/seller/product/*/delete").hasAnyRole("MANAGER")
                        .requestMatchers("/seller/**").hasAnyRole("EMPLOYEE", "MANAGER")
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(accessDenyHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
