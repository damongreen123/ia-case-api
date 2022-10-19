package uk.gov.hmcts.reform.iacaseapi.infrastructure.clients;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.UserDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;


@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class CcdSupplementaryUpdaterTest {

    private static final String SERVICE_TOKEN = "ABCDEF";
    private static final String ACCESS_TOKEN = "12345";

    private CcdSupplementaryUpdater ccdSupplementaryUpdater;
    private String ccdUrl = "some-host";
    private String ccdSupplementaryApiPath = "some-path";
    private String hmctsServiceId = "some-id";

    @Mock private AuthTokenGenerator serviceAuthTokenGenerator;
    @Mock private RestTemplate restTemplate;
    @Mock private ResponseEntity<Object> responseEntity;

    @Mock private Callback<AsylumCase> callback;
    @Mock private CaseDetails<AsylumCase> caseDetails;
    @Mock private UserDetails userDetails;

    @BeforeEach
    public void setUp() {

        when(serviceAuthTokenGenerator.generate()).thenReturn(SERVICE_TOKEN);
        when(userDetails.getAccessToken()).thenReturn(ACCESS_TOKEN);

        ccdSupplementaryUpdater = new CcdSupplementaryUpdater(
                restTemplate,
                serviceAuthTokenGenerator,
                userDetails,
                ccdUrl,
                ccdSupplementaryApiPath,
                hmctsServiceId);
    }

    @Test
    void should_sent_post_to_update_ccd_and_receive_201() {

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getId()).thenReturn(123L);

        when(restTemplate
            .exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Object.class)
            )
        ).thenReturn(responseEntity);

        when(responseEntity.getStatusCodeValue()).thenReturn(HttpStatus.CREATED.value());

        ccdSupplementaryUpdater.setSupplementaryValues(callback, singletonMap("HMCTSServiceId", hmctsServiceId));

        verify(restTemplate)
            .exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Object.class)
            );

    }

    @Test
    void should_handle_when_rest_exception_thrown() {

        RestClientResponseException restClientResponseEx = mock(RestClientResponseException.class);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getId()).thenReturn(123L);

        when(restTemplate
            .exchange(
                eq(ccdUrl + ccdSupplementaryApiPath),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Object.class)
            )
        ).thenThrow(restClientResponseEx);

        assertThatThrownBy(() -> ccdSupplementaryUpdater.setSupplementaryValues(callback,
                singletonMap("HMCTSServiceId", hmctsServiceId)))
            .isInstanceOf(CcdDataIntegrationException.class)
            .hasMessage("Couldn't update CCD case supplementary data using API: "
                + ccdUrl
                + ccdSupplementaryApiPath)
            .hasCauseInstanceOf(RestClientResponseException.class);

        verify(restTemplate)
            .exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Object.class)
            );

    }

    @Test
    void should_throw_when_callback_param_is_null() {

        assertThatThrownBy(() -> ccdSupplementaryUpdater.setSupplementaryValues(null, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

    }

    @Test
    void should_throw_when_callback_appellant_flag_param_is_null() {

        assertThatThrownBy(() -> ccdSupplementaryUpdater.setSupplementaryValues(null, null))
                .hasMessage("callback must not be null")
                .isExactlyInstanceOf(NullPointerException.class);

    }

}
