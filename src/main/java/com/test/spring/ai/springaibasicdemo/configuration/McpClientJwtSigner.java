package com.test.spring.ai.springaibasicdemo.configuration;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class McpClientJwtSigner {

    static final String SUBJECT = "spring-ai-basic-demo";
    static final String AUDIENCE = "spring-ai-demo-mcp-server";
    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(5);

    private final ECDSASigner signer;

    private McpClientJwtSigner(ECDSASigner signer) {
        this.signer = signer;
    }

    public static McpClientJwtSigner fromPemFile(Path privateKeyPath) {
        try {
            ECPrivateKey privateKey = readPkcs8EcPrivateKey(privateKeyPath);
            return new McpClientJwtSigner(new ECDSASigner(privateKey));
        } catch (IOException | GeneralSecurityException | JOSEException e) {
            throw new IllegalStateException("Chiave privata MCP non leggibile: " + privateKeyPath, e);
        }
    }

    public String issueToken() {
        SignedJWT jwt = new SignedJWT(buildHeader(), buildClaims(Instant.now()));
        try {
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Firma del JWT MCP non riuscita", e);
        }
    }

    private static ECPrivateKey readPkcs8EcPrivateKey(Path privateKeyPath)
            throws IOException, GeneralSecurityException {
        String base64Body = Files.readString(privateKeyPath)
                .replaceAll("-----[A-Z ]+-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64Body);
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private JWSHeader buildHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.ES256).build();
    }

    private JWTClaimsSet buildClaims(Instant now) {
        return new JWTClaimsSet.Builder()
                .subject(SUBJECT)
                .audience(AUDIENCE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(TOKEN_VALIDITY)))
                .jwtID(UUID.randomUUID().toString())
                .build();
    }
}
