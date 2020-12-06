package com.vrann.cholesky;

import com.github.fommil.netlib.LAPACK;
import org.apache.spark.ml.linalg.DenseMatrix;
import org.netlib.util.intW;

public class Factorization {

    public static DenseMatrix apply(DenseMatrix A11) {
        intW info = new intW(0);
        double[] L11 = A11.toArray();
        LAPACK.getInstance().dpotrf("L", A11.numCols(), L11, A11.numRows(), info);
        return new DenseMatrix(A11.numRows(), A11.numCols(), L11);
    }
}
