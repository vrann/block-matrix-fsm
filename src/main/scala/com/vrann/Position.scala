package com.vrann

case class Position(x: Int, y: Int) {
  override def toString: String = s"$x-$y"
}
