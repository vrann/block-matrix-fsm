//package com.vrann;
//
//import org.apache.spark.ml.linalg.Matrix;
//
//import java.io.*;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//
//public class UnformattedMatrixReader<T extends Matrix> {
//
//    private DataInputStream input;
//
//    public static <L extends Matrix> UnformattedMatrixReader<L> ofStream(DataInputStream input) {
//        return new UnformattedMatrixReader<L>(input);
//    }
//
//    public static <L extends Matrix> UnformattedMatrixReader<L> ofFile(File file) throws FileNotFoundException {
//        DataInputStream input = new DataInputStream(new FileInputStream(file));
//        return ofStream(input);
//    }
//
//    public static <L extends Matrix> UnformattedMatrixReader<L> ofPositionAndMatrixType(
//            Position position, BlockMatrixType matrixType) throws FileNotFoundException {
//
//        return ofFileLocator((pos, type) -> new File((new StringBuilder())
//                .append(System.getProperty("user.home"))
//                .append("/.actorchoreography/")
//                .append(
//                        String.format("matrix-%s-%d-%d.bin",
//                                type,
//                                pos.x,
//                                pos.getY())
//                ).toString()), position, matrixType);
//    }
//
//    public static <L extends Matrix> UnformattedMatrixReader<L> ofFileLocator(
//        FileLocator fileLocator, Position position, BlockMatrixType matrixType) throws FileNotFoundException {
//        File file = fileLocator.getFile(position, matrixType);
//        System.out.println(file.getPath());
//        return ofFile(file);
//    }
//
//    private UnformattedMatrixReader(DataInputStream input) {
//        this.input = input;
//    }
//
//    // should return instance of the Matrix type we would be working with
//    public T readMatrix(MatrixFactory<T> matrixFactory) throws IOException {
//
//        // read the number of rows
//
//        byte[] bufLengthInt = new byte[4];
//        input.read(bufLengthInt);
////
////        while (input.read(bufLengthInt) != -1) {
////            for (byte b: bufLengthInt) {
////                System.out.println(String.format("%x ", b));
////            }
////        }
////        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
////        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
////        byteBuffer.put(bufLengthInt);
////        for (byte b: byteBuffer.array()) {
////            System.out.println(String.format("%x ", b));
////        }
////        int numRows = byteBuffer.getInt();
//        int numRows = ByteBuffer.wrap(bufLengthInt).order(ByteOrder.LITTLE_ENDIAN).getInt();
//
//        //read the number of columns
//        byte[] bufWidthInt = new byte[4];
//        input.read(bufWidthInt);
//        int numCols = ByteBuffer.wrap(bufWidthInt).order(ByteOrder.LITTLE_ENDIAN).getInt();
//
//        //read the data
//        byte[] ibufDouble = new byte[8];
//        double[] matrixData = new double[input.available() / 8];
//
//        int i = 0;
//        while (input.read(ibufDouble) != -1) {
//            double value = ByteBuffer.wrap(ibufDouble).order(ByteOrder.LITTLE_ENDIAN).getDouble();
//            matrixData[i] = value;
//            i++;
//        }
//
////        System.out.println(numRows);
////        System.out.println(numCols);
////        System.out.println(matrixData.length);
//
//        input.close();
//        return matrixFactory.createMatrix(numRows, numCols, matrixData);
//    }
//}
//
//
