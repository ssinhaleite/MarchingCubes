

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;

import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.LabelMultisetType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import cleargl.GLVector;
import graphics.scenery.Box;
import graphics.scenery.Camera;
import graphics.scenery.DetachedHeadCamera;
import graphics.scenery.Mesh;
import graphics.scenery.PointLight;
import graphics.scenery.Scene;
import graphics.scenery.SceneryDefaultApplication;
import graphics.scenery.SceneryElement;
import graphics.scenery.backends.Renderer;
import ncsa.hdf.hdf5lib.exceptions.HDF5FileNotFoundException;
import net.imglib2.RandomAccessibleInterval;
import util.HDF5Reader;
import util.Parameters;
import viewer.MeshExtractor;
import viewer.MeshView;

/**
 * Unit test for marching cubes
 * 
 * @author vleite
 */
public class MarchingCubesApplication
{
	/** logger */
	static Logger LOGGER;

	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	private static marchingCubes.MarchingCubes.ForegroundCriterion criterion = marchingCubes.MarchingCubes.ForegroundCriterion.EQUAL;


	/**
	 * This method loads the hdf file
	 */
	public static boolean loadData( Parameters params )
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

	/**
	 * Main method - starts the application
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

		final MarchingCubesViewer viewer = new MarchingCubesViewer( "Marching cube", 800, 600 );
		viewer.setParameters( params );
		viewer.main();
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

	/**
	 * Application - viewer
	 * 
	 * @author vleite
	 *
	 */
	private static class MarchingCubesViewer extends SceneryDefaultApplication
	{
		private Parameters params;

		public MarchingCubesViewer( String applicationName, int windowWidth, int windowHeight )
		{
			super( applicationName, windowWidth, windowHeight, false );
		}

		public void setParameters( Parameters params )
		{
			this.params = params;
		}

		@Override
		public void init()
		{
			boolean success = loadData( params );
			if ( !success )
			{
				LOGGER.error( "Failed to load the data" );
				return;
			}

			setRenderer( Renderer.Factory.createRenderer( getHub(), getApplicationName(), getScene(), getWindowWidth(),
					getWindowHeight() ) );
			getHub().add( SceneryElement.Renderer, getRenderer() );

			final Box hull = new Box( new GLVector( 50.0f, 50.0f, 50.0f ), true );
			hull.getMaterial().setDiffuse( new GLVector( 0.5f, 0.5f, 0.5f ) );
			hull.getMaterial().setDoubleSided( true );
			getScene().addChild( hull );

			final Camera cam = new DetachedHeadCamera();

			cam.perspectiveCamera( 50f, getWindowHeight(), getWindowWidth(), 0.001f, 1000.0f );
			cam.setActive( true );
			cam.setPosition( new GLVector( 2f, 2f, 10 ) );
			getScene().addChild( cam );

			PointLight[] lights = new PointLight[ 4 ];

			for ( int i = 0; i < lights.length; i++ )
			{
				lights[ i ] = new PointLight();
				lights[ i ].setEmissionColor( new GLVector( 1.0f, 1.0f, 1.0f ) );
				lights[ i ].setIntensity( 100.2f * 5 );
				lights[ i ].setLinear( 0.0f );
				lights[ i ].setQuadratic( 0.1f );
			}

			lights[ 0 ].setPosition( new GLVector( 1.0f, 0f, -1.0f / ( float ) Math.sqrt( 2.0 ) ) );
			lights[ 1 ].setPosition( new GLVector( -1.0f, 0f, -1.0f / ( float ) Math.sqrt( 2.0 ) ) );
			lights[ 2 ].setPosition( new GLVector( 0.0f, 1.0f, 1.0f / ( float ) Math.sqrt( 2.0 ) ) );
			lights[ 3 ].setPosition( new GLVector( 0.0f, -1.0f, 1.0f / ( float ) Math.sqrt( 2.0 ) ) );

			for ( int i = 0; i < lights.length; i++ )
			{
				getScene().addChild( lights[ i ] );
			}

			new Thread()
			{
				public void run()
				{
					marchingCube( params.foregroundValue, getScene() );
				}
			}.start();
		}
	}

	private static void marchingCube( int foregroundValue, Scene scene )
	{
		int[] cubeSize = new int[ 3 ];
		MeshView meshView = new MeshView( foregroundValue, scene );

		for ( int voxelSize = 32; voxelSize > 0; voxelSize /= 2 )
		{
			meshView.addMesh();

			cubeSize[ 0 ] = voxelSize;
			cubeSize[ 1 ] = voxelSize;
			cubeSize[ 2 ] = 1;

			MeshExtractor meshExtractor = new MeshExtractor( volumeLabels, cubeSize, foregroundValue, criterion );
			int[] position = new int[] { 0, 0, 0 };
			meshExtractor.createChunks( position );

			while ( meshExtractor.hasNext() )
			{
				Mesh neuron = new Mesh();
				neuron = meshExtractor.next();

				meshView.render( neuron, cubeSize );
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
				meshView.removeMesh();
		}
	}

	/**
	 * this method update the mesh with new data
	 * 
	 * @param m
	 *            mesh information to be converted in a mesh for scenery
	 * @param neuron
	 *            scenery mesh that will receive the information
	 * @param overwriteArray
	 *            if it is true, means that the data is processed all at once,
	 *            so, the verticesArray will be overwritten, if it is false,
	 *            means that the data is processed block-wise, this way, every
	 *            time this method is called it add more vertices to the already
	 *            existing array.
	 */
//	public static void updateMesh( viewer.Mesh m, Mesh neuron, boolean overwriteArray )
//	{
//		/** max value int = 2,147,483,647 */
//		if ( LOGGER.isDebugEnabled() )
//		{
//			LOGGER.debug( "previous size of vertices: " + verticesArray.length );
//		}
//
//		final int vertexCount;
//		// resize array to fit the new mesh
//		if ( overwriteArray )
//		{
//			vertexCount = 0;
//			verticesArray = new float[ m.getNumberOfVertices() * 3 ];
//		}
//		else
//		{
//			vertexCount = verticesArray.length;
//			verticesArray = Arrays.copyOf( verticesArray, ( m.getNumberOfVertices() * 3 + vertexCount ) );
//		}
//
//		float[][] vertices = m.getVertices();
//		int v = 0;
//		for ( int i = 0; i < m.getNumberOfVertices(); i++ )
//		{
//			verticesArray[ vertexCount + v++ ] = vertices[ i ][ 0 ];
//			verticesArray[ vertexCount + v++ ] = vertices[ i ][ 1 ];
//			verticesArray[ vertexCount + v++ ] = vertices[ i ][ 2 ];
//		}
//
//		// omp parallel for
//		for ( int i = vertexCount; i < verticesArray.length; ++i )
//		{
//			verticesArray[ i ] /= maxAxisVal;
//			writer.println( verticesArray[ i ] );
//		}
//
//		neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
//		neuron.recalculateNormals();
//		neuron.setDirty( true );
//	}
}
