package io.sensefly.logging.log4j;

import com.amazonaws.services.logs.model.InputLogEvent;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.sensefly.logging.log4j.CloudWatchDebugger.debug;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

@Plugin(name = "CloudWatchAppender", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public class CloudWatchAppender extends AbstractAppender {

  private static final int DEFAULT_QUEUE_LENGTH = 1024;
  private static final int DEFAULT_MESSAGE_BATCH_SIZE = 128;
  private static final int DAEMON_SLEEP = 20;

  /**
   * The queue used to buffer log entries
   */
  private final LinkedBlockingQueue<LogEvent> logEventsQueue;

  /**
   * The maximum number of log entries to send in one go to the AWS CloudWatch Log service
   */
  private final int messagesBatchSize;

  private final AtomicBoolean appenderInitialised = new AtomicBoolean(false);

  private final CloudWatchLogService cloudWatchLogService;

  private CloudWatchAppender(
      String name,
      String logGroupName,
      String logStreamNamePrefix,
      int queueLength,
      int messagesBatchSize,
      Layout<Serializable> layout) {
    this(name, queueLength, messagesBatchSize, layout, new CloudWatchLogService(logGroupName, logStreamNamePrefix));
  }

  // visible for testing
  CloudWatchAppender(
      String name,
      int queueLength,
      int messagesBatchSize,
      Layout<Serializable> layout,
      CloudWatchLogService cloudWatchLogService) {
    super(name, null, layout == null ? PatternLayout.createDefaultLayout() : layout, false);
    this.messagesBatchSize = messagesBatchSize;
    this.cloudWatchLogService = cloudWatchLogService;
    logEventsQueue = new LinkedBlockingQueue<>(queueLength);

    initDaemon();
    appenderInitialised.set(true);
  }

  @PluginFactory
  public static CloudWatchAppender createAppender(
      @PluginAttribute(value = "name", defaultString = "CloudWatchAppender") String name,
      @PluginAttribute("logGroupName") @Required(message = "logGroupName is required") String logGroupName,
      @PluginAttribute("logStreamNamePrefix") String logStreamNamePrefix,
      @PluginAttribute(value = "queueLength", defaultInt = DEFAULT_QUEUE_LENGTH) int queueLength,
      @PluginAttribute(value = "messagesBatchSize", defaultInt = DEFAULT_MESSAGE_BATCH_SIZE) int messagesBatchSize,
      @PluginElement("Layout") Layout<Serializable> layout) {
    return new CloudWatchAppender(name, logGroupName, logStreamNamePrefix, queueLength, messagesBatchSize, layout);
  }

  @Override
  @SuppressWarnings("squid:S899") // Return values should not be ignored when they contain the operation status code
  public void append(LogEvent event) {
    if(appenderInitialised.get()) {
      logEventsQueue.offer(event);
    } else {
      debug("Cannot append as appender not yet initialised");
    }
  }

  private void sendMessages() {

    Collection<LogEvent> loggingEvents = new ArrayList<>();

    LogEvent polledLoggingEvent = logEventsQueue.poll();
    while(polledLoggingEvent != null && loggingEvents.size() <= messagesBatchSize) {
      loggingEvents.add(polledLoggingEvent);
      polledLoggingEvent = logEventsQueue.poll();
    }

    List<InputLogEvent> inputLogEvents = loggingEvents.stream()
        .map(event -> new InputLogEvent()
            .withTimestamp(event.getTimeMillis())
            .withMessage(new String(getLayout().toByteArray(event), UTF_8)))
        .collect(toList());

    cloudWatchLogService.sendMessages(inputLogEvents);
  }

  @Override
  public void stop() {
    super.stop();
    while(logEventsQueue != null && !logEventsQueue.isEmpty()) {
      sendMessages();
    }
  }

  private void initDaemon() {
    new Thread(() -> {
      while(true) {
        try {
          if(!logEventsQueue.isEmpty()) {
            sendMessages();
          }
          Thread.sleep(DAEMON_SLEEP);
        } catch(InterruptedException e) {
          debug("CloudWatch appender error", e);
          // Restore interrupted state...
          Thread.currentThread().interrupt();
        }
      }
    }).start();
  }

}
