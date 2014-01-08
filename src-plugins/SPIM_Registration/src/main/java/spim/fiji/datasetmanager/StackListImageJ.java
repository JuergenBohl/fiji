package spim.fiji.datasetmanager;

import ij.ImagePlus;
import ij.io.Opener;
import ij.measure.Calibration;

import java.io.File;

import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.io.IOFunctions;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import spim.fiji.spimdata.imgloaders.StackImgLoaderIJ;

public class StackListImageJ extends StackList
{
	public static int defaultAngleChoice = 1;
	public static int defaultTimePointChoice = 1;
	public static int defaultChannelleChoice = 0;
	public static int defaultIlluminationChoice = 0;

	@Override
	public String getTitle() 
	{
		return "Image Stacks (ImageJ Opener)";
	}

	@Override
	public String getExtendedDescription()
	{
		return "This dataset definition supports a series of three-dimensional (3d) image stacks\n" +  
				 "all present in the same folder. The filename of each file must define timepoint,\n" +  
				 "angle, channel and illumination direction (or a subset of those).\n" + 
				 "The 3d image stacks must be stored as TIFF that can be opened by ImageJ natively.\n" + 
				 "\n" + 
				 "\n" + 
				 "The filenames of the 3d image stacks could be for example:\n" +
				 "\n" + 
				 "spim_TL1_Channel1_Illum1_Angle0.tif ... spim_TL100_Channel2_Illum2_Angle315.tif\n" + 
				 "data_TP01_Angle000.lsm ... data_TP70_Angle180.lsm\n" +
				 "Angle0.ome.tiff ... Angle288.ome.tiff\n" +
				 "\n" +
				 "Note: this definition can be used for OpenSPIM data if saved as plain TIFF.";
	}

	@Override
	protected boolean loadCalibration( final File file ) 
	{
		try
		{
			IOFunctions.println( "Loading calibration for: " + file.getAbsolutePath() );
			
			if ( !file.exists() )
			{
				IOFunctions.println( "File '" + file + "' does not exist. Stopping." );
				return false;
			}
			
			final ImagePlus imp = new Opener().openImage( file.getAbsolutePath() );

			if ( imp == null )
			{
				IOFunctions.println( "Could not open file: '" + file.getAbsolutePath() + "'" );
				return false;				
			}
			
			final Calibration c = imp.getCalibration();
			
			calX = c.pixelWidth;
			calY = c.pixelHeight;
			calZ = c.pixelDepth;
			
			calUnit = c.getUnit();
			
			imp.close();
			
			return true;
		}
		catch ( Exception e )
		{
			IOFunctions.println( "Could not open file: '" + file.getAbsolutePath() + "'" );
			return false;
		}
	}

	@Override
	protected ImgLoader createAndInitImgLoader( final String path, final File basePath, final ImgFactory< ? extends NativeType< ? > > imgFactory )
	{
		final StackImgLoaderIJ imgLoader = new StackImgLoaderIJ();
		
		imgLoader.init( path, basePath, fileNamePattern, imgFactory, hasMultipleTimePoints, hasMultipleChannels, hasMultipleIlluminations, hasMultipleAngles );
		
		return imgLoader;
	}

	@Override
	protected boolean supportsMultipleTimepointsPerFile() { return false; }

	@Override
	protected boolean supportsMultipleChannelsPerFile() { return false; }

	@Override
	protected boolean supportsMultipleAnglesPerFile() { return false; }

	@Override
	protected boolean supportsMultipleIlluminationsPerFile() { return false; }

	@Override
	protected int getDefaultMultipleAngles() { return defaultAngleChoice; }

	@Override
	protected int getDefaultMultipleTimepoints() { return defaultTimePointChoice; }

	@Override
	protected int getDefaultMultipleChannels() { return defaultChannelleChoice; }

	@Override
	protected int getDefaultMultipleIlluminations() { return defaultIlluminationChoice; }

	@Override
	protected void setDefaultMultipleAngles( final int a ) { defaultAngleChoice = a; }

	@Override
	protected void setDefaultMultipleTimepoints( final int t ) { defaultTimePointChoice = t; }

	@Override
	protected void setDefaultMultipleChannels( final int c ) { defaultChannelleChoice = c; }

	@Override
	protected void setDefaultMultipleIlluminations( final int i ) { defaultIlluminationChoice = i; }

	@Override
	public StackListImageJ newInstance() { return new StackListImageJ(); }
}
