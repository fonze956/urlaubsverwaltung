package org.synyx.urlaubsverwaltung.security;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.synyx.urlaubsverwaltung.person.PersonService;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.security.config.http.SessionCreationPolicy.NEVER;
import static org.synyx.urlaubsverwaltung.person.Role.BOSS;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.USER;

@Configuration
@EnableMethodSecurity
class SecurityWebConfiguration {

    private final PersonService personService;
    private final SessionService sessionService;
    private final OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    SecurityWebConfiguration(PersonService personService, SessionService sessionService,
                             OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler,
                             ClientRegistrationRepository clientRegistrationRepository) {
        this.personService = personService;
        this.sessionService = sessionService;
        this.oidcClientInitiatedLogoutSuccessHandler = oidcClientInitiatedLogoutSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    @Order(2)
    SecurityFilterChain actuatorSecurityFilterChain(final HttpSecurity http) throws Exception {
        return http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(
                sessionManagement -> sessionManagement.sessionCreationPolicy(NEVER)
            )
            .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain calendarSecurityFilterChain(final HttpSecurity http) throws Exception {
        return http
            .securityMatchers(configurer ->
                configurer
                    .requestMatchers(GET, "/web/company/persons/*/calendar")
                    .requestMatchers(GET, "/web/departments/*/persons/*/calendar")
                    .requestMatchers(GET, "/web/persons/*/calendar")
            )
            .authorizeHttpRequests(requests ->
                requests
                    .requestMatchers(GET, "/web/company/persons/*/calendar").permitAll()
                    .requestMatchers(GET, "/web/departments/*/persons/*/calendar").permitAll()
                    .requestMatchers(GET, "/web/persons/*/calendar").permitAll()
                    .anyRequest().authenticated()
            )
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(
                sessionManagement -> sessionManagement.sessionCreationPolicy(NEVER)
            )
            .build();
    }

    @Bean
    @Order(4)
    SecurityFilterChain webSecurityFilterChain(final HttpSecurity http, DelegatingSecurityContextRepository securityContextRepository) throws Exception {
        return http
            .authorizeHttpRequests(requests ->
                requests
                    // Swagger API
                    .requestMatchers("/api", "/api/", "/swagger-ui/index.html").hasAuthority(USER.name())
                    // Assets
                    .requestMatchers("/favicons/**").permitAll()
                    .requestMatchers("/browserconfig.xml").permitAll()
                    .requestMatchers("/site.webmanifest").permitAll()
                    .requestMatchers("/css/**").permitAll()
                    .requestMatchers("/fonts/**").permitAll()
                    .requestMatchers("/images/**").permitAll()
                    .requestMatchers("/assets/**").permitAll()
                    // Web
                    .requestMatchers("/login*").permitAll()
                    .requestMatchers("/web/absences/**").hasAuthority(USER.name())
                    .requestMatchers("/web/application/**").hasAuthority(USER.name())
                    .requestMatchers("/web/department/**").hasAnyAuthority(BOSS.name(), OFFICE.name())
                    .requestMatchers("/web/google-api-handshake/**").hasAuthority(OFFICE.name())
                    .requestMatchers("/web/overview").hasAuthority(USER.name())
                    .requestMatchers("/web/overtime/**").hasAuthority(USER.name())
                    .requestMatchers("/web/person/**").hasAuthority(USER.name())
                    .requestMatchers("/web/sicknote/**").hasAuthority(USER.name())
                    .requestMatchers("/web/settings/**").hasAuthority(OFFICE.name())
                    .anyRequest().authenticated()
            )
            .oauth2Login(
                loginCustomizer -> loginCustomizer.authorizationEndpoint(
                    endpointCustomizer -> endpointCustomizer.authorizationRequestResolver(new LoginHintAwareResolver(clientRegistrationRepository))
                )
            )
            .logout(
                logoutCustomizer -> logoutCustomizer.logoutSuccessHandler(oidcClientInitiatedLogoutSuccessHandler)
            )
            .securityContext(
                securityContext -> securityContext.securityContextRepository(securityContextRepository)
            )
            .addFilterAfter(new ReloadAuthenticationAuthoritiesFilter(personService, sessionService, securityContextRepository), BasicAuthenticationFilter.class)
            .build();
    }

    @Bean
    DelegatingSecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
            new RequestAttributeSecurityContextRepository(),
            new HttpSessionSecurityContextRepository()
        );
    }
}
