package fiji.plugin.constrainedshapes;

import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class TwoCircleShape implements Shape, Sampling2DShape {

	/*
	 * INNER CLASSES & ENUMS
	 */
	
	/**
	 * Enum to specify how we compute an energy value from a list of pixel values.
	 * If the usage requires it, we can make this a proper interface with taylored 
	 * implementing classes.
	 */
	public static enum EvalFunction  {
		/** Will return the mean of the pixel intensities, which will minimize it. */
		MEAN,
		/** Will return the opposite of the pixel value mean, effectively maximizing it. */
		MINUS_MEAN;
		public float compute(final float[] pixel_list)  {			
			float result = 0.0f;
			switch (this) {
			case MEAN:
				result = pixel_list[0];
				for (int i = 1; i < pixel_list.length; i++) {
					result += pixel_list[i];
				}
				result /= pixel_list.length;
				break;
			case MINUS_MEAN:
				result = - pixel_list[0];
				for (int i = 1; i < pixel_list.length; i++) {
					result -= pixel_list[i];
				}
				result /= pixel_list.length;
				break;
			}
			return result;
		}
	}
	
	/** Enum that describes the instance two circles arrangement. Useful for drawing.	 */
	public enum Arrangement { ISOLATED, CIRCLE_1_SWALLOWED, CIRCLE_2_SWALLOWED, INTERSECTING }

	/*
	 * FIELDS
	 */
	
	/**
	 * Circle 1 & 2 coordinates and radius. We store them as array to be able to deal with 
	 * multiple shapes.
	 */
	public float xc1, yc1, r1, xc2, yc2, r2;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public TwoCircleShape() {
		this(Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
	}
	
	public TwoCircleShape(float _xc1, float _yc1, float _r1, float _xc2, float _yc2, float _r2) {
		xc1 = _xc1;
		yc1 = _yc1;
		r1 = _r1;
		xc2 = _xc2;
		yc2 = _yc2;
		r2 = _r2;
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public float eval(final ImageProcessor ip, final EvalFunction function) { 
		return eval(ip, function, (int)getPerimeter());
	}
	
	// SHOULD IT BE A TWOCIRCLEROI METHOD? TODO
	public float eval(final ImageProcessor ip, final EvalFunction function, final int n_points) {
		final float[][] pixels_as_float = ip.getFloatArray();
		final double[][] xy = sample(n_points);
		final double[] x = xy[0];
		final double[] y = xy[1];
		final float[] pixel_list = new float[x.length];
		for (int i = 0; i < pixel_list.length; i++) {
			pixel_list[i] = pixels_as_float[(int)x[i]][(int)y[i]]; // MAYBE PUT SOME INTERPOLATION HERE TODO
		}
		return function.compute(pixel_list);
	}
	
	/**
	 * Return the perimeter of this shape.
	 */
	public double getPerimeter() {
		double l = Double.NaN;
		final double a = Math.sqrt((xc2-xc1)*(xc2-xc1) + (yc2-yc1)*(yc2-yc1)); // distance C1 to C2
		final boolean separated_circles = a > r1+r2; // true if the two circles do not intersect, resulting in having 2 separated circles 
		final boolean circle_1_swallowed = r2 > r1 + a; // true if circle 1 is totally within circle 2, resulting in having only 1 circle
		final boolean circle_2_swallowed = r1 > r2 + a;
		if (circle_1_swallowed) { 
			l = 2 * Math.PI * r2;
		} else if (circle_2_swallowed) {
			l = 2 * Math.PI * r1;
		} else if (separated_circles) {
			l = 2 * Math.PI * (r1+r2);
		} else { 
			final double lx1 = ( a*a - r2*r2 + r1*r1 ) / (2*a); // distance C1 to cap
			final double lx2 = ( a*a + r2*r2 - r1*r1 ) / (2*a); // distance C2 to cap
			final double alpha1 = Math.acos(lx1/r1); // cap angle seen from C1
			final double alpha2 = Math.acos(lx2/r2); // cap angle seen from C1
			l = 2 * ( Math.PI - alpha1) * r1 + 2 * ( Math.PI - alpha2) * r2; 
		}
		return l;
	}
	
	public double[][] sample(final int n_points) {
		double[] x = new double[n_points];
		double[] y = new double[n_points];
		
		final double phi = Math.atan2(yc2-yc1, xc2-xc1); // angle of C1C2 with x axis
		final double a = Math.sqrt((xc2-xc1)*(xc2-xc1) + (yc2-yc1)*(yc2-yc1)); // distance C1 to C2
		
		final boolean separated_circles = a > r1+r2; // true if the two circles do not intersect, resulting in having 2 separated circles 
		final boolean circle_1_swallowed = r2 > r1 + a; // true if circle 1 is totally within circle 2, resulting in having only 1 circle
		final boolean circle_2_swallowed = r1 > r2 + a; 

		if (circle_1_swallowed) {
			double theta;
			for (int i=0; i<n_points; i++) {
				theta = i * 2 * Math.PI / n_points;
				x[i] = xc2 + r2 * Math.cos(theta);
				y[i] = yc2 + r2 * Math.sin(theta);
			}

		} else if (circle_2_swallowed) {
			double theta;
			for (int i=0; i<n_points; i++) {
				theta = i * 2 * Math.PI / n_points;
				x[i] = xc1 + r1 * Math.cos(theta);
				y[i] = yc1 + r1 * Math.sin(theta);
			} 

		}else 	if (separated_circles) {
			final int N1 = Math.round(n_points / (1+r2/r1));
			final int N2 = n_points - N1;
			double theta;
			for (int i=0; i<N1; i++) {
				theta = i * 2 * Math.PI / N1;
				x[i] = xc1 + r1 * Math.cos(theta);
				y[i] = yc1 + r1 * Math.sin(theta);
			}
			for (int i = N1; i<n_points; i++) {
				theta = (i-N1) * 2 * Math.PI / N2;
				x[i] = xc2 + r2 * Math.cos(theta);
				y[i] = yc2 + r2 * Math.sin(theta);
			}

		} else {
			final double lx1 = ( a*a - r2*r2 + r1*r1 ) / (2*a); // distance C1 to cap
			final double lx2 = ( a*a + r2*r2 - r1*r1 ) / (2*a); // distance C2 to cap
			final double alpha1 = Math.acos(lx1/r1); // cap angle seen from C1
			final double alpha2 = Math.acos(lx2/r2); // cap angle seen from C1

			final double corr = (Math.PI-alpha1)/(Math.PI-alpha2) * r1/r2;
			final int N1 = (int) Math.round( n_points/(1+1/corr)) - 1;
			final int N2 = n_points - N1;
			double alpha;
			for (int i=0; i<N1; i++) {
				alpha = phi + alpha1 + i * 2 * (Math.PI-alpha1) / N1 ;
				x[i] = xc1 + r1*Math.cos(alpha);
				y[i] = yc1 + r1*Math.sin(alpha);
			}
			for (int i=N1; i<n_points; i++) {
				alpha = Math.PI + phi + alpha2 + (i-N1) * 2 * (Math.PI-alpha2) / N2;
				x[i] = xc2 + r2*Math.cos(alpha);
				y[i] = yc2 + r2*Math.sin(alpha);
			}
		}
		return new double[][] {x, y};
	}

	public Arrangement getArrangement() {
		final double a = Math.sqrt((xc2-xc1)*(xc2-xc1) + (yc2-yc1)*(yc2-yc1)); // distance C1 to C2
		final boolean separated_circles = a > r1+r2; // true if the two circles do not intersect, resulting in having 2 separated circles 
		final boolean circle_1_swallowed = r2 > r1 + a; // true if circle 1 is totally within circle 2, resulting in having only 1 circle
		final boolean circle_2_swallowed = r1 > r2 + a;

		if (circle_1_swallowed) {
			return Arrangement.CIRCLE_1_SWALLOWED;
		} else if (circle_2_swallowed) {
			return Arrangement.CIRCLE_2_SWALLOWED;
		} else if (separated_circles) {
			return Arrangement.ISOLATED;
		} else {
			return Arrangement.INTERSECTING;
		}
	}
	
	public Point2D getC1() {
		return new Point2D.Float(xc1, yc1);
	}

	public Point2D getC2() {
		return new Point2D.Float(xc2, yc2);
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Return a {@link GeneralPath} that describes the outline of this two-circle shape. 
	 * As {@link Ellipse2D} are used internally, the path will be made of Bézier curves.
	 * The path generated by this method is then used in {@link Shape} methods.
	 * 
	 * @see {@link #contains(Point2D)}, 
	 */
	private GeneralPath getPath() {
		final float xb1 = xc1 - r1;
		final float yb1 = yc1 - r1;		
		final float xb2 = xc2 - r2;
		final float yb2 = yc2 - r2;		
		final Ellipse2D circle1 = new Ellipse2D.Float(xb1, yb1, 2*r1, 2*r1);
		final Ellipse2D circle2 = new Ellipse2D.Float(xb2, yb2, 2*r2, 2*r2);
		GeneralPath path = new GeneralPath();
		path.append(circle1, false);
		path.append(circle2, false);
		Area area = new Area(path); // We want the outline
		return new GeneralPath(area);
	}
	

	
	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] args) {
		
		class TestCanvas extends Canvas {
			private TwoCircleShape[] shape;
			public TestCanvas(TwoCircleShape[] _shape) {
				this.shape = _shape;
			}
			private static final long serialVersionUID = 1L;
			public void paint(Graphics g) {
				super.paint(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.setStroke(new CircleStroke(2));
				for (TwoCircleShape s : shape) {					
					g2.draw(s);
					g2.drawString(s.getArrangement().toString(), s.xc1, s.yc1);
				}				
			}
		}
		
		TwoCircleShape tcs1 = new TwoCircleShape(100, 100, 70, 150, 150, 50); // mingled
		TwoCircleShape tcs2 = new TwoCircleShape(50, 200, 30, 150, 250, 60); // separated
		TwoCircleShape tcs3 = new TwoCircleShape(100, 400, 70, 100, 410, 50); // inside
		TwoCircleShape[] shapes = new TwoCircleShape[] { tcs1, tcs2, tcs3 };
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		BorderLayout thisLayout = new BorderLayout();
		frame.getContentPane().setLayout(thisLayout);
		TestCanvas canvas = new TestCanvas(shapes);
		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.pack();
		frame.setSize(250, 500);
		frame.setVisible(true);		
	}
	
	/*
	 * SHAPE METHODS
	 */
	
	public PathIterator getPathIterator(AffineTransform at) {
		return getPath().getPathIterator(at);
//		return new SamplingPathIterator(this, 20);
	}

	public PathIterator getPathIterator(AffineTransform at, double flatness) {
//		return new SamplingPathIterator(this, 20);
		return getPath().getPathIterator(at, flatness);
	}

	
	
	public boolean contains(Point2D p) {
		return getPath().contains(p);
	}

	public boolean contains(Rectangle2D r) {
		return getPath().contains(r);
	}

	public boolean contains(double x, double y) {
		return getPath().contains(x, y);
	}

	public boolean contains(double x, double y, double w, double h) {
		return getPath().contains(x, y, w, h);
	}

	public Rectangle getBounds() {
		return getPath().getBounds();
	}

	public Rectangle2D getBounds2D() {
		return getPath().getBounds2D();
	}

	public boolean intersects(Rectangle2D r) {
		return getPath().intersects(r);
	}

	public boolean intersects(double x, double y, double w, double h) {
		return getPath().intersects(x, y, w, h);
	}
}
