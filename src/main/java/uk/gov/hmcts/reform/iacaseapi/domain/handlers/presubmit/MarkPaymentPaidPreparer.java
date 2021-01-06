package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AppealType.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.RemissionDecision.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.RemissionType.*;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AppealType;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.RemissionDecision;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.RemissionType;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.PaymentStatus;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;

@Component
public class MarkPaymentPaidPreparer implements PreSubmitCallbackHandler<AsylumCase> {

    private final boolean isfeePaymentEnabled;

    public MarkPaymentPaidPreparer(
        @Value("${featureFlag.isfeePaymentEnabled}") boolean isfeePaymentEnabled
    ) {
        this.isfeePaymentEnabled = isfeePaymentEnabled;
    }

    public boolean canHandle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return callbackStage == PreSubmitCallbackStage.ABOUT_TO_START
               && callback.getEvent() == Event.MARK_APPEAL_PAID
               && isfeePaymentEnabled;
    }

    public PreSubmitCallbackResponse<AsylumCase> handle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final AsylumCase asylumCase = callback.getCaseDetails().getCaseData();
        final PreSubmitCallbackResponse<AsylumCase> callbackResponse = new PreSubmitCallbackResponse<>(asylumCase);

        Optional<PaymentStatus> optPaymentStatus = asylumCase.read(PAYMENT_STATUS, PaymentStatus.class);
        if (optPaymentStatus.isPresent() && optPaymentStatus.get() == PaymentStatus.PAID) {

            callbackResponse.addError("The fee for this appeal has already been paid.");
        }

        asylumCase.read(APPEAL_TYPE, AppealType.class)
            .ifPresent(appealType -> {
                switch (appealType) {
                    case EA:
                    case HU:
                    case PA:
                        RemissionType remissionType = asylumCase.read(REMISSION_TYPE, RemissionType.class)
                            .orElseThrow(() -> new IllegalStateException("Remission type is not present"));
                        Optional<RemissionDecision> remissionDecision = asylumCase.read(REMISSION_DECISION, RemissionDecision.class);

                        if (appealType == PA && remissionType == NO_REMISSION) {

                            asylumCase.read(PA_APPEAL_TYPE_PAYMENT_OPTION, String.class)
                                .filter(option -> option.equals("payLater"))
                                .ifPresent(s ->
                                    callbackResponse.addError("The Mark appeal as paid option is not available.")
                                );
                        } else if (remissionType != NO_REMISSION && !remissionDecision.isPresent()) {

                            callbackResponse.addError("You cannot mark this appeal as paid because the remission decision has not been recorded.");
                        } else if (remissionType != NO_REMISSION && remissionDecision.isPresent() && remissionDecision.get() == APPROVED) {

                            callbackResponse.addError("You cannot mark this appeal as paid because a full remission has been approved.");
                        }
                        break;

                    case RP:
                    case DC:
                        callbackResponse.addError("Payment is not required for this type of appeal.");
                        break;

                    default:
                        break;
                }
            });

        return callbackResponse;
    }
}