# Log4j 2.x appender for AWS CloudWatch logs

[![Build Status](https://travis-ci.org/sensefly-sa/log4j-cloudwatch-appender.svg?branch=master)](https://travis-ci.org/sensefly-sa/log4j-cloudwatch-appender)
[ ![Download](https://api.bintray.com/packages/sensefly/maven/log4j-cloudwatch-appender/images/download.svg) ](https://bintray.com/sensefly/maven/log4j-cloudwatch-appender/_latestVersion)


Sends logs to specified `logGroupName`.   
Creates log streams with optional `logStreamNamePrefix` and year/month. 
Log stream is created only on app startup... so a long running app won't create log stream for every months. 


## Build

```
./gradlew clean test
```

## log4j2.xml example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">

  <Properties>
    <Property name="PID">????</Property>
    <Property name="LOG_EXCEPTION_CONVERSION_WORD">%xwEx</Property>
    <Property name="LOG_LEVEL_PATTERN">%5p</Property>
    <Property name="CONSOLE_LOG_PATTERN">%clr{%d{yyyy-MM-dd HH:mm:ss.SSS}}{faint} %clr{${LOG_LEVEL_PATTERN}}%clr{${sys:PID}}{magenta} %clr{---}{faint} %clr{[%15.15t]}{faint} %clr{%-40.40c{1.}}{cyan} %clr{:}{faint}%m%n${sys:LOG_EXCEPTION_CONVERSION_WORD}</Property>
    <Property name="CLOUDWATCH_LOG_PATTERN">${LOG_LEVEL_PATTERN} ${sys:PID} --- [%t]%-40.40c{1.} : %m%n${sys:LOG_EXCEPTION_CONVERSION_WORD}</Property>
  </Properties>

  <Appenders>

    <Console name="Console" target="SYSTEM_OUT" follow="true">
      <PatternLayout pattern="${sys:CONSOLE_LOG_PATTERN}"/>
    </Console>

    <CloudWatchAppender name="CloudWatch" logGroupName="test-logging" logStreamNamePrefix="logging">
      <PatternLayout>
        <Pattern>${sys:CLOUDWATCH_LOG_PATTERN}</Pattern>
      </PatternLayout>
    </CloudWatchAppender>

  </Appenders>

  <Loggers>
    <Root level="info">
      <AppenderRef ref="Console"/>
      <AppenderRef ref="CloudWatch"/>
    </Root>
  </Loggers>

</Configuration>

```

