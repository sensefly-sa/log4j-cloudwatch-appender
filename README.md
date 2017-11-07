# Log4j 2.x appender for AWS CloudWatch logs

[![Build Status](https://travis-ci.org/sensefly-sa/log4j-cloudwatch-appender.svg?branch=master)](https://travis-ci.org/sensefly-sa/log4j-cloudwatch-appender)
[![codecov](https://codecov.io/gh/sensefly-sa/log4j-cloudwatch-appender/branch/master/graph/badge.svg)](https://codecov.io/gh/sensefly-sa/log4j-cloudwatch-appender)
[![Download](https://api.bintray.com/packages/sensefly/maven/log4j-cloudwatch-appender/images/download.svg) ](https://bintray.com/sensefly/maven/log4j-cloudwatch-appender/_latestVersion)


Sends logs to specified `logGroupName`.   
Creates log streams with optional `logStreamNamePrefix` and year/month. 
Log stream is created only on app startup... so a long running app won't create log stream for every months. 

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

## AWS credentials

AWS credentials are read using [DefaultAWSCredentialsProviderChain](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html):
* Environment Variables: `AWS_ACCESS_KEY`, `AWS_SECRET_KEY` and `AWS_REGION`
* Java System Properties: `aws.accessKeyId` and `aws.secretKey`
* Credential profiles file at the default location (`~/.aws/credentials`) shared by all AWS SDKs and the AWS CLI
* Credentials delivered through the Amazon EC2 container service if `AWS_CONTAINER_CREDENTIALS_RELATIVE_URI` environment 
variable is set and security manager has permission to access the variable
* Instance profile credentials delivered through the Amazon EC2 metadata service

## Build

```
./gradlew clean build
```

