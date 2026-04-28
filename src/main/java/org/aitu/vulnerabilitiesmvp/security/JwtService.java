package org.aitu.vulnerabilitiesmvp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.aitu.vulnerabilitiesmvp.config.AppProperties;
import org.springframework.stereotype.Service;

@Service
// public class jwtService {
public final class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long expirationMinutes;

    public JwtService(AppProperties appProperties) {
        this.signingKey = buildSigningKey(appProperties.getSecurity().getJwt().getSecret());
        this.issuer = appProperties.getSecurity().getJwt().getIssuer();
        this.expirationMinutes = appProperties.getSecurity().getJwt().getExpirationMinutes();
    }

    public String generateToken(AppUserPrincipal principal) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationMinutes * 60);

        return Jwts.builder()
            .subject(principal.getUsername())
            .issuer(issuer)
            .id(UUID.randomUUID().toString())
            .claims(Map.of("role", principal.getRole().name(), "uid", principal.getId()))
            .issuedAt(Date.from(issuedAt))
            .notBefore(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, AppUserPrincipal principal) {
        Claims claims = extractAllClaims(token);
        Number uidClaim = claims.get("uid", Number.class);
        String roleClaim = claims.get("role", String.class);
        String tokenId = claims.getId();
        Date issuedAt = claims.getIssuedAt();
        Date notBefore = claims.getNotBefore();
        Date expiration = claims.getExpiration();
        Date now = new Date();

        if (uidClaim == null
            || roleClaim == null
            || tokenId == null
            || tokenId.isBlank()
            || issuedAt == null
            || notBefore == null
            || expiration == null) {
            return false;
        }

        return principal.getUsername().equals(claims.getSubject())
            && issuer.equals(claims.getIssuer())
            && principal.getId().equals(uidClaim.longValue())
            && principal.getRole().name().equals(roleClaim)
            && !issuedAt.after(now)
            && !notBefore.after(now)
            && expiration.after(now);
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey buildSigningKey(String base64Secret) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits encoded in Base64");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
