package io.sensefly.logging.log4j;

class CloudWatchDebugger {

  @SuppressWarnings("AccessOfSystemProperties")
  private static final boolean DEBUG_MODE = System.getProperty("log4j.debug") != null;

  private CloudWatchDebugger() {}

  static void debug(String message) {
    debug(message, null);
  }

  @SuppressWarnings({ "UseOfSystemOutOrSystemErr", "CallToPrintStackTrace", "squid:S1148", "squid:S899", "squid:S106" })
  static void debug(String message, Throwable throwable) {
    if(DEBUG_MODE) {
      System.out.println(message);
      if(throwable != null) throwable.printStackTrace();
    }
  }

}
