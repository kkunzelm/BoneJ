package org.doube.geometry;

import org.doube.jama.EigenvalueDecomposition;
import org.doube.jama.Matrix;

/**
 * <p>
 * Represents an ellipsoid defined by its centroid, eigenvalues and 3x3
 * eigenvector matrix. Semiaxis lengths (radii) are calculated as the inverse
 * square root of the eigenvalues.
 * </p>
 * 
 * @author Michael Doube
 */
public class Ellipsoid {

	/** Centroid of ellipsoid (cx, cy, cz) */
	private double cx, cy, cz;

	/**
	 * Radii (semiaxis lengths) of ellipsoid. Size-based ordering (e.g. a > b >
	 * c) is not performed. They are in the same order as the eigenvalues and
	 * eigenvectors.
	 */
	private double ra, rb, rc;

	/** Volume of ellipsoid, calculated as 4 * PI * ra * rb * rc / 3 */
	private double volume;

	/**
	 * Eigenvector matrix Size-based ordering is not performed. They are in the
	 * same order as the eigenvalues.
	 */
	private double[][] ev;

	/**
	 * Eigenvalue matrix. Size-based ordering is not performed. They are in the
	 * same order as the eigenvectors.
	 */
	private double[][] ed;

	/** 3x3 matrix describing shape of ellipsoid */
	private double[][] eh;

	/**
	 * Instantiate an ellipsoid from the result of FitEllipsoid
	 * 
	 * @param ellipsoid
	 */
	public Ellipsoid(Object[] ellipsoid) {
		double[] centre = (double[]) ellipsoid[0];
		this.cx = centre[0];
		this.cy = centre[1];
		this.cz = centre[2];

		double[] radii = (double[]) ellipsoid[1];
		this.ra = radii[0];
		this.rb = radii[1];
		this.rc = radii[2];

		if (Double.isNaN(ra) || Double.isNaN(rb) || Double.isNaN(rc))
			throw new IllegalArgumentException("Radius is NaN");

		if (ra <= 0 || rb <= 0 || rc <= 0)
			throw new IllegalArgumentException("Radius cannot be <= 0");

		this.ev = new double[3][3];
		this.ed = new double[3][3];
		this.eh = new double[3][3];
		setRotation((double[][]) ellipsoid[2]);
		setEigenvalues();
		setVolume();
	}

	/**
	 * Construct an Ellipsoid from the radii (a,b,c), centroid (cx, cy, cz) and
	 * Eigenvectors.
	 * 
	 * @param a
	 * @param b
	 * @param c
	 * @param cx
	 * @param cy
	 * @param cz
	 * @param eigenVectors
	 */
	public Ellipsoid(double a, double b, double c, double cx, double cy,
			double cz, double[][] eigenVectors) {

		this.ra = a;
		this.rb = b;
		this.rc = c;
		this.cx = cx;
		this.cy = cy;
		this.cz = cz;
		this.ev = new double[3][3];
		this.ed = new double[3][3];
		this.eh = new double[3][3];
		setRotation(eigenVectors);
		setEigenvalues();
		setVolume();
	}

	/**
	 * Gets the volume of this ellipsoid, calculated as PI * a * b * c * 4 / 3
	 * 
	 * @return
	 */
	public double getVolume() {
		final double d = this.volume;
		return d;
	}

	private void setVolume() {
		volume = Math.PI * ra * rb * rc * 4 / 3;
	}

	/**
	 * @return the semiaxis lengths a, b and c. Note these are not ordered by
	 *         size, but the order does relate to the 0th, 1st and 2nd columns
	 *         of the rotation matrix respectively.
	 */
	public double[] getRadii() {
		double[] radii = { ra, rb, rc };
		return radii.clone();
	}

