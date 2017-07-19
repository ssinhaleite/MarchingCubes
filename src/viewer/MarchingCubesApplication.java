package viewer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import graphics.scenery.GeometryType;
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
//	static int isoLevel = 73396;
////	static int isoLevel = 1854;
//	static int[] volDim = {2340, 1685, 153};

	/** small hdf5 for test - subset from sample B */
	private static String path = "data/sample_B_20160708_frags_46_50.hdf";

	private static int isoLevel = 7;

	private static int[] volDim = { 500, 500, 5 };

	private static String path_label = "/volumes/labels/neuron_ids";

	// /** tiny hdf5 for test - dummy values */
//	 static String path_label = "/volumes/labels/small_neuron_ids";
//	 int isoLevel = 2;
//	 int[] volDim = {3, 3, 3};

//	static float[] voxDim = { 0.5f, 0.5f, 0.5f };
	private static float[] voxDim = { 4f, 4f, 4f };

	private static PrintWriter writer = null;

	private static float maxAxisVal = 0;

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
	 * @param args
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception
	{
		// Set the log level
		System.setProperty( org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO" );
		final MarchingCubeApplication viewer = new MarchingCubeApplication( "Marching cube", 800, 600 );
		viewer.main();
	}

	private static class MarchingCubeApplication extends SceneryDefaultApplication
	{
		public MarchingCubeApplication( String applicationName, int windowWidth, int windowHeight )
		{
			super( applicationName, windowWidth, windowHeight, false );
		}

		@Override
		public void init()
		{
			loadData();

			try
			{
				writer = new PrintWriter( "vertices_16.txt", "UTF-8" );
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
			cam.setPosition( new GLVector( 0.5f, 0.5f, 5 ) );
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

//			final Material material = new Material();
//			material.setAmbient( new GLVector( 0.1f * ( 1 ), 1.0f, 1.0f ) );
//			material.setDiffuse( new GLVector( 0.1f * ( 1 ), 0.0f, 1.0f ) );
//			material.setSpecular( new GLVector( 0.1f * ( 1 ), 0f, 0f ) );
//
//			Mesh neuron = new Mesh();
//			neuron.setMaterial( material );
//			neuron.setName( "neuron" );
//			neuron.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );
////			neuron.setScale( new GLVector( 1.0f, 1.0f, 10.0f ) );
//			neuron.setGeometryType( GeometryType.POINTS );
//			getScene().addChild( neuron );

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

	private static void marchingCube( Scene scene)
	{
		int numberOfCellsX = ( int ) ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) + 1 ) / 32;
		int numberOfCellsY = ( int ) ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) + 1 ) / 32;
		int numberOfCellsZ = ( int ) ( ( volumeLabels.max( 2 ) - volumeLabels.min( 2 ) ) + 1 ) / 32;

		LOGGER.trace( "division: " + numberOfCellsX + " " + numberOfCellsY + " " + numberOfCellsZ );

		numberOfCellsX = numberOfCellsX >= 7 ? 7 * 32 : numberOfCellsX * 32;
		numberOfCellsY = numberOfCellsY >= 7 ? 7 * 32 : numberOfCellsY * 32;
		numberOfCellsZ = numberOfCellsZ >= 7 ? 7 * 32 : numberOfCellsZ * 32;

		LOGGER.trace( "partition size 1: " + numberOfCellsX + " " + numberOfCellsY + " " + numberOfCellsZ );

		numberOfCellsX = ( numberOfCellsX == 0 ) ? 1 : numberOfCellsX;
		numberOfCellsY = ( numberOfCellsY == 0 ) ? 1 : numberOfCellsY;
		numberOfCellsZ = ( numberOfCellsZ == 0 ) ? 1 : numberOfCellsZ;

		LOGGER.trace( "zero verification: " + numberOfCellsX + " " + numberOfCellsY + " " + numberOfCellsZ );

		int[] partitionSize = new int[] { numberOfCellsX, numberOfCellsY, numberOfCellsZ };
		LOGGER.trace( "final partition size: " + numberOfCellsX + " " + numberOfCellsY + " " + numberOfCellsZ );

		final List< int[] > offsets = new ArrayList<>();
		final List< RandomAccessibleInterval< LabelMultisetType > > subvolumes = new ArrayList< RandomAccessibleInterval< LabelMultisetType > >();
		
		CompletionService< viewer.Mesh > executor = null;
		List< Future< viewer.Mesh > > resultMeshList = null;
		for ( int voxSize = 32; voxSize > 0; voxSize /= 2 )
		{
			// clean the vertices, offsets and subvolumes
			verticesArray = new float[ 0 ];
			offsets.clear();
			subvolumes.clear();

			Mesh neuron = new Mesh();
			final Material material = new Material();
			material.setAmbient( new GLVector( 1f , 0.0f , 1f ) );
			material.setSpecular( new GLVector( 1f , 0.0f , 1f ) );

			if (voxSize == 32)
				material.setDiffuse( new GLVector( 1, 0 , 0 ) );
			if (voxSize == 16)
				material.setDiffuse( new GLVector( 0, 1 , 0 ) );
			if (voxSize == 8)
				material.setDiffuse( new GLVector( 0, 0 , 1 ) );
			if (voxSize == 4)
				material.setDiffuse( new GLVector( 1, 0 , 1 ) );
			if (voxSize == 2)
				material.setDiffuse( new GLVector( 0, 1 , 1 ) );
			if (voxSize == 1)
				material.setDiffuse( new GLVector( 1, 1 , 0 ) );

			neuron.setMaterial( material );
			neuron.setName( String.valueOf(isoLevel + " " + voxSize) );
			neuron.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );
//			neuron.setScale( new GLVector( 1.0f, 1.0f, 10.0f ) );
			neuron.setGeometryType( GeometryType.POINTS );
			scene.addChild( neuron);
			voxDim[0] = voxSize;
			voxDim[1] = voxSize;
			voxDim[2] = 1;
	
			util.VolumePartitioner partitioner = new util.VolumePartitioner( volumeLabels, partitionSize, voxDim );
			partitioner.dataPartitioning( subvolumes, offsets );
//		}
//		subvolumes.clear();
//		subvolumes.add( volumeLabels );
//		offsets.set( 0, new int[] { 0, 0, 0 } );

			LOGGER.info( "starting executor..." );
			executor = new ExecutorCompletionService< viewer.Mesh >(
					Executors.newWorkStealingPool() );

			resultMeshList = new ArrayList<>();

			final float maxX = volDim[ 0 ] - 1;
			final float maxY = volDim[ 1 ] - 1;
			final float maxZ = volDim[ 2 ] - 1;

			maxAxisVal = Math.max( maxX, Math.max( maxY, maxZ ) );
			LOGGER.trace( "maxX " + maxX + " maxY: " + maxY + " maxZ: " + maxZ + " maxAxisVal: " + maxAxisVal );

			LOGGER.info( "creating callables for " + subvolumes.size() + " partitions..." );

			for ( int i = 0; i < subvolumes.size(); i++ )
			{
				LOGGER.info( "dimension: " + subvolumes.get( i ).dimension( 0 ) + "x" + subvolumes.get( i ).dimension( 1 )
						+ "x" + subvolumes.get( i ).dimension( 2 ) );
				int[] subvolDim = new int[] { ( int ) subvolumes.get( i ).dimension( 0 ), ( int ) subvolumes.get( i ).dimension( 1 ),
						( int ) subvolumes.get( i ).dimension( 2 ) };
	
				LOGGER.info( "offset: " + offsets.get( i )[ 0 ] + " " + offsets.get( i )[ 1 ] + " " + offsets.get( i )[ 2 ] );
				MarchingCubesCallable callable = new MarchingCubesCallable( subvolumes.get( i ), subvolDim, offsets.get( i ), voxDim, true, isoLevel,
						false );
	
				LOGGER.trace( "callable: " + callable );
				Future< viewer.Mesh > result = executor.submit( callable );
				resultMeshList.add( result );
			}

			Future< viewer.Mesh > completedFuture = null;
			LOGGER.info( "waiting results..." );

			while ( resultMeshList.size() > 0 )
			{
				// block until a task completes
				try
				{
					completedFuture = executor.take();
					LOGGER.trace( "task " + completedFuture + " is ready: " + completedFuture.isDone() );
				}
				catch ( InterruptedException e )
				{
					// TODO Auto-generated catch block
					LOGGER.error( " task interrupted: " + e.getCause() );
				}
	
				resultMeshList.remove( completedFuture );
				viewer.Mesh m = new viewer.Mesh();
	
				// get the mesh, if the task was able to create it
				try
				{
					m = completedFuture.get();
					LOGGER.info( "getting mesh" );
				}
				catch ( InterruptedException | ExecutionException e )
				{
					LOGGER.error( "Mesh creation failed: " + e.getCause() );
					break;
				}
	
				// a mesh was created, so update the existing mesh
				if ( m.getNumberOfTriangles() > 0 )
				{
					LOGGER.info( "updating mesh..." );
					updateMesh( m, neuron );
					neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
					neuron.recalculateNormals();
					neuron.setDirty( true );
				}
			}

			if( LOGGER.isDebugEnabled())
			{
				LOGGER.debug( "size of mesh " + verticesArray.length );
			}
			
			LOGGER.info( "all results generated!" );
			writer.close();
		}
	}

	/**
	 * this method assumes that the data is processed completely at once, this
	 * way, every time this method is called it overwrites the vertices and
	 * normals arrays.
	 * 
	 * @param m
	 *            mesh information to be converted in a mesh for scenery
	 * @param neuron
	 *            scenery mesh that will receive the information
	 */
	public void updateMeshComplete( viewer.Mesh m, Mesh neuron )
	{
		int numberOfTriangles = m.getNumberOfTriangles();

		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug( "previous size of vertices: " + verticesArray.length );
			LOGGER.debug( "number of triangles: " + numberOfTriangles );
			LOGGER.debug( "size of verticesArray: " + numberOfTriangles * 3 * 3 );
		}

		verticesArray = new float[ numberOfTriangles * 3 * 3 ];

		final float[][] vertices = m.getVertices();
		final int[] triangles = m.getTriangles();

		float[] point0 = new float[ 3 ];
		float[] point1 = new float[ 3 ];
		float[] point2 = new float[ 3 ];
		int v = 0;

		for ( int i = 0; i < numberOfTriangles; i++ )
		{
			final long id0 = triangles[ i * 3 ];
			final long id1 = triangles[ i * 3 + 1 ];
			final long id2 = triangles[ i * 3 + 2 ];

			point0 = vertices[ ( int ) id0 ];
			point1 = vertices[ ( int ) id1 ];
			point2 = vertices[ ( int ) id2 ];

			verticesArray[ v++ ] = point0[ 0 ];
			verticesArray[ v++ ] = point0[ 1 ];
			verticesArray[ v++ ] = point0[ 2 ];

			verticesArray[ v++ ] = point1[ 0 ];
			verticesArray[ v++ ] = point1[ 1 ];
			verticesArray[ v++ ] = point1[ 2 ];

			verticesArray[ v++ ] = point2[ 0 ];
			verticesArray[ v++ ] = point2[ 1 ];
			verticesArray[ v++ ] = point2[ 2 ];
		}

		if ( LOGGER.isDebugEnabled() )
		{
			LOGGER.debug( "vsize: " + verticesArray.length );
		}
		for ( int i = 0; i < verticesArray.length; ++i )
		{
			verticesArray[ i ] /= maxAxisVal;
			writer.println( verticesArray[ i ] );
		}

		neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
		neuron.recalculateNormals();
		neuron.setDirty( true );
	}

	/**
	 * this method assumes that the data is processed block-wise, this way,
	 * every time this method is called it add more vertices to the already
	 * existing array.
	 * 
	 * @param m
	 *            mesh information to be converted in a mesh for scenery
	 * @param neuron
	 *            scenery mesh that will receive the information
	 */
	public static void updateMesh( viewer.Mesh m, Mesh neuron )
	{
		/** max value int = 2,147,483,647 */
		final int vertexCount = verticesArray.length;
		final int numberOfTriangles = m.getNumberOfTriangles();
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug( "previous size of vertices: " + verticesArray.length );
			LOGGER.debug( "number of triangles: " + numberOfTriangles );
			LOGGER.debug( "size of verticesArray: " + ( numberOfTriangles * 3 * 3 + vertexCount ) );
		}

		// resize array to fit the new mesh
		verticesArray = Arrays.copyOf( verticesArray, ( numberOfTriangles * 3 * 3 + vertexCount ) );

		float[][] vertices = m.getVertices();
		int[] triangles = m.getTriangles();

		float[] point0 = new float[ 3 ];
		float[] point1 = new float[ 3 ];
		float[] point2 = new float[ 3 ];
		int v = 0;

		for ( int i = 0; i < numberOfTriangles; i++ )
		{
			final long id0 = triangles[ i * 3 ];
			final long id1 = triangles[ i * 3 + 1 ];
			final long id2 = triangles[ i * 3 + 2 ];

			point0 = vertices[ ( int ) id0 ];
			point1 = vertices[ ( int ) id1 ];
			point2 = vertices[ ( int ) id2 ];

			verticesArray[ vertexCount + v++ ] = point0[ 0 ];
			verticesArray[ vertexCount + v++ ] = point0[ 1 ];
			verticesArray[ vertexCount + v++ ] = point0[ 2 ];

			verticesArray[ vertexCount + v++ ] = point1[ 0 ];
			verticesArray[ vertexCount + v++ ] = point1[ 1 ];
			verticesArray[ vertexCount + v++ ] = point1[ 2 ];

			verticesArray[ vertexCount + v++ ] = point2[ 0 ];
			verticesArray[ vertexCount + v++ ] = point2[ 1 ];
			verticesArray[ vertexCount + v++ ] = point2[ 2 ];
		}

		// omp parallel for
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug( "vsize: " + verticesArray.length );
		}
		
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
//						voxDim = new float[] { 1.0f, 1.0f, 1.0f };
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
//						voxDim = new float[] { 1.0f, 1.0f, 1.0f };
//						logger.debug( "updating mesh dist2" );
//						marchingCube( neuron, neuron.getMaterial(), scene, cam );
//						dist2 = false;
//						dist3 = true;
//						dist1 = true;
//					}
//					else if ( diff < 2 && diff >= 1 && dist1 )
//					{
//						voxDim = new float[] { 0.5f, 0.5f, 0.5f };
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
