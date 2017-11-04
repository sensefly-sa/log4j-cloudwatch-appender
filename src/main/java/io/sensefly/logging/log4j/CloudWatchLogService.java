package io.sensefly.logging.log4j;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static io.sensefly.logging.log4j.CloudWatchDebugger.debug;

class CloudWatchLogService {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

  private final String logGroupName;
  private final String logStreamName;
  private final AWSLogs awsLogs;
  private final AtomicReference<String> lastSequenceToken = new AtomicReference<>();

  CloudWatchLogService(String logGroupName, String logStreamNamePrefix) {
    this(logGroupName, logStreamNamePrefix, AWSLogsClientBuilder.defaultClient(), Clock.systemUTC());
  }

  // visible for testing
  CloudWatchLogService(String logGroupName, String logStreamNamePrefix, AWSLogs awsLogs, Clock clock) {
    this.logGroupName = logGroupName;
    this.awsLogs = awsLogs;
    logStreamName = buildLogStreamName(logStreamNamePrefix, clock);
    setupLogGroup();
    setupLogStream();
  }

  private String buildLogStreamName(String logStreamNamePrefix, Clock clock) {
    String date = FORMATTER.format(LocalDate.now(clock));
    if(logStreamNamePrefix == null || logStreamNamePrefix.trim().isEmpty()) {
      return date;
    }
    return logStreamNamePrefix + "/" + date;
  }

  @SuppressWarnings("OverlyBroadCatchBlock")
  void sendMessages(List<InputLogEvent> inputLogEvents) {
    try {
      PutLogEventsRequest request = new PutLogEventsRequest(logGroupName, logStreamName, inputLogEvents)
          .withSequenceToken(lastSequenceToken.get());
      send(request);
    } catch(Exception e) {
      debug("Error while sending logs:", e);
    }
  }

  private void setupLogGroup() {
    DescribeLogGroupsRequest request = new DescribeLogGroupsRequest().withLogGroupNamePrefix(logGroupName);

    DescribeLogGroupsResult groupsResult = awsLogs.describeLogGroups(request);
    if(groupsResult != null) {
      Optional<LogGroup> existing = groupsResult.getLogGroups().stream()
          .filter(logGroup -> logGroup.getLogGroupName().equalsIgnoreCase(logGroupName))
          .findFirst();

      if(!existing.isPresent()) {
        debug("Creates LogGroup: " + logGroupName);
        awsLogs.createLogGroup(new CreateLogGroupRequest().withLogGroupName(logGroupName));
      }
    }
  }

  private void setupLogStream() {
    DescribeLogStreamsRequest request = new DescribeLogStreamsRequest()
        .withLogGroupName(logGroupName)
        .withLogStreamNamePrefix(logStreamName);

    DescribeLogStreamsResult streamsResult = awsLogs.describeLogStreams(request);
    if(streamsResult != null) {
      Optional<LogStream> existing = streamsResult.getLogStreams().stream()
          .filter(logStream -> logStream.getLogStreamName().equalsIgnoreCase(logStreamName))
          .findFirst();

      if(!existing.isPresent()) {
        debug("Creates LogStream: " + logStreamName + " in LogGroup: " + logGroupName);
        awsLogs.createLogStream(new CreateLogStreamRequest().withLogGroupName(logGroupName)
            .withLogStreamName(logStreamName));
      }
    }
  }

  private void send(PutLogEventsRequest request) {
    try {
      PutLogEventsResult result = awsLogs.putLogEvents(request);
      lastSequenceToken.set(result.getNextSequenceToken());
    } catch(InvalidSequenceTokenException e) {
      debug("InvalidSequenceTokenException while sending logs", e);
      request.setSequenceToken(e.getExpectedSequenceToken());
      PutLogEventsResult result = awsLogs.putLogEvents(request);
      lastSequenceToken.set(result.getNextSequenceToken());
    }
  }

}
