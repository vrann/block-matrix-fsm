package com.vrann.cholesky

import com.vrann.BlockMatrixType

object CholeskyBlockMatrixType {
  case object A11 extends BlockMatrixType
  case object L11 extends BlockMatrixType
  case object L21 extends BlockMatrixType
  case object A22 extends BlockMatrixType
  case object aMN extends BlockMatrixType
  case object L12 extends BlockMatrixType
}
