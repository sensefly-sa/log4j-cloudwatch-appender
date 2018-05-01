package io.sensefly.logging.log4j;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsResult;
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsResult;
import com.amazonaws.services.logs.model.LogGroup;
import com.amazonaws.services.logs.model.LogStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.LocalDate;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LogStreamProviderTest {

  private static final LocalDate CLOCK_DATE = LocalDate.of(2017, 9, 1);
  private static final String GROUP_NAME = "test_group";
  private static final String STREAM_NAME_PREFIX = "test_stream";
  private static final String STREAM_NAME = "test_stream/2017/09";

  @Mock
  private AWSLogs awsLogs;

  @Mock
  private Clock clock;

  private final Clock fixedClock = Clock.fixed(CLOCK_DATE.atStartOfDay().toInstant(UTC), UTC.normalized());

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());

    DescribeLogGroupsRequest groupRequest = new DescribeLogGroupsRequest().withLogGroupNamePrefix(GROUP_NAME);
    DescribeLogGroupsResult groupResult = new DescribeLogGroupsResult()
        .withLogGroups(new LogGroup().withLogGroupName(GROUP_NAME));
    when(awsLogs.describeLogGroups(groupRequest))
        .thenReturn(groupResult);
  }

  @Test
  public void log_stream_should_not_be_created_if_already_exists() {
    DescribeLogStreamsRequest streamsRequest = new DescribeLogStreamsRequest()
        .withLogGroupName(GROUP_NAME)
        .withLogStreamNamePrefix(STREAM_NAME_PREFIX);
    DescribeLogStreamsResult streamsResult = new DescribeLogStreamsResult()
        .withLogStreams(new LogStream().withLogStreamName(STREAM_NAME_PREFIX));
    when(awsLogs.describeLogStreams(streamsRequest))
        .thenReturn(streamsResult);

    LogStreamProvider provider = new LogStreamProvider(awsLogs, clock);
    provider.getName(STREAM_NAME_PREFIX, GROUP_NAME);

    verify(awsLogs, never()).createLogStream(any(CreateLogStreamRequest.class));
  }

  @Test
  public void log_stream_should_be_created_if_not_existing() {
    DescribeLogStreamsRequest streamsRequest = new DescribeLogStreamsRequest()
        .withLogGroupName(GROUP_NAME)
        .withLogStreamNamePrefix(STREAM_NAME);
    when(awsLogs.describeLogStreams(streamsRequest))
        .thenReturn(new DescribeLogStreamsResult());

    LogStreamProvider provider = new LogStreamProvider(awsLogs, clock);
    String logStream = provider.getName(STREAM_NAME_PREFIX, GROUP_NAME);
    assertThat(logStream).isEqualTo(STREAM_NAME);

    ArgumentCaptor<CreateLogStreamRequest> streamCaptor = ArgumentCaptor.forClass(CreateLogStreamRequest.class);
    verify(awsLogs, times(1)).createLogStream(streamCaptor.capture());
    assertThat(streamCaptor.getValue().getLogGroupName()).isEqualTo(GROUP_NAME);
    assertThat(streamCaptor.getValue().getLogStreamName()).isEqualTo(STREAM_NAME);
  }

  @Test
  public void stream_name_should_support_prefix() {
    LogStreamProvider provider = new LogStreamProvider(awsLogs, clock);
    String logStream = provider.getName(STREAM_NAME_PREFIX, GROUP_NAME);
    assertThat(logStream).isEqualTo(STREAM_NAME);
  }

  @Test
  public void stream_name_should_support_no_prefix() {
    LogStreamProvider provider = new LogStreamProvider(awsLogs, clock);
    String logStream = provider.getName(null, GROUP_NAME);
    assertThat(logStream).isEqualTo("2017/09");
  }

  @Test
  public void log_stream_should_be_created_on_new_month() {
    LogStreamProvider provider = new LogStreamProvider(awsLogs, clock);
    String logStream = provider.getName(STREAM_NAME_PREFIX, GROUP_NAME);
    assertThat(logStream).isEqualTo(STREAM_NAME);

    DescribeLogGroupsRequest groupRequest = new DescribeLogGroupsRequest().withLogGroupNamePrefix(GROUP_NAME);
    when(awsLogs.describeLogGroups(groupRequest))
        .thenReturn(new DescribeLogGroupsResult());

    DescribeLogStreamsRequest streamsRequest = new DescribeLogStreamsRequest()
        .withLogGroupName(GROUP_NAME)
        .withLogStreamNamePrefix("test_stream/2017/10");
    when(awsLogs.describeLogStreams(streamsRequest))
        .thenReturn(new DescribeLogStreamsResult());

    when(clock.instant()).thenReturn(fixedClock.instant().plus(31, DAYS));

    String newLogStream = provider.getName(STREAM_NAME_PREFIX, GROUP_NAME);
    assertThat(newLogStream).isEqualTo("test_stream/2017/10");

    ArgumentCaptor<CreateLogStreamRequest> streamCaptor = ArgumentCaptor.forClass(CreateLogStreamRequest.class);
    verify(awsLogs, times(1)).createLogStream(streamCaptor.capture());
    assertThat(streamCaptor.getValue().getLogGroupName()).isEqualTo(GROUP_NAME);
    assertThat(streamCaptor.getValue().getLogStreamName()).isEqualTo("test_stream/2017/10");

  }

}