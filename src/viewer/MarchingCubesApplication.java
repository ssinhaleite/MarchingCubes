package viewer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.LabelMultisetType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import cleargl.GLVector;
import graphics.scenery.Box;
import graphics.scenery.Camera;
import graphics.scenery.DetachedHeadCamera;
import graphics.scenery.Material;
import graphics.scenery.Mesh;
import graphics.scenery.PointLight;
import graphics.scenery.Scene;
import graphics.scenery.SceneryDefaultApplication;
import graphics.scenery.SceneryElement;
import graphics.scenery.backends.Renderer;
import net.imglib2.RandomAccessibleInterval;
import util.HDF5Reader;

/**
 * Unit test for marching cubes
 * 
 * @author vleite
 */
public class MarchingCubesApplication
{
	/** logger */
	static final Logger LOGGER = LoggerFactory.getLogger( MarchingCubesApplication.class );

	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	private static float[] verticesArray = new float[ 0 ];

	/** big hdf5 for test - whole sample B */
//	static String path = "data/sample_B.augmented.0.hdf";
//
////	static int foregroundValue = 73396;
//	static int foregroundValue = 1854;
//
//	static int[] volDim = { 2340, 1685, 153 };

	/** small hdf5 for test - subset from sample B */
	private static String path = "resources/sample_B_20160708_frags_46_50.hdf";

	private static int foregroundValue = 7;

	private static int[] volDim = { 500, 500, 5 };

	private static String path_label = "/volumes/labels/neuron_ids";

	// /** tiny hdf5 for test - dummy values */
//	 static String path_label = "/volumes/labels/small_neuron_ids";
//	 int isoLevel = 2;
//	 int[] volDim = {3, 3, 3};

	private static int[] cubeSize = { 4, 4, 4 };

	private static PrintWriter writer = null;

	private static float maxAxisVal = 0;

	private static marchingCubes.MarchingCubes.ForegroundCriterion criterion = marchingCubes.MarchingCubes.ForegroundCriterion.EQUAL;

	/**
	 * This method loads the hdf file
	 */
	public static void loadData()
	{
		LOGGER.info( "Opening labels from " + path );
		final IHDF5Reader reader = HDF5Factory.openForReading( path );

		/** loaded segments */
		ArrayList< H5LabelMultisetSetupImageLoader > labels = null;

		/* labels */
		if ( reader.exists( path_label ) )
		{
			try
			{
				labels = HDF5Reader.readLabels( reader, path_label );
			}
			catch ( IOException e )
			{
				LOGGER.error( "read labels failed: " + e.getCause() );
			}
		}
		else
		{
			LOGGER.error( "no label dataset '" + path_label + "' found" );
		}

		volumeLabels = labels.get( 0 ).getImage( 0 );
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
		System.setProperty( org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO" );
		final MarchingCubesViewer viewer = new MarchingCubesViewer( "Marching cube", 800, 600 );
		viewer.main();
	}

	/**
	 * Application - viewer
	 * 
	 * @author vleite
	 *
	 */
	private static class MarchingCubesViewer extends SceneryDefaultApplication
	{
		public MarchingCubesViewer( String applicationName, int windowWidth, int windowHeight )
		{
			super( applicationName, windowWidth, windowHeight, false );
		}

		@Override
		public void init()
		{
			loadData();

			try
			{
				writer = new PrintWriter( "vertices_.txt", "UTF-8" );
			}
			catch ( IOException e )
			{
				LOGGER.error( "error on printWriter initialization: " + e.getCause() );
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
					marchingCube( getScene() );
				}
			}.start();
//			levelOfDetails( neuron, getScene(), cam );
		}
	}

