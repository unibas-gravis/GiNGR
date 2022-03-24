package other.utils

import breeze.linalg.{CSCMatrix, DenseMatrix}

object CSCHelper {
  def eye(N: Int): CSCMatrix[Double] = {
    val mat = CSCMatrix.zeros[Double](N, N)
    (0 until N).foreach(i => mat(i, i) = 1.0)
    mat
  }

  def DenseMatrix2CSCMatrix(mat: DenseMatrix[Double]): CSCMatrix[Double] = {
    CSCMatrix.tabulate(mat.rows, mat.cols)(mat(_, _))
  }

  def CSCMatrixMultipliedWithDouble(mat: CSCMatrix[Double], scale: Double): CSCMatrix[Double] = {
    mat.mapValues(_ * scale)
  }

  def vertcat(matrices: CSCMatrix[Double]*): CSCMatrix[Double] = {
    require(matrices.forall(m => m.cols == matrices(0).cols), "Not all matrices have the same number of columns")
    val numRows = matrices.foldLeft(0)(_ + _.rows)
    val numCols = matrices(0).cols
    val res = CSCMatrix.zeros[Double](numRows, numCols)
    var offset = 0
    for (m <- matrices) {
      var i = 0
      while (i < m.cols) {
        var j = m.colPtrs(i)
        while (j < m.colPtrs(i + 1)) {
          res(offset + m.rowIndices(j), i) = m.data(j)
          j += 1
        }
        i += 1
      }
      offset += m.rows
    }
    res
  }

  def kroneckerProduct(matrix1: CSCMatrix[Double], matrix2: CSCMatrix[Double]): CSCMatrix[Double] = {
    val r1 = matrix1.rows
    val c1 = matrix1.cols
    val r2 = matrix2.rows
    val c2 = matrix2.cols

    val res = CSCMatrix.zeros[Double](r1 * r2, c1 * c2)

    var i = 0
    while (i < c1) {
      var j = matrix1.colPtrs(i)
      while (j < matrix1.colPtrs(i + 1)) {
        var k = 0
        while (k < c2) {
          var l = matrix2.colPtrs(k)
          while (l < matrix2.colPtrs(k + 1)) {
            res(r2 * matrix1.rowIndices(j) + matrix2.rowIndices(l), c2 * i + k) = matrix1.data(j) * matrix2.data(l)
            l += 1
          }
          k += 1
        }
        j += 1
      }
      i += 1
    }
    res
  }
}
