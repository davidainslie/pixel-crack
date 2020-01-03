package com.backwards.pixel.log

import scribe.Level
import scribe.Level._
import com.backwards.pixel.regex._

trait ScribeConfig {
  val defaultLogLevel: Level = Info

  val logLevel: Level =
    sys.props.getOrElse("log.level", defaultLogLevel.name) match {
      case r"(?i)trace" => Trace
      case r"(?i)debug" => Debug
      case r"(?i)info" => Info
      case r"(?i)warn" => Warn
      case r"(?i)error" => Error
      case _ => defaultLogLevel
    }

  scribe.Logger.root.clearHandlers().clearModifiers().withHandler(minimumLevel = Option(logLevel)).replace()
}