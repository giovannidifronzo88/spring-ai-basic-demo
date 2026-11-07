package com.test.spring.ai.springaibasicdemo.advisor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiCategoryTest {

    @Test
    void masksEmailAddress() {
        String masked = PiiCategory.EMAIL.maskIn("scrivimi a mario.rossi@example.it per favore");

        assertThat(masked).isEqualTo("scrivimi a [EMAIL] per favore");
    }

    @Test
    void masksItalianTaxCode() {
        String masked = PiiCategory.CODICE_FISCALE.maskIn("il mio CF è RSSMRA80A01H501U");

        assertThat(masked).isEqualTo("il mio CF è [CODICE_FISCALE]");
    }

    @Test
    void masksItalianIban() {
        String masked = PiiCategory.IBAN.maskIn("bonifico su IT60X0542811101000000123456");

        assertThat(masked).isEqualTo("bonifico su [IBAN]");
    }

    @Test
    void masksItalianMobilePhoneWithPrefix() {
        String masked = PiiCategory.TELEFONO.maskIn("chiamami al +39 333 123 4567");

        assertThat(masked).isEqualTo("chiamami al [TELEFONO]");
    }

    @Test
    void masksItalianMobilePhoneWithoutPrefix() {
        String masked = PiiCategory.TELEFONO.maskIn("numero 3331234567");

        assertThat(masked).isEqualTo("numero [TELEFONO]");
    }

    @Test
    void doesNotMaskPlainText() {
        String text = "quante ore di ferie spettano al livello 3?";

        assertThat(PiiCategory.values())
                .allMatch(category -> !category.appearsIn(text));
    }
}
