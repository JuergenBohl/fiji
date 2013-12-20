package spim.fiji.spimdata;

import spim.fiji.spimdata.interestpoints.XmlIoViewsInterestPoints;
import mpicbg.spim.data.registration.XmlIoViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.XmlIoImgLoader;
import mpicbg.spim.data.sequence.XmlIoMissingViews;
import mpicbg.spim.data.sequence.XmlIoSequenceDescription;
import mpicbg.spim.data.sequence.XmlIoTimePoints;
import mpicbg.spim.data.sequence.XmlIoViewSetups;

public class XmlIo
{
	/*
	public static XmlIoSpimData< TimePoint, ViewSetupBeads > createDefaultIo()
	{
		final XmlIoSequenceDescription< TimePoint, ViewSetupBeads > seqDesc = 
				new XmlIoSequenceDescription< TimePoint, ViewSetupBeads >( new XmlIoTimePoints(), new XmlIoViewSetupsBeads(), new XmlIoMissingViews(), new XmlIoImgLoader() );
		final XmlIoSpimData< TimePoint, ViewSetupBeads > io = 
				new XmlIoSpimData< TimePoint, ViewSetupBeads >( seqDesc, new XmlIoViewRegistrations() );
			
		return io;
	}
	*/
	
	public static XmlIoSpimData2 createDefaultIo()
	{
		final XmlIoSequenceDescription< TimePoint, ViewSetup > seqDesc = 
				new XmlIoSequenceDescription< TimePoint, ViewSetup >( new XmlIoTimePoints(), new XmlIoViewSetups(), new XmlIoMissingViews(), new XmlIoImgLoader() );
		final XmlIoSpimData2 io = 
				new XmlIoSpimData2( seqDesc, new XmlIoViewRegistrations(), new XmlIoViewsInterestPoints() );
			
		return io;
	}

}
