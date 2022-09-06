package uk.gov.hmcts.reform.iacaseapi.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;

class AsylumFieldCaseNameFixerTest {

    private AsylumFieldCaseNameFixer asylumFieldCaseNameFixer;
    private AsylumCase asylumCase;

    @BeforeEach
    public void setUp() {

        asylumFieldCaseNameFixer = new AsylumFieldCaseNameFixer(HMCTS_CASE_NAME_INTERNAL, APPELLANT_GIVEN_NAMES, APPELLANT_FAMILY_NAME);

        asylumCase = new AsylumCase();
    }

    @Test
    void transposes_asylum_case_name() {

        final String expectedCaseName = "John Smith";
        asylumCase.write(APPELLANT_GIVEN_NAMES, "John");
        asylumCase.write(APPELLANT_FAMILY_NAME, "Smith");

        asylumFieldCaseNameFixer.fix(asylumCase);

        assertThat(asylumCase.read(HMCTS_CASE_NAME_INTERNAL, String.class).get())
            .isEqualTo(expectedCaseName);
        assertThat(asylumCase.read(CASE_NAME_HMCTS_INTERNAL, String.class).get())
            .isEqualTo(expectedCaseName);
    }

    @Test
    void transposes_and_format_asylum_case_name() {

        final String expectedCaseName = "John Smith";
        asylumCase.write(APPELLANT_GIVEN_NAMES, "John ");
        asylumCase.write(APPELLANT_FAMILY_NAME, "  Smith");

        asylumFieldCaseNameFixer.fix(asylumCase);

        assertThat(asylumCase.read(HMCTS_CASE_NAME_INTERNAL, String.class).get())
            .isEqualTo(expectedCaseName);
        assertThat(asylumCase.read(CASE_NAME_HMCTS_INTERNAL, String.class).get())
            .isEqualTo(expectedCaseName);
    }

    @Test
    void transposes_and_format_asylum_case_name_if_already_exists_and_is_incorrect() {

        final String expectedCaseName = "John Smith";
        asylumCase.write(APPELLANT_GIVEN_NAMES, "John ");
        asylumCase.write(APPELLANT_FAMILY_NAME, "  Smith");
        asylumCase.write(HMCTS_CASE_NAME_INTERNAL, "Incorrect-CaseName");
        asylumCase.write(CASE_NAME_HMCTS_INTERNAL, "Incorrect-CaseName");

        asylumFieldCaseNameFixer.fix(asylumCase);

        assertThat(asylumCase.read(HMCTS_CASE_NAME_INTERNAL, String.class).get())
            .isEqualTo(expectedCaseName);
        assertThat(asylumCase.read(CASE_NAME_HMCTS_INTERNAL, String.class).get())
            .isEqualTo(expectedCaseName);
    }

    @Test
    void does_not_set_case_name_when_appellant_given_names_is_not_present() {

        final String expectedCaseName = "Some-CaseName";
        asylumCase.write(APPELLANT_FAMILY_NAME, "Smith");
        asylumCase.write(HMCTS_CASE_NAME_INTERNAL, expectedCaseName);
        asylumCase.write(CASE_NAME_HMCTS_INTERNAL, expectedCaseName);

        asylumFieldCaseNameFixer.fix(asylumCase);

        assertThat(asylumCase.read(HMCTS_CASE_NAME_INTERNAL, String.class).get())
            .isEqualTo(expectedCaseName);
        assertThat(asylumCase.read(CASE_NAME_HMCTS_INTERNAL, String.class).get())
            .isEqualTo(expectedCaseName);
    }

    @Test
    void does_not_set_case_name_when_appellant_family_name_is_not_present() {

        final String expectedCaseName = "Some-CaseName";
        asylumCase.write(APPELLANT_GIVEN_NAMES, "John ");
        asylumCase.write(HMCTS_CASE_NAME_INTERNAL, expectedCaseName);
        asylumCase.write(CASE_NAME_HMCTS_INTERNAL, expectedCaseName);

        asylumFieldCaseNameFixer.fix(asylumCase);

        assertThat(asylumCase.read(HMCTS_CASE_NAME_INTERNAL, String.class).get())
            .isEqualTo(expectedCaseName);
        assertThat(asylumCase.read(CASE_NAME_HMCTS_INTERNAL, String.class).get())
            .isEqualTo(expectedCaseName);
    }
}
