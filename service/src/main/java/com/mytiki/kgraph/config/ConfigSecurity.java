/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

package com.mytiki.kgraph.config;

import com.mytiki.common.ApiConstants;
import com.mytiki.kgraph.utilities.ApiKeyAuthMgr;
import com.mytiki.kgraph.utilities.ApiKeyFilter;
import com.mytiki.kgraph.utilities.Constants;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SingleKeyJWSKeySelector;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@EnableWebSecurity
@EnableGlobalMethodSecurity(jsr250Enabled = true)
public class ConfigSecurity extends WebSecurityConfigurerAdapter {
    private static final String FEATURE_POLICY = "accelerometer" + " 'none'" + "ambient-light-sensor" + " 'none'" +
            "autoplay" + " 'none'" + "battery" + " 'none'" + "camera" + " 'none'" + "display-capture" + " 'none'" +
            "document-domain" + " 'none'" + "encrypted-media" + " 'none'" + "execution-while-not-rendered" + " 'none'" +
            "execution-while-out-of-viewport" + " 'none'" + "fullscreen" + " 'none'" + "geolocation" + " 'none'" +
            "gyroscope" + " 'none'" + "layout-animations" + " 'none'" + "legacy-image-formats" + " 'none'" +
            "magnetometer" + " 'none'" + "microphone" + " 'none'" + "midi" + " 'none'" + "navigation-override" + " 'none'" +
            "oversized-images" + " 'none'" + "payment" + " 'none'" + "picture-in-picture" + " 'none'" + "publickey-credentials-get" + " 'none'" +
            "sync-xhr" + " 'none'" + "usb" + " 'none'" + "vr wake-lock" + " 'none'" + "xr-spatial-tracking" + " 'none'";

    private static final String CONTENT_SECURITY_POLICY = "default-src" + "' self'";

    @Value("${com.mytiki.kgraph.api_key.ingest}")
    private String ingestApiKey;

    @Value("${com.mytiki.kgraph.api_key.etl}")
    private String etlApiKey;

    @Value("${com.mytiki.kgraph.jwt.bouncer.public_key}")
    private String jwtBouncerPublicKey;

    private final AccessDeniedHandler accessDeniedHandler;
    private final AuthenticationEntryPoint authEntryPointImplException;

    public ConfigSecurity(
            @Autowired AccessDeniedHandler accessDeniedHandler,
            @Autowired @Qualifier(value = "authEntryPointImplException")
                    AuthenticationEntryPoint authEntryPointImplException) {
        super(true);
        this.accessDeniedHandler = accessDeniedHandler;
        this.authEntryPointImplException = authEntryPointImplException;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .addFilter(new WebAsyncManagerIntegrationFilter())
                .servletApi().and()
                .exceptionHandling(handler -> handler
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authEntryPointImplException)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext().and()
                .headers(headers -> headers
                        .cacheControl().and()
                        .contentTypeOptions().and()
                        .httpStrictTransportSecurity().and()
                        .frameOptions().and()
                        .xssProtection().and()
                        .referrerPolicy().and()
                        .featurePolicy(FEATURE_POLICY).and()
                        .httpPublicKeyPinning().and()
                        .contentSecurityPolicy(CONTENT_SECURITY_POLICY)
                )
                .anonymous().and()
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt( jwt -> jwt
                                .decoder(new NimbusJwtDecoder(jwtProcessor()))
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authEntryPointImplException)
                )
                .addFilter(new ApiKeyFilter()
                        .setAuthMgr(new ApiKeyAuthMgr(ingestApiKey, Constants.ROLE_INGEST)))
                .addFilter(new ApiKeyFilter()
                        .setAuthMgr(new ApiKeyAuthMgr(etlApiKey, Constants.ROLE_ETL)))
                .authorizeRequests(authorize -> authorize
                        .antMatchers(HttpMethod.GET, ApiConstants.HEALTH_ROUTE).permitAll()
                        .antMatchers(
                                HttpMethod.POST,
                                ApiConstants.API_LATEST_ROUTE + "big-picture"
                        ).permitAll()
                        .anyRequest()
                        .authenticated()
                );
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET","PUT","POST","DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("*/**", configuration);
        return source;
    }

    private JWTProcessor<SecurityContext> jwtProcessor(){
        try {
            DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64
                    .getDecoder()
                    .decode(jwtBouncerPublicKey));
            PublicKey publicKey = KeyFactory
                    .getInstance("EC")
                    .generatePublic(publicKeySpec);
            processor.setJWSKeySelector(new SingleKeyJWSKeySelector<>(JWSAlgorithm.ES256, publicKey));
            processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>());
            return processor;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter(){
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> claims = jwt.getClaims();
            List<GrantedAuthority> authorities = new ArrayList<>();
            if(claims.get(Constants.CLAIMS_ISS).equals(Constants.CLAIM_ISS_BOUNCER))
                authorities.add(new SimpleGrantedAuthority(Constants.ROLE_USER));
            return authorities;
        });
        return jwtAuthenticationConverter;
    }
}
