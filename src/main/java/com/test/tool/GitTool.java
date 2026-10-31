package com.test.tool;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Optional;

// Non è un @Component: il bean viene creato solo con il profilo "git-tool" in GitToolConfiguration.
// Espone ai tool solo le operazioni git; token, processi, validazione e approvazioni sono in classi dedicate.
public class GitTool {

    private static final Logger log = LoggerFactory.getLogger(GitTool.class);
    private static final String TOKEN_ERROR = "Errore: token non valido, operazione git non eseguita.";

    private final GitAccessGuard guard;
    private final GitRepository repo;
    private final GitApprovalStore approvals;
    private final GitApprovalGate gate;

    public GitTool(String expectedToken, String githubPat) {
        this(new GitAccessGuard(expectedToken),
                new GitRepository(new GitCommandRunner(githubPat)),
                new GitApprovalStore());
    }

    GitTool(GitAccessGuard guard, GitRepository repo, GitApprovalStore approvals) {
        this.guard = guard;
        this.repo = repo;
        this.approvals = approvals;
        this.gate = new GitApprovalGate(approvals);
    }

    @Tool(description = "Prepara la pubblicazione su origin dei commit locali non ancora pushati, sul branch corrente. " +
            "Usarlo solo se l'utente chiede esplicitamente di pubblicare; per domande informative usare gitStatus. " +
            "Restituisce solo l'anteprima: la pubblicazione avviene solo dopo la conferma dell'utente nell'interfaccia.")
    public String gitPushIfNeeded(ToolContext toolContext) {
        if (!guard.isAuthorized(toolContext)) {
            return TOKEN_ERROR;
        }
        log.info("git-tool: invocazione autorizzata, cwd={}", System.getProperty("user.dir"));
        try {
            String branch = repo.currentBranch();
            log.info("git-tool: branch corrente '{}'", branch);
            // Whitelist del nome branch: impedisce che un valore strano diventi un argomento di git push.
            if (!GitBranchNames.matchesSafePattern(branch)) {
                return "Branch non valido: " + branch;
            }

            boolean published = repo.hasUpstream();
            String pending = pendingCommits(published);
            if (published && pending.isEmpty()) {
                log.info("git-tool: nessun commit da pushare sul branch {}", branch);
                return "Nessun commit locale da pushare.";
            }
            String target = "origin/" + branch;
            String newBranchNote = published ? "" : "\nIl branch '" + branch + "' non esiste ancora su origin e verrebbe creato.";

            String commits = pending.isEmpty() ? "(nessun commit nuovo)" : pending;
            String preview = "Commit da pubblicare su " + target + ":\n" + commits + newBranchNote;
            //effettua il push solo se è arrivata la conferma dall'utente, altrimenti crea una richiesta di approvazione
            // per far eseguire il push il prossimo prompt.
            return gate.execute(toolContext, GitOperation.PUSH.key(branch), preview, () -> publishBranch(branch, published));
        } catch (Exception e) {
            // Log completo con stack trace: il modello riceve solo il messaggio, qui vediamo la causa reale.
            log.error("git-tool: operazione fallita: {}", e.toString(), e);
            return "Errore: " + e.getMessage();
        }
    }

    // Senza upstream il branch non è mai stato pubblicato: i commit da pubblicare sono quelli non presenti su nessun remote.
    private String pendingCommits(boolean hasUpstream) throws Exception {
        return hasUpstream
                ? repo.logOneline("@{u}..HEAD")
                : repo.logOneline("HEAD", "--not", "--remotes");
    }

    private String publishBranch(String branch, boolean hasUpstream) throws Exception {
        String out = repo.push(branch, !hasUpstream);
        log.info("git-tool: push completato su origin/{}", branch);
        return out.isBlank() ? "Push completato." : out;
    }

    private String createLocalBranch(String branchName, String baseBranch) throws Exception {
        String out = repo.createBranchFrom(branchName, baseBranch);
        log.info("git-tool: branch '{}' creato da '{}'", branchName, baseBranch);
        return out.isBlank() ? "Branch '" + branchName + "' creato da '" + baseBranch + "' e selezionato." : out;
    }

    private String mergeIntoTarget(String sourceBranch, String targetBranch) throws Exception {
        String current = repo.currentBranch();
        if (!targetBranch.equals(current)) {
            repo.switchTo(targetBranch);
            log.info("git-tool: passaggio da '{}' a '{}' per il merge", current, targetBranch);
        }
        try {
            String out = repo.merge(sourceBranch);
            log.info("git-tool: merge di '{}' in '{}' completato", sourceBranch, targetBranch);
            return out.isBlank() ? "Merge completato." : out;
        } catch (IllegalStateException e) {
            // Conflitto o errore durante il merge: si torna allo stato precedente.
            repo.abortMerge();
            return "Errore durante il merge, operazione annullata:\n" + e.getMessage();
        }
    }

