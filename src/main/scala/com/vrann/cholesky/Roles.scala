package com.vrann.cholesky

import com.vrann.{BlockRole, Position}

object Roles {
  def apply(pos: Position): BlockRole = {
    if (pos.x == pos.y) Diagonal
    else Subdiagonal
  }
}
