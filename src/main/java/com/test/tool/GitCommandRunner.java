package com.test.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// Unico punto che avvia processi git. Non conosce le operazioni: riceve solo gli argomenti.
class GitCommandRunner {

    private static final Logger log = LoggerFactory.getLogger(GitCommandRunner.class);
    private static final long TIMEOUT_SECONDS = 30;

    private final String githubPat;

    GitCommandRunner(String githubPat) {
        this.githubPat = githubPat;
    }

    String run(String... args) throws IOException, InterruptedException {
        return execute(Map.of(), args);
    }

    // Se app.github.pat è configurato, il comando usa quel PAT invece delle credenziali salvate nel sistema.
    // Il token passa solo come variabile d'ambiente del processo: non finisce negli argomenti né nei log.
    String runAuthenticated(String... args) throws IOException, InterruptedException {
        return execute(pushAuthEnv(), args);
    }

    private Map<String, String> pushAuthEnv() {
        if (githubPat == null || githubPat.isBlank()) {
            return Map.of();
        }
        String basic = Base64.getEncoder()
                .encodeToString(("x-access-token:" + githubPat).getBytes(StandardCharsets.UTF_8));
        return Map.of(
                "GIT_CONFIG_COUNT", "1",
                "GIT_CONFIG_KEY_0", "http.https://github.com/.extraHeader",
                "GIT_CONFIG_VALUE_0", "Authorization: Basic " + basic);
    }

    private String execute(Map<String, String> extraEnv, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(args));

        // Output su file temporaneo invece che su pipe: evita il blocco del processo quando il buffer si riempie,
        // e permette a waitFor di far scattare il timeout.
        File outFile = Files.createTempFile("git-tool-", ".log").toFile();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd)
                    .directory(new File(System.getProperty("user.dir")))
                    .redirectErrorStream(true)
                    .redirectOutput(outFile);
            pb.environment().put("GIT_TERMINAL_PROMPT", "0");
            pb.environment().putAll(extraEnv);

            Process p = pb.start();
            log.info("git-tool: avvio 'git {}' (cwd={})", String.join(" ", args), pb.directory());
            long start = System.currentTimeMillis();
            if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                log.error("git-tool: 'git {}' in timeout dopo {}s", args[0], TIMEOUT_SECONDS);
                throw new IllegalStateException("git " + args[0] + " in timeout dopo " + TIMEOUT_SECONDS + "s");
            }
            String out = Files.readString(outFile.toPath());
            log.info("git-tool: 'git {}' exit={} in {}ms, output: {}", args[0], p.exitValue(),
                    System.currentTimeMillis() - start, out.trim());
            if (p.exitValue() != 0) {
                throw new IllegalStateException("git " + args[0] + " fallito (exit " + p.exitValue() + "):\n" + out);
            }
            return out;
        } finally {
            Files.deleteIfExists(outFile.toPath());
        }
    }
}
