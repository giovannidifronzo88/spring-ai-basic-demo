package com.test.tool;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

// Non è un @Component: il bean viene creato solo con il profilo "git-tool" in GitToolConfiguration.
public class GitTool {

    private static final Logger log = LoggerFactory.getLogger(GitTool.class);
    private static final long TIMEOUT_SECONDS = 30;
    private static final Pattern SAFE_BRANCH = Pattern.compile("[A-Za-z0-9._/-]+");

    private final String expectedToken;

    public GitTool(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Tool(description = "Pubblica su origin i commit locali non ancora pushati, sul branch corrente. " +
            "Chiamalo con confirm=false per mostrare i commit da pubblicare. " +
            "Chiamalo con confirm=true SOLO dopo che l'utente ha confermato esplicitamente in chat di voler pushare.")
    public String gitPushIfNeeded(
            @ToolParam(description = "false = solo anteprima dei commit; true = esegue il push. Usa true solo dopo conferma esplicita dell'utente.")
            boolean confirm,
            ToolContext toolContext) {
        // Il token arriva dall'header della richiesta via ToolContext, non dal modello: ToolContext non è nello schema JSON.
        // Fail-closed: se APP_GIT_TOOL_TOKEN è vuoto, il tool non esegue nulla.
        String provided = String.valueOf(toolContext.getContext().getOrDefault("gitToolToken", ""));
        if (expectedToken.isBlank()
                || !MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8),
                                          provided.getBytes(StandardCharsets.UTF_8))) {
            log.warn("git-tool: invocazione rifiutata, token mancante o errato");
            return "Errore: token non valido, operazione git non eseguita.";
        }
        log.info("git-tool: invocazione autorizzata, confirm={}, cwd={}", confirm, System.getProperty("user.dir"));
        try {
            String branch = runGit("rev-parse", "--abbrev-ref", "HEAD").trim();
            log.info("git-tool: branch corrente '{}'", branch);
            // Whitelist del nome branch: impedisce che un valore strano diventi un argomento di git push.
            if (!SAFE_BRANCH.matcher(branch).matches()) {
                return "Branch non valido: " + branch;
            }

            String pending = runGit("log", "--oneline", "@{u}..HEAD").trim();
            if (pending.isEmpty()) {
                log.info("git-tool: nessun commit da pushare sul branch {}", branch);
                return "Nessun commit locale da pushare.";
            }

            // Primo giro (confirm=false): solo anteprima. Il push parte solo alla chiamata successiva con confirm=true.
            // Questa conferma è gestita dal modello, non è un controllo lato server.
            if (!confirm) {
                log.info("git-tool: anteprima commit per branch {}", branch);
                return "Commit da pubblicare su origin/" + branch + ":\n" + pending
                        + "\nChiedi conferma all'utente prima di pushare.";
            }

            log.info("git-tool: push confermato su origin/{}, commit:\n{}", branch, pending);
            String out = runGit("push", "origin", branch);
            log.info("git-tool: push completato su origin/{}", branch);
            return out.isBlank() ? "Push completato." : out;
        } catch (Exception e) {
            // Log completo con stack trace: il modello riceve solo il messaggio, qui vediamo la causa reale.
            log.error("git-tool: operazione fallita: {}", e.toString(), e);
            return "Errore: " + e.getMessage();
        }
    }

    private String runGit(String... args) throws IOException, InterruptedException {
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
