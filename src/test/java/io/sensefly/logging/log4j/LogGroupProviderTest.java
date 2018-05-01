package io.sensefly.logging.log4j;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.CreateLogGroupRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsResult;
import com.amazonaws.services.logs.model.LogGroup;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LogGroupProviderTest {

  private static final String GROUP_NAME = "test_group";

  @Mock
  private AWSLogs awsLogs;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void log_group_should_not_be_created_if_already_exists() {
    DescribeLogGroupsRequest request = new DescribeLogGroupsRequest().withLogGroupNamePrefix(GROUP_NAME);
    DescribeLogGroupsResult result = new DescribeLogGroupsResult()
        .withLogGroups(new LogGroup().withLogGroupName(GROUP_NAME));
    when(awsLogs.describeLogGroups(request))
        .thenReturn(result);

    LogGroupProvider provider = new LogGroupProvider(awsLogs);
    provider.ensureExists(GROUP_NAME);

    verify(awsLogs, never()).createLogGroup(any(CreateLogGroupRequest.class));
  }

  @Test
  public void log_group_should_be_created_if_not_existing() {
    DescribeLogGroupsRequest request = new DescribeLogGroupsRequest().withLogGroupNamePrefix(GROUP_NAME);
    when(awsLogs.describeLogGroups(request))
        .thenReturn(new DescribeLogGroupsResult());

    LogGroupProvider provider = new LogGroupProvider(awsLogs);
    provider.ensureExists(GROUP_NAME);

    ArgumentCaptor<CreateLogGroupRequest> groupCaptor = ArgumentCaptor.forClass(CreateLogGroupRequest.class);
    verify(awsLogs, times(1)).createLogGroup(groupCaptor.capture());
    assertThat(groupCaptor.getValue().getLogGroupName()).isEqualTo(GROUP_NAME);
  }
}