    @Tool(description = "Prepara la creazione di un nuovo branch locale a partire da un branch locale di base indicato esplicitamente. " +
            "Usarlo solo se l'utente chiede esplicitamente di creare il branch; per domande informative usare gitStatus. " +
            "Restituisce solo l'anteprima: la creazione avviene solo dopo la conferma dell'utente nell'interfaccia.")
    public String gitCreateBranch(
            @ToolParam(description = "Nome del nuovo branch da creare (lettere, cifre, '.', '_', '/', '-').")
            String branchName,
            @ToolParam(description = "Nome del branch locale di partenza da cui creare il nuovo branch.")
            String baseBranch,
            ToolContext toolContext) {
        if (!guard.isAuthorized(toolContext)) {
            return TOKEN_ERROR;
        }
        log.info("git-tool: richiesta creazione branch '{}' da '{}'", branchName, baseBranch);
        try {
            if (!GitBranchNames.isValid(branchName)) {
                return "Nome branch non valido: " + branchName;
            }
            if (!GitBranchNames.isValid(baseBranch)) {
                return "Nome branch di base non valido: " + baseBranch;
            }
            if (branchName.equals(baseBranch)) {
                return "Il nuovo branch e il branch di base coincidono ('" + baseBranch + "').";
            }
            repo.requireLocalBranch(baseBranch);

            String preview = "Verrebbe creato il branch '" + branchName + "' a partire da '" + baseBranch + "' e selezionato.";
            // Se il branch esiste già, git restituisce un errore esplicito che arriva al modello.
            return gate.execute(toolContext, GitOperation.CREATE_BRANCH.key(branchName, baseBranch), preview,
                    () -> createLocalBranch(branchName, baseBranch));
        } catch (Exception e) {
            log.error("git-tool: creazione branch fallita: {}", e.toString(), e);
            return "Errore: " + e.getMessage();
        }
    }

    @Tool(description = "Prepara il merge di un branch locale sorgente nel branch locale di destinazione indicati esplicitamente. " +
            "Usarlo solo se l'utente chiede esplicitamente di eseguire il merge; per domande informative usare gitStatus. " +
            "Non procede se ci sono modifiche non committate; in caso di conflitto annulla il merge. " +
            "Restituisce solo l'anteprima dei commit: il merge avviene solo dopo la conferma dell'utente nell'interfaccia.")
    public String gitMergeBranch(
            @ToolParam(description = "Nome del branch locale sorgente, cioè quello da integrare.")
            String sourceBranch,
            @ToolParam(description = "Nome del branch locale di destinazione, cioè quello in cui avviene il merge.")
            String targetBranch,
            ToolContext toolContext) {
        if (!guard.isAuthorized(toolContext)) {
            return TOKEN_ERROR;
        }
        log.info("git-tool: richiesta merge di '{}' in '{}'", sourceBranch, targetBranch);
        try {
            if (!GitBranchNames.isValid(sourceBranch)) {
                return "Nome branch sorgente non valido: " + sourceBranch;
            }
            if (!GitBranchNames.isValid(targetBranch)) {
                return "Nome branch di destinazione non valido: " + targetBranch;
            }
            if (sourceBranch.equals(targetBranch)) {
                return "Il branch sorgente coincide con quello di destinazione ('" + targetBranch + "'), merge non necessario.";
            }
            repo.requireLocalBranch(sourceBranch);
            repo.requireLocalBranch(targetBranch);

            String dirty = repo.statusPorcelain();
            if (!dirty.isEmpty()) {
                return "Ci sono modifiche non committate, merge non eseguito:\n" + dirty;
            }

            // Calcolato senza cambiare branch, così l'anteprima non modifica il repository.
            String incoming = repo.logOneline(targetBranch + ".." + sourceBranch);
            if (incoming.isEmpty()) {
                return "Nessun commit da integrare da '" + sourceBranch + "' in '" + targetBranch + "'.";
            }

            String preview = "Commit da integrare da '" + sourceBranch + "' in '" + targetBranch + "':\n" + incoming;
            return gate.execute(toolContext, GitOperation.MERGE_BRANCH.key(sourceBranch, targetBranch), preview,
                    () -> mergeIntoTarget(sourceBranch, targetBranch));
        } catch (Exception e) {
            log.error("git-tool: merge fallito: {}", e.toString(), e);
            return "Errore: " + e.getMessage();
        }
    }

    @Tool(description = "Mostra in sola lettura lo stato del repository: branch corrente, commit locali non ancora pushati e modifiche non committate. " +
            "Usarlo per rispondere a domande su branch, commit o modifiche. Non prepara né esegue alcuna operazione.")
    public String gitStatus(ToolContext toolContext) {
        if (!guard.isAuthorized(toolContext)) {
            return TOKEN_ERROR;
        }
        log.info("git-tool: richiesta stato repository");
        try {
            String branch = repo.currentBranch();
            if (!GitBranchNames.matchesSafePattern(branch)) {
                return "Branch non valido: " + branch;
            }
            boolean published = repo.hasUpstream();
            String pending = pendingCommits(published);
            String dirty = repo.statusPorcelain();
            return formatStatus(branch, published, pending, dirty);
        } catch (Exception e) {
            log.error("git-tool: lettura stato fallita: {}", e.toString(), e);
            return "Errore: " + e.getMessage();
        }
    }

    private String formatStatus(String branch, boolean published, String pending, String dirty) {
        String upstream = published ? "tracciato su origin/" + branch : "non ancora pubblicato su origin";
        String commits = pending.isEmpty() ? "nessun commit locale da pushare" : "commit da pushare:\n" + pending;
        String changes = dirty.isEmpty() ? "nessuna modifica non committata" : "modifiche non committate:\n" + dirty;
        return "Branch corrente: " + branch + " (" + upstream + ")\n" + commits + "\n" + changes;
    }

    // Usato dal controller per esporre l'ID alla UI (non al modello).
    public Optional<String> approvalId(String chatId) {
        return approvals.id(chatId);
    }
}
