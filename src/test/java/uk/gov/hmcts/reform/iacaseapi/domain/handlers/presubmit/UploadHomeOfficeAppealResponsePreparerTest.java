package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.DocumentTag;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.DocumentWithMetadata;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.Document;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.IdValue;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class UploadHomeOfficeAppealResponsePreparerTest {

    @Mock private Callback<AsylumCase> callback;
    @Mock private CaseDetails<AsylumCase> caseDetails;
    @Mock private AsylumCase asylumCase;

    @Mock private DocumentWithMetadata respondentEvidence1WithMetadata;
    @Mock private DocumentWithMetadata respondentEvidence2WithMetadata;
    @Mock private Document document1;
    @Mock private Document document2;
    final String appealResponse01FileName = "Evidence01";
    final String appealResponse02FileName = "Evidence02";
    @Captor ArgumentCaptor<String> fileNames;


    UploadHomeOfficeAppealResponsePreparer uploadHomeOfficeAppealResponsePreparer;

    @BeforeEach
    void setUp() {

        uploadHomeOfficeAppealResponsePreparer =
            new UploadHomeOfficeAppealResponsePreparer();
    }


    @Test
    void should_set_errors_if_upload_action_is_not_available_for_home_office_event() {

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(Event.UPLOAD_HOME_OFFICE_APPEAL_RESPONSE);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);

        List<IdValue<DocumentWithMetadata>> respondentDocuments =

            Arrays.asList(
                new IdValue<>("1", respondentEvidence1WithMetadata),
                new IdValue<>("2", respondentEvidence2WithMetadata)
            );

        when(asylumCase.read(UPLOAD_HOME_OFFICE_APPEAL_RESPONSE_ACTION_AVAILABLE)).thenReturn(Optional.of(YesOrNo.NO));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            uploadHomeOfficeAppealResponsePreparer.handle(PreSubmitCallbackStage.ABOUT_TO_START, callback);

        assertThat(callbackResponse.getErrors()).hasSize(1);
        assertTrue(callbackResponse.getErrors().contains("You cannot upload more documents until your response has been reviewed"));
        verify(asylumCase).read(UPLOAD_HOME_OFFICE_APPEAL_RESPONSE_ACTION_AVAILABLE);
    }

    @Test
    void should_not_set_errors_if_upload_action_is_not_available_for_case_officer_event() {

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);

        List<IdValue<DocumentWithMetadata>> respondentDocuments =

            Arrays.asList(
                new IdValue<>("1", respondentEvidence1WithMetadata),
                new IdValue<>("2", respondentEvidence2WithMetadata)
            );

        when(asylumCase.read(RESPONDENT_DOCUMENTS)).thenReturn(Optional.of(respondentDocuments));

        when(respondentEvidence1WithMetadata.getTag()).thenReturn(DocumentTag.APPEAL_RESPONSE);
        when(respondentEvidence2WithMetadata.getTag()).thenReturn(DocumentTag.APPEAL_RESPONSE);

        when(respondentEvidence1WithMetadata.getDocument()).thenReturn(document1);
        when(respondentEvidence2WithMetadata.getDocument()).thenReturn(document2);

        when(document1.getDocumentFilename()).thenReturn(appealResponse01FileName);
        when(document2.getDocumentFilename()).thenReturn(appealResponse02FileName);
        when(callback.getEvent()).thenReturn(Event.ADD_APPEAL_RESPONSE);
        when(asylumCase.read(UPLOAD_HOME_OFFICE_APPEAL_RESPONSE_ACTION_AVAILABLE)).thenReturn(Optional.of(YesOrNo.NO));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            uploadHomeOfficeAppealResponsePreparer.handle(PreSubmitCallbackStage.ABOUT_TO_START, callback);

        assertThat(callbackResponse.getErrors()).hasSize(0);
        verify(asylumCase).read(UPLOAD_HOME_OFFICE_APPEAL_RESPONSE_ACTION_AVAILABLE);
        verify(asylumCase, times(1)).read(RESPONDENT_DOCUMENTS);
        verify(respondentEvidence1WithMetadata, times(1)).getTag();
        verify(respondentEvidence2WithMetadata, times(1)).getTag();
        verify(document1, times(1)).getDocumentFilename();
        verify(document2, times(1)).getDocumentFilename();
        verify(asylumCase).write(eq(UPLOADED_HOME_OFFICE_APPEAL_RESPONSE_DOCS), fileNames.capture());

        final String value = fileNames.getValue();
        assertTrue(value.contains(appealResponse01FileName));
        assertTrue(value.contains(appealResponse02FileName));

    }

    @Test
    void should_not_set_errors_if_upload_action_is_available_for_case_officer_event() {

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);

        List<IdValue<DocumentWithMetadata>> respondentDocuments =

            Arrays.asList(
                new IdValue<>("1", respondentEvidence1WithMetadata),
                new IdValue<>("2", respondentEvidence2WithMetadata)
            );

        when(asylumCase.read(RESPONDENT_DOCUMENTS)).thenReturn(Optional.of(respondentDocuments));

        when(callback.getEvent()).thenReturn(Event.ADD_APPEAL_RESPONSE);
        when(asylumCase.read(UPLOAD_HOME_OFFICE_APPEAL_RESPONSE_ACTION_AVAILABLE)).thenReturn(Optional.of(YesOrNo.YES));
        when(asylumCase.read(RESPONDENT_DOCUMENTS)).thenReturn(Optional.empty());

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            uploadHomeOfficeAppealResponsePreparer.handle(PreSubmitCallbackStage.ABOUT_TO_START, callback);

        assertThat(callbackResponse.getErrors()).hasSize(0);
        verify(asylumCase).read(UPLOAD_HOME_OFFICE_APPEAL_RESPONSE_ACTION_AVAILABLE);
        verify(asylumCase, times(1)).read(RESPONDENT_DOCUMENTS);
        verifyNoInteractions(respondentEvidence1WithMetadata);
        verifyNoInteractions(respondentEvidence2WithMetadata);
        verifyNoInteractions(document1);
        verifyNoInteractions(document2);
        verify(asylumCase).write(eq(UPLOADED_HOME_OFFICE_APPEAL_RESPONSE_DOCS), fileNames.capture());

        final String value = fileNames.getValue();
        assertTrue(value.contains("- None"));

    }

    @Test
    void should_set_uploaded_documents() {

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(Event.UPLOAD_HOME_OFFICE_APPEAL_RESPONSE);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);

        List<IdValue<DocumentWithMetadata>> respondentDocuments =

            Arrays.asList(
                new IdValue<>("1", respondentEvidence1WithMetadata),
                new IdValue<>("2", respondentEvidence2WithMetadata)
            );

        when(asylumCase.read(RESPONDENT_DOCUMENTS)).thenReturn(Optional.of(respondentDocuments));

        when(respondentEvidence1WithMetadata.getTag()).thenReturn(DocumentTag.APPEAL_RESPONSE);
        when(respondentEvidence2WithMetadata.getTag()).thenReturn(DocumentTag.APPEAL_RESPONSE);

        when(respondentEvidence1WithMetadata.getDocument()).thenReturn(document1);
        when(respondentEvidence2WithMetadata.getDocument()).thenReturn(document2);

        when(document1.getDocumentFilename()).thenReturn(appealResponse01FileName);
        when(document2.getDocumentFilename()).thenReturn(appealResponse02FileName);
        when(asylumCase.read(UPLOAD_HOME_OFFICE_APPEAL_RESPONSE_ACTION_AVAILABLE)).thenReturn(Optional.of(YesOrNo.YES));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            uploadHomeOfficeAppealResponsePreparer.handle(PreSubmitCallbackStage.ABOUT_TO_START, callback);

        assertThat(callbackResponse.getErrors()).hasSize(0);

        verify(asylumCase).read(UPLOAD_HOME_OFFICE_APPEAL_RESPONSE_ACTION_AVAILABLE);
        verify(asylumCase, times(1)).read(RESPONDENT_DOCUMENTS);
        verify(respondentEvidence1WithMetadata, times(1)).getTag();
        verify(respondentEvidence2WithMetadata, times(1)).getTag();
        verify(document1, times(1)).getDocumentFilename();
        verify(document2, times(1)).getDocumentFilename();
        verify(asylumCase).write(eq(UPLOADED_HOME_OFFICE_APPEAL_RESPONSE_DOCS), fileNames.capture());

        final String value = fileNames.getValue();
        assertTrue(value.contains(appealResponse01FileName));
        assertTrue(value.contains(appealResponse02FileName));

    }

    @Test
    void handling_should_throw_if_cannot_actually_handle() {

        assertThatThrownBy(() -> uploadHomeOfficeAppealResponsePreparer.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);

        when(callback.getEvent()).thenReturn(Event.SEND_DIRECTION);
        assertThatThrownBy(() -> uploadHomeOfficeAppealResponsePreparer.handle(PreSubmitCallbackStage.ABOUT_TO_START, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    void it_can_handle_callback() {

        for (Event event : Event.values()) {

            when(callback.getEvent()).thenReturn(event);

            for (PreSubmitCallbackStage callbackStage : PreSubmitCallbackStage.values()) {

                boolean canHandle = uploadHomeOfficeAppealResponsePreparer.canHandle(callbackStage, callback);

                if ((callback.getEvent() == Event.UPLOAD_HOME_OFFICE_APPEAL_RESPONSE || callback.getEvent() == Event.ADD_APPEAL_RESPONSE)
                    && callbackStage == PreSubmitCallbackStage.ABOUT_TO_START) {

                    assertTrue(canHandle);
                } else {
                    assertFalse(canHandle);
                }
            }

            reset(callback);
        }
    }

    @Test
    void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> uploadHomeOfficeAppealResponsePreparer.canHandle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> uploadHomeOfficeAppealResponsePreparer.canHandle(PreSubmitCallbackStage.ABOUT_TO_START, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> uploadHomeOfficeAppealResponsePreparer.handle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> uploadHomeOfficeAppealResponsePreparer.handle(PreSubmitCallbackStage.ABOUT_TO_START, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);
    }
}
