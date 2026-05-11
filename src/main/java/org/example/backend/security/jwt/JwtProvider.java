package org.example.backend.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.security.principle.MyUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;

import java.security.Key;
import java.util.*;
import java.util.function.Function;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration}")
    private Long EXPIRED_ACCESS;

    @Value("${jwt.refresh}")
    private Long REFRESH_INTERVAL;
    /**
     * 1. Trích xuất Username (Email) từ Token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 2. Trích xuất thời gian hết hạn
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 3. Hàm generic để trích xuất bất kỳ thông tin nào (Claims)
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 4. Xác thực Token có hợp lệ với User đang đăng nhập không
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            final String type = extractClaim(token, claims -> claims.get("type", String.class));

            return (username.equals(userDetails.getUsername())
                    && !isTokenExpired(token)
                    && "access".equals(type));

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return false;
        } catch (io.jsonwebtoken.JwtException e) {
            return false;
        }
    }
    public Boolean validateRefreshToken(String token) {
        try {
            final String type = extractClaim(token, c -> c.get("type", String.class));

            return (!isTokenExpired(token) && "refresh".equals(type));

        } catch (Exception e) {
            return false;
        }
    }
    /**
     * 6. Chuyển đổi SECRET_KEY từ Base64 sang đối tượng Key hợp lệ
     */
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String buildToken(Map<String, Object> claims,
                              String username,
                              long expiration,
                              String type) {

        claims.put("type", type);
        claims.put("jti", UUID.randomUUID().toString());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuer("taskflow-app")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateAccessToken(Authentication authentication) {

        Map<String, Object> claims = new HashMap<>();
        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();

        Object principal = authentication.getPrincipal();
        if (principal instanceof MyUserDetails myUserDetails) {
            User user = myUserDetails.getUser();
            if (user != null && user.getRoles() != null) {
                for (Role role : user.getRoles()) {
                    if (role == null) {
                        continue;
                    }
                    if (role.getName() != null && !role.getName().isBlank()) {
                        roles.add(role.getName());
                    }
                    if (role.getPermissions() != null) {
                        role.getPermissions().stream()
                                .filter(permission -> permission != null && !permission.isBlank())
                                .forEach(permissions::add);
                    }
                }
            }
        } else {
            authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(Objects::nonNull)
                    .forEach(authority -> {
                        if (authority.startsWith("ROLE_") && authority.length() > 5) {
                            roles.add(authority.substring(5));
                        } else if (!authority.startsWith("PROJECT_")) {
                            permissions.add(authority);
                        }
                    });
        }

        claims.put("permissions", new ArrayList<>(permissions));
        claims.put("roles", new ArrayList<>(roles));

        return buildToken(claims, authentication.getName(), EXPIRED_ACCESS, "access");
    }

    public String generateRefreshToken(Authentication authentication) {
        return buildToken(new HashMap<>(), authentication.getName(), REFRESH_INTERVAL, "refresh");
    }

    public List<String> extractRoles(String token) {
        return extractStringListClaim(token, "roles");
    }

    public List<String> extractPermissions(String token) {
        return extractStringListClaim(token, "permissions");
    }

    public Long getAccessExpired() {
        return EXPIRED_ACCESS;
    }

    private List<String> extractStringListClaim(String token, String claimKey) {
        Object raw = extractClaim(token, c -> c.get(claimKey));
        if (!(raw instanceof List<?> rawList)) {
            return new ArrayList<>();
        }
        return rawList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    public String generateOtpVerificationToken(String email) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("purpose", "REGISTER_VERIFY");

        long OTP_VERIFY_EXPIRED = 5 * 60 * 1000; // 5 phút

        return buildToken(
                claims,
                email,
                OTP_VERIFY_EXPIRED,
                "otp_verify"
        );
    }

    public boolean isOtpVerificationToken(String token) {
        try {
            String type = extractClaim(
                    token,
                    claims -> claims.get("type", String.class)
            );
            String purpose = extractClaim(
                    token,
                    claims -> claims.get("purpose", String.class)
            );
            return "otp_verify".equals(type)
                    && "REGISTER_VERIFY".equals(purpose)
                    && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}