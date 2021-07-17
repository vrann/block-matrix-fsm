package com.vrann;

import org.apache.spark.ml.linalg.DenseMatrix;
import org.apache.spark.ml.linalg.Matrix;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnformattedMatrixWriter<T extends Matrix> {

    private DataOutputStream output;

    public static <L extends Matrix> UnformattedMatrixWriter<L> ofStream(DataOutputStream output) {
        return new UnformattedMatrixWriter<L>(output);
    }

    public static <L extends Matrix> UnformattedMatrixWriter<L> ofFile(File file)
            throws IOException
    {
        if (!file.exists()) {
            file.createNewFile();
        }
        DataOutputStream output = new DataOutputStream(new FileOutputStream(file));
        return new UnformattedMatrixWriter<L>(output);
    }

//    public static <L extends Matrix> UnformattedMatrixWriter<L> ofPositionAndMatrixType(
//            Position position, BlockMatrixType matrixType) throws IOException {
//
//        return ofFileLocator((pos, type) -> new File((new StringBuilder())
//                .append(System.getProperty("user.home"))
//                .append("/.actorchoreography/")
//                .append(
//                        String.format("matrix-%s-%d-%d.bin",
//                                type,
//                                pos.getX(),
//                                pos.getY())
//                ).toString()), position, matrixType);
//    }
//
//    public static <L extends Matrix> UnformattedMatrixWriter<L> ofFileLocator(
//            FileLocator fileLocator, Position position, BlockMatrixType matrixType) throws IOException {
//        File file = fileLocator.getFile(position, matrixType);
//        return ofFile(file);
//    }

    private UnformattedMatrixWriter(DataOutputStream output) {
        this.output = output;
    }

    public void writeMatrix(DenseMatrix matrix) throws IOException {
        ByteBuffer numRows = ByteBuffer.allocate(4);
        numRows.order(ByteOrder.LITTLE_ENDIAN);
        numRows.putInt(matrix.numRows());
        byte[] bytes = numRows.array();
        output.write(bytes);

        ByteBuffer numCols = ByteBuffer.allocate(4);
        numCols.order(ByteOrder.LITTLE_ENDIAN);
        numCols.putInt(matrix.numCols());
        bytes = numCols.array();
        output.write(bytes);

        double[] data = matrix.toArray();
        for (int i = 0; i < data.length; i++) {
            ByteBuffer bufferForDouble = ByteBuffer.allocate(8);
            bufferForDouble.order(ByteOrder.LITTLE_ENDIAN);
            bytes = bufferForDouble.putDouble(data[i]).array();
            output.write(bytes);
        }
        output.close();
    }
}


