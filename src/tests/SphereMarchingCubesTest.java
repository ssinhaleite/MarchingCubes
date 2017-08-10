package tests;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.MarchingCubesApplication;
import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.LabelMultisetType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import net.imglib2.RandomAccessibleInterval;
import util.HDF5Reader;

/**
 * This test creates an sphere and then generates the mesh using the Marching
 * Cubes implementation
 * 
 * @author vleite
 *
 */
public class SphereMarchingCubesTest
{
	/** logger */
	static Logger LOGGER;

	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	public static void main( String[] args )
	{
		System.setProperty( org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "info" );
		LOGGER = LoggerFactory.getLogger( SphereMarchingCubesTest.class );

		loadSphere();

		final MarchingCubesApplication sceneryApplication = new MarchingCubesApplication( "Marching cubes", 800, 600 );
		sceneryApplication.setVolumeLabels( volumeLabels );
		sceneryApplication.setForegroundValue( 1 );
		sceneryApplication.setVolumeResolution( new float[] { 0.5f, 0.75f, 0.5f } );
		sceneryApplication.main();
	}

	private static void loadSphere()
	{
		final IHDF5Reader reader = HDF5Factory.openForReading( "resources/sphere500.hdf" );
		final String pathLabel = "/volumes/labels/sphere";
		/** loaded segments */
		ArrayList< H5LabelMultisetSetupImageLoader > labels = null;
		/* labels */
		if ( reader.exists( pathLabel ) )
		{
			try
			{
				labels = HDF5Reader.readLabels( reader, pathLabel );
			}
			catch ( IOException e )
			{
				LOGGER.error( "read labels failed: " + e.getCause() );
			}
		}
		else
		{
			LOGGER.error( "no label dataset '" + pathLabel + "' found" );
		}

		volumeLabels = labels.get( 0 ).getImage( 0 );
	}
}