	private static void marchingCube( Scene scene )
	{
		for ( int voxSize = 32; voxSize > 0; voxSize /= 2 )
		{
			// clean the vertices, offsets and subvolumes
			verticesArray = new float[ 0 ];

			Mesh neuron = new Mesh();
			final Material material = new Material();
			material.setAmbient( new GLVector( 1f, 0.0f, 1f ) );
			material.setSpecular( new GLVector( 1f, 0.0f, 1f ) );

			if ( voxSize == 32 )
				material.setDiffuse( new GLVector( 1, 0, 0 ) );
			if ( voxSize == 16 )
				material.setDiffuse( new GLVector( 0, 1, 0 ) );
			if ( voxSize == 8 )
				material.setDiffuse( new GLVector( 0, 0, 1 ) );
			if ( voxSize == 4 )
				material.setDiffuse( new GLVector( 1, 0, 1 ) );
			if ( voxSize == 2 )
				material.setDiffuse( new GLVector( 0, 1, 1 ) );
			if ( voxSize == 1 )
				material.setDiffuse( new GLVector( 1, 1, 0 ) );

			neuron.setMaterial( material );
			neuron.setName( String.valueOf( foregroundValue + " " + voxSize ) );
			neuron.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );
			neuron.setScale( new GLVector( 4.0f, 4.0f, 40.0f ) );
//			neuron.setGeometryType( GeometryType.POINTS );
			scene.addChild( neuron );
			cubeSize[ 0 ] = voxSize;
			cubeSize[ 1 ] = voxSize;
			cubeSize[ 2 ] = 1;

			MeshExtractor extractor = new MeshExtractor( cubeSize, foregroundValue, criterion );
			extractor.extract();

			final float maxX = volDim[ 0 ] - 1;
			final float maxY = volDim[ 1 ] - 1;
			final float maxZ = volDim[ 2 ] - 1;

			maxAxisVal = Math.max( maxX, Math.max( maxY, maxZ ) );

			if ( LOGGER.isTraceEnabled() )
			{
				LOGGER.trace( "maxX " + maxX + " maxY: " + maxY + " maxZ: " + maxZ + " maxAxisVal: " + maxAxisVal );
			}

			LOGGER.info( "all results generated!" );
			writer.close();

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
			if ( voxSize != 1 )
				scene.removeChild( neuron );
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
	public static void updateMesh( viewer.Mesh m, Mesh neuron, boolean overwriteArray )
	{
		/** max value int = 2,147,483,647 */
		if ( LOGGER.isDebugEnabled() )
		{
			LOGGER.debug( "previous size of vertices: " + verticesArray.length );
		}

		final int vertexCount;
		// resize array to fit the new mesh
		if ( overwriteArray )
		{
			vertexCount = 0;
			verticesArray = new float[ m.getNumberOfVertices() * 3 ];
		}
		else
		{
			vertexCount = verticesArray.length;
			verticesArray = Arrays.copyOf( verticesArray, ( m.getNumberOfVertices() * 3 + vertexCount ) );
		}

		float[][] vertices = m.getVertices();
		int v = 0;
		for ( int i = 0; i < m.getNumberOfVertices(); i++ )
		{
			verticesArray[ vertexCount + v++ ] = vertices[ i ][ 0 ];
			verticesArray[ vertexCount + v++ ] = vertices[ i ][ 1 ];
			verticesArray[ vertexCount + v++ ] = vertices[ i ][ 2 ];
		}

		// omp parallel for
		for ( int i = vertexCount; i < verticesArray.length; ++i )
		{
			verticesArray[ i ] /= maxAxisVal;
			writer.println( verticesArray[ i ] );
		}

		neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
		neuron.recalculateNormals();
		neuron.setDirty( true );
	}

	private void levelOfDetails( Mesh neuron, Scene scene, Camera cam )
	{
		final Thread neuronPositionThread = new Thread()
		{
			@Override
			public void run()
			{
				boolean dist3 = true;
				boolean dist2 = false;
				boolean dist1 = false;
				while ( true )
				{
					neuron.setNeedsUpdate( true );
//
//					float diff = cam.getPosition().minus( neuron.getPosition() ).magnitude();
//					logger.debug(" camera position: " + cam.getPosition().get( 0 ) + ":" + cam.getPosition().get( 1 ) + ":" + cam.getPosition().get( 2 ));
//					logger.debug(" mesh position: " + neuron.getPosition().get( 0 ) + ":" + neuron.getPosition().get( 1 ) + ":" + neuron.getPosition().get( 2 ));
//					logger.debug( "distance to camera: " + diff );
//					logger.debug( "dists - 4: " + dist3 + " 2: " + dist2 + " 1: " + dist1 );
//					if ( diff < 6 && diff >= 3 && dist3 )
//					{
//						cubeSize = new float[] { 1 , 1 , 1 };
//						logger.debug( "updating mesh dist4" );
//						logger.debug( "position before: " + neuron.getPosition() );
//						marchingCube( neuron, neuron.getMaterial(), scene, cam );
//						logger.debug( "position after: " + neuron.getPosition() );
//
//						dist3 = false;
//						dist2 = true;
//						dist1 = true;
//					}

//					else if ( diff < 3 && diff >= 2 && dist2 )
//					{
//						cubeSize = new int[] { 1, 1, 1 };
//						logger.debug( "updating mesh dist2" );
//						marchingCube( neuron, neuron.getMaterial(), scene, cam );
//						dist2 = false;
//						dist3 = true;
//						dist1 = true;
//					}
//					else if ( diff < 2 && diff >= 1 && dist1 )
//					{
//						cubeSize = new float[] { 1, 1, 1 };
//						logger.debug( "updating mesh dist1" );
//						marchingCube( neuron, neuron.getMaterial(), scene, cam );
//						dist1 = false;
//						dist2 = false;
//						dist3 = false;
//					}

					try
					{
						Thread.sleep( 20 );
					}
					catch ( InterruptedException e )
					{
						LOGGER.error( " thread sleep interrupted: " + e.getCause() );
					}
				}
			}
		};
		neuronPositionThread.start();
	}
}