	/**
	 * Method based on the inequality
	 * 
	 * (X-X0)H(X-X0)^T <= 1
	 * 
	 * Where X is the test point, X0 is the centroid, H is the ellipsoid's 3x3
	 * matrix
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return true if the point (x,y,z) lies inside or on the ellipsoid, false
	 *         otherwise
	 */
	public boolean contains(double x, double y, double z) {
		// calculate vector between point and centroid
		final double vx = x - cx;
		final double vy = y - cy;
		final double vz = z - cz;

		// calculate distance from centroid
		final double length = Math.sqrt(vx * vx + vy * vy + vz * vz);

		// double[] radii = { ra, rb, rc };
		// Arrays.sort(radii);
		double[] radii = getSortedRadii();

		// if further from centroid than major semiaxis length
		// must be outside
		if (length > radii[2])
			return false;

		// if length closer than minor semiaxis length
		// must be inside
		if (length <= radii[0])
			return true;

		final double[][] h = eh;

		final double dot0 = vx * h[0][0] + vy * h[1][0] + vz * h[2][0];
		final double dot1 = vx * h[0][1] + vy * h[1][1] + vz * h[2][1];
		final double dot2 = vx * h[0][2] + vy * h[1][2] + vz * h[2][2];

		final double dot = dot0 * vx + dot1 * vy + dot2 * vz;

		if (dot <= 1)
			return true;

		return false;
	}

	/**
	 * Get the radii sorted in ascending order. Note that there is no guarantee
	 * that this ordering relates at all to the eigenvectors or eigenvalues.
	 * 
	 * @return radii in ascending order
	 */
	public double[] getSortedRadii() {
		
		double a = this.ra;
		double b = this.rb;
		double c = this.rc;
		double temp = 0;
		
		if (a > b){
			temp = a;
			a = b;
			b = temp;
		}
		if (b > c){
			temp = b;
			b = c;
			c = temp;
		}
		if (a > b){
			temp = a;
			a = b;
			b = temp;
		}
			
		double[] sortedRadii = { a, b, c };

		return sortedRadii;
	}
	
	public double[] getCentre() {
		double[] centre = { cx, cy, cz };
		return centre.clone();
	}

	public double[][] getSurfacePoints(final int nPoints) {

		// get regularly-spaced points on the unit sphere
		double[][] vectors = Vectors.regularVectors(nPoints);
		return getSurfacePoints(vectors);

	}

	public double[][] getSurfacePoints(final double[][] vectors) {
		final int nPoints = vectors.length;
		for (int p = 0; p < nPoints; p++) {
			// stretch the unit sphere into an ellipsoid
			final double x = ra * vectors[p][0];
			final double y = rb * vectors[p][1];
			final double z = rc * vectors[p][2];
			// rotate and translate the ellipsoid into position
			vectors[p][0] = x * ev[0][0] + y * ev[0][1] + z * ev[0][2] + cx;
			vectors[p][1] = x * ev[1][0] + y * ev[1][1] + z * ev[1][2] + cy;
			vectors[p][2] = x * ev[2][0] + y * ev[2][1] + z * ev[2][2] + cz;
		}
		return vectors;
	}

	/**
	 * Dilate all three axes by a fractional increment
	 * 
	 * @param increment
	 */
	public void dilate(double increment) {
		dilate(this.ra * increment, this.rb * increment, this.rc * increment);
	}

	/**
	 * Constrict all three axes by a fractional increment
	 * 
	 * @param increment
	 */
	public void contract(double increment) {
		dilate(-increment);
	}

	/**
	 * Contract the semiaxes by independent absolute amounts
	 * 
	 * @param ca
	 * @param cb
	 * @param cd
	 */
	public void contract(double ca, double cb, double cd) {
		dilate(-ca, -cb, -cd);
	}

	/**
	 * Dilate the ellipsoid semiaxes by independent absolute amounts
	 * 
	 * @param da
	 * @param db
	 * @param dc
	 */
	public void dilate(double da, double db, double dc) {
		setRadii(this.ra + da, this.rb + db, this.rc + dc);
	}

	/**
	 * Translate the ellipsoid
	 * 
	 * @param dx
	 *            shift in x
	 * @param dy
	 *            shift in y
	 * @param dz
	 *            shift in z
	 */
	public void translate(double dx, double dy, double dz) {
		this.cx += dx;
		this.cy += dy;
		this.cz += dz;
	}

