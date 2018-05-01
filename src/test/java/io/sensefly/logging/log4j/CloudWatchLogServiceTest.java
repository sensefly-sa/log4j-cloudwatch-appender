package io.sensefly.logging.log4j;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.emptyList;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("ResultOfObjectAllocationIgnored")
public class CloudWatchLogServiceTest {

  private static final LocalDate CLOCK_DATE = LocalDate.of(2017, 9, 1);
  private static final String GROUP_NAME = "test_group";
  private static final String STREAM_NAME_PREFIX = "test_stream";
  private static final String STREAM_NAME = "test_stream/2017/09";

  @Mock
  private AWSLogs awsLogs;

  private final Clock clock = Clock.fixed(CLOCK_DATE.atStartOfDay().toInstant(UTC), UTC.normalized());

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void should_send_message() {
    CloudWatchLogService service = new CloudWatchLogService(GROUP_NAME, STREAM_NAME_PREFIX, awsLogs, clock);

    long now = System.currentTimeMillis();
    List<InputLogEvent> events = newArrayList(
        new InputLogEvent().withMessage("message 1").withTimestamp(now),
        new InputLogEvent().withMessage("message 2").withTimestamp(now));

    service.sendMessages(events);

    ArgumentCaptor<PutLogEventsRequest> captor = ArgumentCaptor.forClass(PutLogEventsRequest.class);
    verify(awsLogs, times(1)).putLogEvents(captor.capture());

    PutLogEventsRequest request = captor.getValue();
    assertThat(request.getLogGroupName()).isEqualTo(GROUP_NAME);
    assertThat(request.getLogStreamName()).isEqualTo(STREAM_NAME);
    assertThat(request.getLogEvents()).hasSize(2);
  }

  @Test
  public void send_message_should_support_sequence_token_error() {
    CloudWatchLogService service = new CloudWatchLogService(GROUP_NAME, STREAM_NAME_PREFIX, awsLogs, clock);
    when(awsLogs.putLogEvents(any(PutLogEventsRequest.class)))
        .thenThrow(new InvalidSequenceTokenException("error").withExpectedSequenceToken("token"));

    service.sendMessages(emptyList());

    verify(awsLogs, times(2)).putLogEvents(any(PutLogEventsRequest.class));
  }


}