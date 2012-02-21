package org.bonej.maths;

import Jama.Matrix;

public class MatrixHelper {

	/**
	 * Get the diagonal of the matrix as a column vector
	 * 
	 * @return Column vector containing diagonal
	 */
	public static Matrix diag(Matrix matrix) {
		final int min = Math.min(matrix.getRowDimension(),
				matrix.getColumnDimension());
		double[][] diag = new double[min][1];
		for (int i = 0; i < min; i++) {
			diag[i][0] = matrix.get(i, i);
		}
		return new Matrix(diag);
	}
}
