package spim.fiji.plugin;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;

import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewDescription;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.io.IOFunctions;

import spim.fiji.plugin.LoadParseQueryXML.XMLParseResult;
import spim.fiji.plugin.interestpointregistration.ChannelProcess;
import spim.fiji.plugin.interestpointregistration.GeometricHashing3d;
import spim.fiji.plugin.interestpointregistration.InterestPointRegistration;
import spim.fiji.plugin.interestpoints.InterestPointDetection;
import spim.fiji.spimdata.SpimData2;
import spim.fiji.spimdata.interestpoints.ViewInterestPointLists;
import spim.fiji.spimdata.interestpoints.ViewInterestPoints;

/**
 *
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 *
 */
public class Interest_Point_Registration implements PlugIn
{
	public static ArrayList< InterestPointRegistration > staticAlgorithms = new ArrayList< InterestPointRegistration >();
	public static String[] registrationChoices = new String[]{ "Register several timepoints individually", "Register a time-series (all to one reference)" };
	
	public static int defaultAlgorithm = 0;
	public static int defaultRegistration = 0;
	public static int[] defaultChannelLabels = null;
	
	final protected String warningLabel = " (WARNING: Only available for "; 
	
	static
	{
		staticAlgorithms.add( new GeometricHashing3d( null, null, null ) );
	}

	@Override
	public void run( final String arg )
	{
		final XMLParseResult result = new LoadParseQueryXML().queryXML( true );

		if ( result == null )
			return;
		
		// the GenericDialog needs a list[] of String for the algorithms that can register
		final String[] descriptions = new String[ staticAlgorithms.size() ];
		
		for ( int i = 0; i < staticAlgorithms.size(); ++i )
			descriptions[ i ] = staticAlgorithms.get( i ).getDescription();
		
		if ( defaultAlgorithm >= descriptions.length )
			defaultAlgorithm = 0;

		// ask which channels have the objects we are searching for
		final ArrayList< Channel > channels = result.getData().getSequenceDescription().getAllChannels();

		// build up the dialog
		final GenericDialog gd = new GenericDialog( "Basic Registration Parameters" );
		
		gd.addChoice( "Registration_algorithm", descriptions, descriptions[ defaultAlgorithm ] );
		
		if ( result.getTimePointsToProcess().size() > 1 )
			gd.addChoice( "Type_of_registration", registrationChoices, registrationChoices[ defaultRegistration ] );

		if ( defaultChannelLabels == null || defaultChannelLabels.length != channels.size() )
			defaultChannelLabels = new int[ channels.size() ];
		
		// check which channels and labels are available and build the choices
		final ArrayList< String[] > channelLabels = new ArrayList< String[] >();
		int i = 0;
		for ( final Channel channel : channels )
		{
			final String[] labels = getAllInterestPointLabelsForChannel( result.getData(), result.getTimePointsToProcess(), channel );
			
			if ( channelLabels == null )
				return;
			
			if ( defaultChannelLabels[ channel.getId() ] >= labels.length )
				defaultChannelLabels[ channel.getId() ] = 0;
			
			gd.addChoice( "Interest_points_channel_" + channel.getName(), labels, labels[ defaultChannelLabels[ i++ ] ] );
			channelLabels.add( labels );
		}
		GUIHelper.addWebsite( gd );
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		final int algorithm = defaultAlgorithm = gd.getNextChoiceIndex();
		final int registrationType;
		
		if ( result.getTimePointsToProcess().size() > 1 )
			defaultRegistration = registrationType = gd.getNextChoiceIndex();
		else
			defaultRegistration = registrationType = 0;

		// assemble which channels have been selected with with label
		final ArrayList< ChannelProcess > channelsToProcess = new ArrayList< ChannelProcess >();
		i = 0;
		
		for ( final Channel channel : channels )
		{
			final int channelChoice = defaultChannelLabels[ channel.getId() ] = gd.getNextChoiceIndex();
			
			if ( channelChoice < channelLabels.get( i ).length - 1 )
			{
				String label = channelLabels.get( i++ )[ channelChoice ];
				
				if ( label.contains( warningLabel ) )
					label = label.substring( 0, label.indexOf( warningLabel ) );
				
				channelsToProcess.add( new ChannelProcess( channel, label ) );
			}
		}
		
		if ( channelsToProcess.size() == 0 )
		{
			IOFunctions.println( "No channels selected. Quitting." );
			return;
		}
		
		for ( final ChannelProcess c : channelsToProcess )
			IOFunctions.println( "registering channel: " + c.getChannel().getId()  + " label: '" + c.getLabel() + "'" );
		
		final InterestPointRegistration ipr = staticAlgorithms.get( algorithm ).newInstance( result.getData(), result.getTimePointsToProcess(), channelsToProcess );

		if ( registrationType == 0 )
			registerIndividualTimePoints( ipr );
		else
			registerTimeSeries( ipr );
	}
	
