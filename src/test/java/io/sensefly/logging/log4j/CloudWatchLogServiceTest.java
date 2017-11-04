package io.sensefly.logging.log4j;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.*;
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
import static org.mockito.Mockito.never;
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
  public void log_group_and_stream_should_not_be_created_if_not_existing() {
    DescribeLogGroupsRequest groupRequest = new DescribeLogGroupsRequest().withLogGroupNamePrefix(GROUP_NAME);
    DescribeLogGroupsResult groupResult = new DescribeLogGroupsResult()
        .withLogGroups(new LogGroup().withLogGroupName(GROUP_NAME));
    when(awsLogs.describeLogGroups(groupRequest))
        .thenReturn(groupResult);

    DescribeLogStreamsRequest streamsRequest = new DescribeLogStreamsRequest()
        .withLogGroupName(GROUP_NAME)
        .withLogStreamNamePrefix(STREAM_NAME_PREFIX);
    DescribeLogStreamsResult streamsResult = new DescribeLogStreamsResult()
        .withLogStreams(new LogStream().withLogStreamName(STREAM_NAME_PREFIX));
    when(awsLogs.describeLogStreams(streamsRequest))
        .thenReturn(streamsResult);

    new CloudWatchLogService(GROUP_NAME, STREAM_NAME_PREFIX, awsLogs, clock);
    verify(awsLogs, never()).createLogGroup(any(CreateLogGroupRequest.class));
    verify(awsLogs, never()).createLogStream(any(CreateLogStreamRequest.class));
  }

  @Test
  public void log_group_and_stream_should_be_created_if_not_existing() {
    DescribeLogGroupsRequest groupRequest = new DescribeLogGroupsRequest().withLogGroupNamePrefix(GROUP_NAME);
    when(awsLogs.describeLogGroups(groupRequest))
        .thenReturn(new DescribeLogGroupsResult());

    DescribeLogStreamsRequest streamsRequest = new DescribeLogStreamsRequest()
        .withLogGroupName(GROUP_NAME)
        .withLogStreamNamePrefix(STREAM_NAME);
    when(awsLogs.describeLogStreams(streamsRequest))
        .thenReturn(new DescribeLogStreamsResult());

    new CloudWatchLogService(GROUP_NAME, STREAM_NAME_PREFIX, awsLogs, clock);

    ArgumentCaptor<CreateLogGroupRequest> groupCaptor = ArgumentCaptor.forClass(CreateLogGroupRequest.class);
    verify(awsLogs, times(1)).createLogGroup(groupCaptor.capture());
    assertThat(groupCaptor.getValue().getLogGroupName()).isEqualTo(GROUP_NAME);

    ArgumentCaptor<CreateLogStreamRequest> streamCaptor = ArgumentCaptor.forClass(CreateLogStreamRequest.class);
    verify(awsLogs, times(1)).createLogStream(streamCaptor.capture());
    assertThat(streamCaptor.getValue().getLogGroupName()).isEqualTo(GROUP_NAME);
    assertThat(streamCaptor.getValue().getLogStreamName()).isEqualTo(STREAM_NAME);
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

  @Test
  public void stream_name_should_support_prefix() {
    CloudWatchLogService service = new CloudWatchLogService(GROUP_NAME, STREAM_NAME_PREFIX, awsLogs, clock);

    long now = System.currentTimeMillis();
    service.sendMessages(newArrayList(new InputLogEvent().withMessage("message 1").withTimestamp(now)));

    ArgumentCaptor<PutLogEventsRequest> captor = ArgumentCaptor.forClass(PutLogEventsRequest.class);
    verify(awsLogs, times(1)).putLogEvents(captor.capture());

    PutLogEventsRequest request = captor.getValue();
    assertThat(request.getLogStreamName()).isEqualTo(STREAM_NAME);
  }

  @Test
  public void stream_name_should_support_no_prefix() {
    CloudWatchLogService service = new CloudWatchLogService(GROUP_NAME, null, awsLogs, clock);

    long now = System.currentTimeMillis();
    service.sendMessages(newArrayList(new InputLogEvent().withMessage("message 1").withTimestamp(now)));

    ArgumentCaptor<PutLogEventsRequest> captor = ArgumentCaptor.forClass(PutLogEventsRequest.class);
    verify(awsLogs, times(1)).putLogEvents(captor.capture());

    PutLogEventsRequest request = captor.getValue();
    assertThat(request.getLogStreamName()).isEqualTo("2017/09");
  }
}