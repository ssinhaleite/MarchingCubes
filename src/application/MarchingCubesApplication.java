package application;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;

import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.LabelMultisetType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ncsa.hdf.hdf5lib.exceptions.HDF5FileNotFoundException;
import net.imglib2.RandomAccessibleInterval;
import util.HDF5Reader;
import util.Parameters;
import viewer.MarchingCubesSceneryApplication;

/**
 * Main class for the Marching Cubes
 * 
 * @author vleite
 */
public class MarchingCubesApplication
{
	/** logger */
	static Logger LOGGER;

	/** volume with the labeled segmentation */
	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	/**
	 * Main method - starts the scenery application
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception
	{
		// Set the log level
		System.setProperty( org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "info" );
//		System.setProperty( org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "messages.txt" );
		LOGGER = LoggerFactory.getLogger( MarchingCubesApplication.class );

		// get the parameters
		final Parameters params = new Parameters();
		JCommander.newBuilder()
				.addObject( params )
				.build()
				.parse( args );

		boolean success = validateParameters( params );
		if ( !success )
		{
			LOGGER.error( "Failed: one of the parameters was not informed" );
			return;
		}

		success = loadData( params );
		if ( !success )
		{
			LOGGER.error( "Failed to load the data" );
			return;
		}

		final MarchingCubesSceneryApplication sceneryApplication = new MarchingCubesSceneryApplication( "Marching cube", 800, 600 );
		sceneryApplication.setVolumeLabels( volumeLabels );
		sceneryApplication.setForegroundValue( params.foregroundValue );
		sceneryApplication.main();
	}

	/**
	 * This method loads the volume labels from the hdf file
	 */
	private static boolean loadData( Parameters params )
	{
		final IHDF5Reader reader;
		try
		{
			reader = HDF5Factory.openForReading( params.filePath );
		}
		catch ( HDF5FileNotFoundException e )
		{
			LOGGER.error( "input file not found" );
			return false;
		}

		LOGGER.info( "Opening labels from " + params.filePath );
		/** loaded segments */
		ArrayList< H5LabelMultisetSetupImageLoader > labels = null;

		/* label dataset */
		if ( reader.exists( params.labelDatasetPath ) )
		{
			try
			{
				labels = HDF5Reader.readLabels( reader, params.labelDatasetPath );
			}
			catch ( IOException e )
			{
				LOGGER.error( "read labels failed: " + e.getCause() );
				return false;
			}
		}
		else
		{
			LOGGER.error( "no label dataset '" + params.labelDatasetPath + "' found" );
			return false;
		}

		volumeLabels = labels.get( 0 ).getImage( 0 );
		return true;
	}

	private static boolean validateParameters( Parameters params )
	{
		String errorMessage = "";
		if ( params.filePath == "" )
		{
			errorMessage += "the input file path was not informed";
		}

		if ( params.foregroundValue == -1 )
		{
			if ( errorMessage != "" )
				errorMessage += " and ";

			errorMessage += "the foreground value was not informed";
		}

		if ( errorMessage != "" )
		{
			LOGGER.error( errorMessage );
			return false;
		}

		return true;
	}
}