	public static String[] inputChoice = new String[]{ "Calibration only", "Current view transformations" };	
	public static int defaultTransformInputChoice = 0;
	public static boolean defaultDisplayTransformOnly = false;
	
	protected void registerIndividualTimePoints( final InterestPointRegistration ipr )
	{
		final GenericDialog gd = new GenericDialog( "Register several timepoints individually" );
		
		gd.addChoice( "Register_based_on", inputChoice, inputChoice[ defaultTransformInputChoice ] );
		gd.addCheckbox( "Display final transformation only (do not edit XML)", defaultDisplayTransformOnly );
		
		gd.addMessage( "" );
		gd.addMessage( "Algorithm parameters [" + ipr.getDescription() + "]", new Font( Font.SANS_SERIF, Font.BOLD, 12 ) );
		gd.addMessage( "" );
		
		ipr.addQuery( gd, false );
		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;

		final int inputTransform = defaultTransformInputChoice = gd.getNextChoiceIndex();
		final boolean displayOnly = defaultDisplayTransformOnly = gd.getNextBoolean();
		
		ipr.parseDialog( gd, false );
		
		ipr.prepareInputPoints( inputTransform );
	}

	protected void registerTimeSeries( final InterestPointRegistration ipr )
	{
		IOFunctions.println( "Not implemented yet." );
	}

	/**
	 * Goes through all ViewDescriptions and checks all available labels for interest point detection
	 * 
	 * @param spimData
	 * @param timepointsToProcess
	 * @param channel
	 * @return
	 */
	protected String[] getAllInterestPointLabelsForChannel( final SpimData2 spimData, final ArrayList< TimePoint > timepointsToProcess, final Channel channel )
	{
		final ViewInterestPoints interestPoints = spimData.getViewInterestPoints();
		final HashMap< String, Integer > labels = new HashMap< String, Integer >();
		
		int countViewDescriptions = 0;
		
		for ( final TimePoint t : timepointsToProcess )
			for ( final Angle a : spimData.getSequenceDescription().getAllAngles() )
				for ( final Illumination i : spimData.getSequenceDescription().getAllIlluminations() )
				{
					final ViewId viewId = SpimData2.getViewId( spimData.getSequenceDescription(), t, channel, a, i );
					
					// could the viewid be resolved? this should always work
					if ( viewId == null )
					{
						IOFunctions.println( "An error occured. Count not find the corresponding ViewSetup for angle: " + 
							a.getId() + " channel: " + channel.getId() + " illum: " + i.getId() );
						
						return null;
					}
					
					// get the viewdescription
					final ViewDescription< TimePoint, ViewSetup > viewDescription = spimData.getSequenceDescription().getViewDescription( 
							viewId.getTimePointId(), viewId.getViewSetupId() );

					// check if the view is present
					if ( !viewDescription.isPresent() )
						continue;
					
					// which lists of interest points are available
					final ViewInterestPointLists lists = interestPoints.getViewInterestPointLists( viewId );
					
					for ( final String label : lists.getHashMap().keySet() )
					{
						int count = 1;

						if ( labels.containsKey( label ) )
							count += labels.get( label );
						
						labels.put( label, count );
					}
					
					// are they available in all viewdescriptions?
					++countViewDescriptions;
				}
		
		final String[] allLabels = new String[ labels.keySet().size() + 1 ];
		
		int i = 0;
		
		for ( final String label : labels.keySet() )
		{
			allLabels[ i ] = label;
			
			if ( labels.get( label ) != countViewDescriptions )
				allLabels[ i ] += warningLabel + labels.get( label ) + "/" + countViewDescriptions + " Views!)";
			
			++i;
		}
		
		allLabels[ i ] = "[DO NOT register this channel]";
		
		return allLabels;
	}

	public static void main( String[] args )
	{
		new Interest_Point_Registration().run( null );
	}
}