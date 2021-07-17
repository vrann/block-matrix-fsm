package com.vrann.positioned

import com.vrann.Position

object PositionMatrix {
  def apply(M: Int, N: Int, sectionIds: List[Int]): PositionMatrix = {
    new PositionMatrix(M, N, sectionIds)
  }
}

class PositionMatrix(M: Int, N: Int, sectionIds: List[Int]) {

  var sections = Map.empty[String, List[Position]]
  reassign(sectionIds)

  def reassign(positions: List[Int]): PositionMatrix = {
    val sectionsNumber = positions.length
    if (sectionsNumber == 0) {
      return this
    }
    val sectionsSize = M / sectionsNumber
    var sectionId = 0
    for (i <- 0 until sectionsNumber) {
      val columnNum = i until sectionsNumber
      //      val isLastColumn = i == sectionsNumber - 1
      for (j <- columnNum) {
        val isLastRow = j == sectionsNumber - 1
        val x = i * sectionsSize
        val y = j * sectionsSize

        var sectionPositions = sections.get(sectionId.toString) match {
          case None    => List()
          case Some(b) => b
        }

        val endColumnElement = if (isLastRow) sectionsSize * 2 else sectionsSize
        for (k <- 0 until endColumnElement) {
          val endRowElement = if (isLastRow) sectionsSize * 2 - k else sectionsSize

          for (l <- 0 until endRowElement) {
            sectionPositions :+= Position(x + l, y + k + l)
          }
        }
        sections += (sectionId.toString -> sectionPositions)
      }
      sectionId += 1
    }
    this
  }

  def ++(positions: List[Int]): PositionMatrix = new PositionMatrix(M, N, sectionIds ++ positions)

  def --(positions: List[Int]): PositionMatrix = new PositionMatrix(M, N, sectionIds diff positions)
}