	/**
	 * Translate the ellipsoid to a given new centroid
	 * 
	 * @param x
	 *            new centroid x-coordinate
	 * @param y
	 *            new centroid y-coordinate
	 * @param z
	 *            new centroid z-coordinate
	 */
	public void setCentroid(double x, double y, double z) {
		this.cx = x;
		this.cy = y;
		this.cz = z;
	}

	/**
	 * Rotate the ellipsoid by the given 3x3 Matrix
	 * 
	 * @param R
	 *            a 3x3 rotation matrix
	 */
	public void rotate(double[][] rotation) {
		setRotation(times(this.ev, rotation));
	}

	/**
	 * Set rotation to the supplied rotation matrix. Does no error checking.
	 */
	public void setRotation(double[][] rotation) {
		this.ev = rotation.clone();
		update3x3Matrix();
	}

	/**
	 * Return a copy of the ellipsoid's eigenvector matrix
	 * 
	 * @return
	 */
	public double[][] getRotation() {
		return ev.clone();
	}

	/**
	 * Set the radii (semiaxes). No ordering is assumed, except with regard to
	 * the columns of the eigenvector rotation matrix (i.e. a relates to the 0th
	 * eigenvector column, b to the 1st and c to the 2nd)
	 * 
	 * @param a
	 * @param b
	 * @param c
	 */
	public void setRadii(double a, double b, double c) {
		if (a <= 0 || b <= 0 || c <= 0) {
			throw new IllegalArgumentException(
					"Ellipsoid cannot have semiaxis <= 0");
		}
		this.ra = a;
		this.rb = b;
		this.rc = c;
		setEigenvalues();
		setVolume();
	}

	/**
	 * Calculates eigenvalues from current radii
	 */
	private void setEigenvalues() {
		this.ed[0][0] = 1 / (this.ra * this.ra);
		this.ed[1][1] = 1 / (this.rb * this.rb);
		this.ed[2][2] = 1 / (this.rc * this.rc);
		update3x3Matrix();
	}

	/**
	 * Needs to be run any time the eigenvalues or eigenvectors change
	 */
	private void update3x3Matrix() {
		this.eh = times(times(ev, ed), transpose(ev));
	}

	/**
	 * High performance 3x3 matrix multiplier with no bounds or error checking
	 * 
	 * @param a
	 * @param b
	 * @return result of matrix multiplication, c = ab
	 */
	private double[][] times(double[][] a, double[][] b) {
		final double a00 = a[0][0];
		final double a01 = a[0][1];
		final double a02 = a[0][2];
		final double a10 = a[1][0];
		final double a11 = a[1][1];
		final double a12 = a[1][2];
		final double a20 = a[2][0];
		final double a21 = a[2][1];
		final double a22 = a[2][2];
		final double b00 = b[0][0];
		final double b01 = b[0][1];
		final double b02 = b[0][2];
		final double b10 = b[1][0];
		final double b11 = b[1][1];
		final double b12 = b[1][2];
		final double b20 = b[2][0];
		final double b21 = b[2][1];
		final double b22 = b[2][2];
		double[][] c = {
				{ a00 * b00 + a01 * b10 + a02 * b20,
						a00 * b01 + a01 * b11 + a02 * b21,
						a00 * b02 + a01 * b12 + a02 * b22 },
				{ a10 * b00 + a11 * b10 + a12 * b20,
						a10 * b01 + a11 * b11 + a12 * b21,
						a10 * b02 + a11 * b12 + a12 * b22 },
				{ a20 * b00 + a21 * b10 + a22 * b20,
						a20 * b01 + a21 * b11 + a22 * b21,
						a20 * b02 + a21 * b12 + a22 * b22 }, };
		return c;
	}

	/**
	 * Transpose a 3x3 matrix in double[][] format. Does no error checking.
	 */
	public static double[][] transpose(double[][] a) {
		double[][] t = new double[3][3];
		t[0][0] = a[0][0];
		t[0][1] = a[1][0];
		t[0][2] = a[2][0];
		t[1][0] = a[0][1];
		t[1][1] = a[1][1];
		t[1][2] = a[2][1];
		t[2][0] = a[0][2];
		t[2][1] = a[1][2];
		t[2][2] = a[2][2];
		return t;
	}

