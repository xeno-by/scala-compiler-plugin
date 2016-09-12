// Copyright: 2010 - 2016 Rory Graves, Sam Halliday
// License: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.reflect.internal.util.Position

trait BackCompat {
  implicit class RichPosition(pos: Position) {
    def startOrCursor: Int = pos.start
    def endOrCursor: Int = pos.end
  }
}
