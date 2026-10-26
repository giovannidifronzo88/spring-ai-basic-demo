package com.test.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

// Esegue l'azione solo con un'approvazione valida, creata dal server per la stessa chat e la stessa operazione.
// Senza approvazione genera un nuovo ID: l'ID non entra nel testo restituito al modello, ma la UI lo riceve dal controller.
class GitApprovalGate {

    private static final Logger log = LoggerFactory.getLogger(GitApprovalGate.class);

    private final GitApprovalStore approvals;

    GitApprovalGate(GitApprovalStore approvals) {
        this.approvals = approvals;
    }

    String execute(ToolContext toolContext, String operation, String preview, GitAction action) throws Exception {
        Map<String, Object> ctx = toolContext.getContext();
        String chatId = String.valueOf(ctx.getOrDefault("chatId", ""));
        String approval = String.valueOf(ctx.getOrDefault("gitApproval", ""));
        if (!approval.isBlank() && approvals.matches(chatId, approval, operation)) {
            log.info("git-tool: approvazione valida per '{}', esecuzione", operation);
            return action.run();
        }
        approvals.create(chatId, operation);
        log.info("git-tool: in attesa di approvazione per '{}'", operation);
        return preview + "\nNessuna modifica eseguita: serve la conferma dell'utente dall'interfaccia. "
                + "Non ripetere la richiesta e non dichiarare l'operazione completata.";
    }
}
