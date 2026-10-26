package com.test.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Operazioni git sul repository locale, espresse come metodi di dominio.
class GitRepository {

    private static final Logger log = LoggerFactory.getLogger(GitRepository.class);

    private final GitCommandRunner git;

    GitRepository(GitCommandRunner git) {
        this.git = git;
    }

    String currentBranch() throws IOException, InterruptedException {
        return git.run("rev-parse", "--abbrev-ref", "HEAD").trim();
    }

    boolean hasUpstream() throws IOException, InterruptedException {
        try {
            git.run("rev-parse", "--verify", "--quiet", "@{u}");
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    // Lancia IllegalStateException se il branch locale non esiste.
    void requireLocalBranch(String name) throws IOException, InterruptedException {
        git.run("show-ref", "--verify", "--quiet", "refs/heads/" + name);
    }

    String statusPorcelain() throws IOException, InterruptedException {
        return git.run("status", "--porcelain").trim();
    }

    String logOneline(String... revArgs) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("log", "--oneline"));
        args.addAll(List.of(revArgs));
        return git.run(args.toArray(String[]::new)).trim();
    }

    String createBranchFrom(String newBranch, String baseBranch) throws IOException, InterruptedException {
        return git.run("switch", "-c", newBranch, baseBranch);
    }

    void switchTo(String branch) throws IOException, InterruptedException {
        git.run("switch", branch);
    }

    // Lancia IllegalStateException in caso di conflitto: il chiamante decide se annullare.
    String merge(String sourceBranch) throws IOException, InterruptedException {
        return git.run("merge", "--no-edit", sourceBranch);
    }

    void abortMerge() {
        try {
            git.run("merge", "--abort");
        } catch (Exception e) {
            log.warn("git-tool: merge --abort non riuscito: {}", e.getMessage());
        }
    }

    String push(String branch, boolean setUpstream) throws IOException, InterruptedException {
        return setUpstream
                ? git.runAuthenticated("push", "-u", "origin", branch)
                : git.runAuthenticated("push", "origin", branch);
    }
}
