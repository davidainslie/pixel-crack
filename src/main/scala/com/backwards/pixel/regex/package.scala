package com.backwards.pixel

import scala.util.matching.Regex

package object regex {
  implicit class R(sc: StringContext) {
    def r: Regex = sc.parts.mkString.r
  }
}