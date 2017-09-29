package tests;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;

import application.MarchingCubesApplication;
import application.MarchingCubesController;
import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.LabelMultisetType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import javafx.application.Application;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ncsa.hdf.hdf5lib.exceptions.HDF5FileNotFoundException;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import util.HDF5Reader;

public class ExampleApplication extends Application
{
	/** logger */
	static Logger LOGGER;

	/** volume with the labeled segmentation */
	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	private static MarchingCubesApplication sceneryApplication;

	/** resolution of the volume */
	private static double[] resolution = new double[] { 4, 4, 40 };

	public static void main( final String[] args )
	{
		// Set the log level
		System.setProperty( org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off" );
//		System.setProperty( org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "messages.txt" );
		LOGGER = LoggerFactory.getLogger( MarchingCubesController.class );

		final util.Parameters params = getParameters( args );

		boolean success = loadData( params );
		if ( !success )
		{
			LOGGER.error( "Failed to load the data" );
			return;
		}

		launch( args );
	}

	@Override
	public void start( final Stage primaryStage ) throws Exception
	{
		primaryStage.setTitle( "JavaFX - Scenery Application" );
		final StackPane stackPane = new StackPane();

		sceneryApplication = new MarchingCubesApplication( "Marching cubes", 500, 500, false );
		stackPane.getChildren().addAll( sceneryApplication.getPanel() );

		final javafx.scene.Scene scene = new javafx.scene.Scene( stackPane );
		primaryStage.setScene( scene );
		primaryStage.show();

		new Thread( () -> {
			sceneryApplication.main();
		} ).start();

		final MarchingCubesController controller = new MarchingCubesController();
		controller.setMode( MarchingCubesController.ViewerMode.ONLY_ONE_NEURON_VISIBLE );
		System.out.println( "scenery application: " + sceneryApplication );
		controller.setViewer3D( sceneryApplication );
		controller.setResolution( resolution );

		Localizable location = new Point( new int[] { 10, 267, 0 } );
		controller.generateMesh( volumeLabels, location );
	}

	private static util.Parameters getParameters( String[] args )
	{
		// get the parameters
		final util.Parameters params = new util.Parameters();
		JCommander.newBuilder()
				.addObject( params )
				.build()
				.parse( args );

		boolean success = validateParameters( params );
		if ( !success )
		{
			LOGGER.error( "Failed: one of the parameters was not informed" );
			return null;
		}

		return params;
	}

	private static boolean validateParameters( util.Parameters params )
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

	/**
	 * This method loads the volume labels from the hdf file
	 */
	private static boolean loadData( util.Parameters params )
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
}
