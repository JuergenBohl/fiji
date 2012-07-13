package mpicbg.stitching;

import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;

public class StitchingParameters 
{
	/**
	 * If we cannot wrap, which factory do we use for computing the phase correlation
	 */
	public static ImgFactory< ? > phaseCorrelationFactory = new ArrayImgFactory< FloatType >();
	
	/**
	 * If you want to force that the {@link ContainerFactory} above is always used set this to true
	 */
	public static boolean alwaysCopy = false;
	
	public int dimensionality;
	public int fusionMethod;
	public String fusedName;
	public int checkPeaks;
	public boolean computeOverlap, subpixelAccuracy;
	public double xOffset;
	public double yOffset;
	public double zOffset;

	public boolean virtual = false;
	public int channel1;
	public int channel2;

	public int timeSelect;
	
	public int cpuMemChoice = 0;
	// 0 == fuse&display, 1 == writeToDisk
	public int outputVariant = 0;
	public String outputDirectory = null;
	
	public double regThreshold = -2;
	public double relativeThreshold = 2.5;
	public double absoluteThreshold = 3.5;
}
