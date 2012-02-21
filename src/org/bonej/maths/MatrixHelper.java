package org.bonej.maths;

import ij.IJ;
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
	
	/**
	 * Print Matrix to ImageJ log window
	 */
	public void printToIJLog(Matrix matrix) {
		printToIJLog(matrix, "");
		return;
	}

	/**
	 * Print the Matrix to the ImageJ log
	 * 
	 * @param title
	 *            Title of the Matrix
	 */
	public void printToIJLog(Matrix matrix, String title) {
		if (!title.isEmpty())
			IJ.log(title);
		int nCols = matrix.getColumnDimension();
		int nRows = matrix.getRowDimension();
		double[][] eVal = matrix.getArrayCopy();
		for (int r = 0; r < nRows; r++) {
			String row = "||";
			for (int c = 0; c < nCols; c++) {
				row = row + IJ.d2s(eVal[r][c], 3) + "|";
			}
			row = row + "|";
			IJ.log(row);
		}
		IJ.log("");
		return;
	}

}
