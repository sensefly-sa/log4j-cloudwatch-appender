package io.sensefly.logging.log4j;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.CreateLogGroupRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsResult;
import com.amazonaws.services.logs.model.LogGroup;

import java.util.Optional;

import static io.sensefly.logging.log4j.CloudWatchDebugger.debug;

class LogGroupProvider {

  private final AWSLogs awsLogs;

  LogGroupProvider(AWSLogs awsLogs) {
    this.awsLogs = awsLogs;
  }

  void ensureExists(String name) {
    DescribeLogGroupsRequest request = new DescribeLogGroupsRequest().withLogGroupNamePrefix(name);

    DescribeLogGroupsResult groupsResult = awsLogs.describeLogGroups(request);
    if(groupsResult != null) {
      Optional<LogGroup> existing = groupsResult.getLogGroups().stream()
          .filter(logGroup -> logGroup.getLogGroupName().equalsIgnoreCase(name))
          .findFirst();

      if(!existing.isPresent()) {
        debug("Creates LogGroup: " + name);
        awsLogs.createLogGroup(new CreateLogGroupRequest().withLogGroupName(name));
      }
    }
  }
}
