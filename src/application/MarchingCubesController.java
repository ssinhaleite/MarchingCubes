package application;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;

import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.LabelMultisetType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import cleargl.GLVector;
import graphics.scenery.Material;
import graphics.scenery.Mesh;
import marchingCubes.MarchingCubes.ForegroundCriterion;
import ncsa.hdf.hdf5lib.exceptions.HDF5FileNotFoundException;
import net.imglib2.RandomAccessibleInterval;
import util.HDF5Reader;
import util.MeshExtractor;
import util.Parameters;

/**
 * Main class for the Marching Cubes
 * 
 * @author vleite
 */
public class MarchingCubesController
{
	/** logger */
	static Logger LOGGER;

	/** volume with the labeled segmentation */
	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	/** resolution of the volume */
	private static float[] resolution = new float[] { 4f, 4f, 40f };

	private static ForegroundCriterion criterion = ForegroundCriterion.EQUAL;

	private static int[] cubeSize = { 4, 4, 4 };

	private static int foregroundValue;

	private static float[] verticesArray = new float[ 0 ];

	private static MarchingCubesApplication sceneryApplication;

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
		LOGGER = LoggerFactory.getLogger( MarchingCubesController.class );

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

		foregroundValue = params.foregroundValue;

		sceneryApplication = new MarchingCubesApplication( "Marching cube", 800, 600 );
		sceneryApplication.setVolumeResolution( resolution );

		new Thread( () -> {
			sceneryApplication.main();
		} ).start();

		new Thread( () -> {
			marchingCube();
		} ).start();
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

	private static void marchingCube()
	{
		MeshExtractor meshExtractor = new MeshExtractor( volumeLabels, cubeSize, foregroundValue, criterion );

		for ( int voxelSize = 32; voxelSize > 0; voxelSize /= 2 )
		{
			LOGGER.info( "voxel size: " + voxelSize );

			Mesh completeNeuron = new Mesh();
			final Material material = new Material();
			material.setAmbient( new GLVector( 1f, 0.0f, 1f ) );
			material.setSpecular( new GLVector( 1f, 0.0f, 1f ) );

			if ( voxelSize == 32 )
			{
				material.setDiffuse( new GLVector( 1, 0, 0 ) );
			}
			if ( voxelSize == 16 )
			{
				material.setDiffuse( new GLVector( 0, 1, 0 ) );
			}
			if ( voxelSize == 8 )
			{
				material.setDiffuse( new GLVector( 0, 0, 1 ) );
			}
			if ( voxelSize == 4 )
			{
				material.setDiffuse( new GLVector( 1, 0, 1 ) );
			}
			if ( voxelSize == 2 )
			{
				material.setDiffuse( new GLVector( 0, 1, 1 ) );
			}
			if ( voxelSize == 1 )
			{
				material.setDiffuse( new GLVector( 1, 1, 0 ) );
			}

			completeNeuron.setMaterial( material );
			completeNeuron.setName( String.valueOf( foregroundValue + " " + voxelSize ) );
			completeNeuron.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );
			completeNeuron.setScale( new GLVector( resolution[ 0 ], resolution[ 1 ], resolution[ 2 ] ) );
			sceneryApplication.addChild( completeNeuron );

			cubeSize[ 0 ] = voxelSize;
			cubeSize[ 1 ] = voxelSize;
			cubeSize[ 2 ] = 1;

			meshExtractor.setCubeSize( cubeSize );
			int[] position = new int[] { 0, 0, 0 };
			meshExtractor.createChunks( position );

			float[] completeNeuronVertices = new float[ 0 ];
			int completeMeshSize = 0;
			while ( meshExtractor.hasNext() )
			{
				Mesh neuron = new Mesh();
				neuron = meshExtractor.next();

				if ( completeNeuron.getVertices().hasArray() )
				{
					completeNeuronVertices = completeNeuron.getVertices().array();
					completeMeshSize = completeNeuronVertices.length;
				}

				float[] neuronVertices = neuron.getVertices().array();
				int meshSize = neuronVertices.length;
				verticesArray = Arrays.copyOf( completeNeuronVertices, completeMeshSize + meshSize );
				System.arraycopy( neuronVertices, 0, verticesArray, completeMeshSize, meshSize );

				System.out.println( "number of elements complete mesh: " + verticesArray.length );
				completeNeuron.setVertices( FloatBuffer.wrap( verticesArray ) );
				completeNeuron.recalculateNormals();
				completeNeuron.setDirty( true );
			}

			LOGGER.info( "all results generated!" );

			// Pause for 2 seconds
			try
			{
				Thread.sleep( 2000 );
			}
			catch ( InterruptedException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if ( voxelSize != 1 )
				sceneryApplication.removeChild( completeNeuron );
		}
	}
}
