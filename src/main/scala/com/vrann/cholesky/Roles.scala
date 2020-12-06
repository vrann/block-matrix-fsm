package com.vrann.cholesky

import com.vrann.{BlockRole, Position}

object Test extends BlockRole {}

object Roles {
  def apply(pos: Position): BlockRole = {
    if (pos.x < 0) Test
    else if (pos.x == pos.y) Diagonal
    else Subdiagonal
  }
}
