package io.sensefly.logging.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.apache.logging.log4j.core.layout.PatternLayout.SIMPLE_CONVERSION_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class CloudWatchAppenderTest {

  @Mock
  private CloudWatchLogService cloudWatchLogService;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testLogger() {

    Layout layout = PatternLayout.newBuilder().withPattern(SIMPLE_CONVERSION_PATTERN).build();
    CloudWatchAppender appender = new CloudWatchAppender("test-appender", 1, 128, layout, cloudWatchLogService);

    appender.start();
    try {
      LogEvent event = Log4jLogEvent.newBuilder()
          .setLoggerName("TestLogger")
          .setLoggerFqcn(CloudWatchAppenderTest.class.getName())
          .setLevel(Level.INFO)
          .setMessage(new SimpleMessage("Test"))
          .build();

      assertThat(appender.isStarted()).isTrue();

      appender.append(event);

      verify(cloudWatchLogService, timeout(5000).times(1)).sendMessages(anyList());

    } finally {
      appender.stop();
    }

  }

}