package com.test.spring.ai.springaibasicdemo.advisor;

import java.util.regex.Pattern;

/**
 * Categorie di dati personali riconosciute nel prompt, con il pattern di rilevamento e il segnaposto usato per il mascheramento.
 */
public enum PiiCategory {

    EMAIL("[EMAIL]",
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")),

    // Codice fiscale italiano: 6 lettere, 2 cifre, 1 lettera di mese, 2 cifre, 1 lettera comune, 3 cifre, 1 carattere di controllo.
    CODICE_FISCALE("[CODICE_FISCALE]",
            Pattern.compile("\\b[A-Z]{6}\\d{2}[ABCDEHLMPRST]\\d{2}[A-Z]\\d{3}[A-Z]\\b", Pattern.CASE_INSENSITIVE)),

    IBAN("[IBAN]",
            Pattern.compile("\\bIT\\d{2}[A-Z]\\d{22}\\b", Pattern.CASE_INSENSITIVE)),

    // Cellulare italiano: inizia per 3, 10 cifre, con prefisso internazionale +39 opzionale.
    TELEFONO("[TELEFONO]",
            Pattern.compile("(?<!\\d)(\\+39[\\s.-]?)?3\\d{2}[\\s.-]?\\d{3}[\\s.-]?\\d{4}(?!\\d)"));

    private final String placeholder;
    private final Pattern pattern;

    PiiCategory(String placeholder, Pattern pattern) {
        this.placeholder = placeholder;
        this.pattern = pattern;
    }

    public boolean appearsIn(String text) {
        return pattern.matcher(text).find();
    }

    public String maskIn(String text) {
        return pattern.matcher(text).replaceAll(placeholder);
    }
}
