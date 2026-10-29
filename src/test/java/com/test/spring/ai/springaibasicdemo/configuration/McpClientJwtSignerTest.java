package com.test.spring.ai.springaibasicdemo.configuration;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class McpClientJwtSignerTest {

    @TempDir
    Path tempDir;

    @Test
    void issuedTokenIsVerifiableWithThePublicKeyAndCarriesExpectedClaims() throws Exception {
        KeyPair keys = generateEcKeyPair();
        Path privateKeyFile = tempDir.resolve("mcp-client-private.pem");
        Files.writeString(privateKeyFile, toPem("PRIVATE KEY", keys.getPrivate().getEncoded()));

        String token = McpClientJwtSigner.fromPemFile(privateKeyFile).issueToken();

        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.verify(new ECDSAVerifier((ECPublicKey) keys.getPublic()))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("spring-ai-basic-demo");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("spring-ai-demo-mcp-server");
    }

    private static KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static String toPem(String type, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }
}
