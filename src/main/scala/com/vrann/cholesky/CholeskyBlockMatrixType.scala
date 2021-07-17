package com.vrann.cholesky

import com.fasterxml.jackson.annotation.JsonValue
import com.vrann.BlockMatrixType

object CholeskyBlockMatrixType {
  case object A11 extends BlockMatrixType { @JsonValue override val jsonValue: String = "A11" }
  case object L11 extends BlockMatrixType { @JsonValue override val jsonValue: String = "L11" }
  case object L21 extends BlockMatrixType { @JsonValue override val jsonValue: String = "L21" }
  case object A22 extends BlockMatrixType { @JsonValue override val jsonValue: String = "A22" }
  case object aMN extends BlockMatrixType { @JsonValue override val jsonValue: String = "aMN" }
  case object L12 extends BlockMatrixType { @JsonValue override val jsonValue: String = "L12" }
}