	/**
	 * Calculate the matrix representation of the ellipsoid (centre,
	 * eigenvalues, eigenvectors) from the equation variables
	 * <i>ax</i><sup>2</sup> + <i>by</i><sup>2</sup> + <i>cz</i><sup>2</sup> +
	 * 2<i>dxy</i> + 2<i>exz</i> + 2<i>fyz</i> + 2<i>gx</i> + 2<i>hy</i> +
	 * 2<i>iz</i> = 1 <br />
	 * 
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 * @param e
	 * @param f
	 * @param g
	 * @param h
	 * @param i
	 * @return Object[] array containing centre (double[3]), eigenvalues
	 *         (double[3][3]), eigenvectors (double[3][3]), and the
	 *         EigenvalueDecomposition
	 */
	public static Object[] matrixFromEquation(double a, double b, double c,
			double d, double e, double f, double g, double h, double i) {

		// the fitted equation
		double[][] v = { { a }, { b }, { c }, { d }, { e }, { f }, { g },
				{ h }, { i } };
		Matrix V = new Matrix(v);

		// 4x4 based on equation variables
		double[][] aa = { { a, d, e, g }, { d, b, f, h }, { e, f, c, i },
				{ g, h, i, -1 }, };
		Matrix A = new Matrix(aa);

		// find the centre
		Matrix C = (A.getMatrix(0, 2, 0, 2).times(-1).inverse()).times(V
				.getMatrix(6, 8, 0, 0));

		// using the centre and 4x4 calculate the
		// eigendecomposition
		Matrix T = Matrix.eye(4);
		T.setMatrix(3, 3, 0, 2, C.transpose());
		Matrix R = T.times(A.times(T.transpose()));
		double r33 = R.get(3, 3);
		Matrix R02 = R.getMatrix(0, 2, 0, 2);
		EigenvalueDecomposition E = new EigenvalueDecomposition(R02.times(-1
				/ r33));

		double[] centre = C.getColumnPackedCopy();
		double[][] eigenVectors = E.getV().getArrayCopy();
		double[][] eigenValues = E.getD().getArrayCopy();
		Object[] result = { centre, eigenValues, eigenVectors, E };
		return result;
	}

	/**
	 * Perform a deep copy of this Ellipsoid
	 * 
	 * @return
	 */
	public Ellipsoid copy() {
		Ellipsoid copy = new Ellipsoid(this.ra, this.rb, this.rc, this.cx,
				this.cy, this.cz, this.ev.clone());
		return copy;
	}

	/**
	 * Generate a string of useful information about this Ellipsoid
	 */
	public String debugOutput() {
		String string = "Ellipsoid variables:\n";

		string = string + "\nCentre: \n";
		string = string + "cx: " + this.cx + "\n";
		string = string + "cy: " + this.cy + "\n";
		string = string + "cz: " + this.cz + "\n";

		string = string + "\nRadii: \n";
		string = string + "ra: " + this.ra + "\n";
		string = string + "rb: " + this.rb + "\n";
		string = string + "rc: " + this.rc + "\n";

		string = string + "\nEigenvalues: \n";
		string = string + "eVal0 = " + ed[0][0] + "\n";
		string = string + "eVal1 = " + ed[1][1] + "\n";
		string = string + "eVal2 = " + ed[2][2] + "\n";

		string = string + "\nEigenvectors: \n";
		string = string + "eV00 = " + ev[0][0] + "\n";
		string = string + "eV01 = " + ev[0][1] + "\n";
		string = string + "eV02 = " + ev[0][2] + "\n";
		string = string + "eV10 = " + ev[1][0] + "\n";
		string = string + "eV11 = " + ev[1][1] + "\n";
		string = string + "eV12 = " + ev[1][2] + "\n";
		string = string + "eV20 = " + ev[2][0] + "\n";
		string = string + "eV21 = " + ev[2][1] + "\n";
		string = string + "eV22 = " + ev[2][2] + "\n";
		return string;
	}
}