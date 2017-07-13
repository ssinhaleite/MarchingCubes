package viewer;

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
import graphics.scenery.*;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;
import org.junit.BeforeClass;
import org.junit.Test;
import util.HDF5Reader;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.FloatBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Unit test for marching cubes
 * 
 * @author vleite
 */
public class MarchingCubesApplication
{
	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	Timestamp begin = new Timestamp( System.currentTimeMillis() );

	Timestamp end = new Timestamp( System.currentTimeMillis() );

	static float[] verticesArray = new float[ 0 ];

	/** big hdf5 for test - whole sample B */
//	 static String path = "data/sample_B.augmented.0.hdf";
////	 int isoLevel = 73396;
//	 int isoLevel = 1854;
//	 int[] volDim = {2340, 1685, 153};

	/** small hdf5 for test - subset from sample B */
	static String path = "data/sample_B_20160708_frags_46_50.hdf";

	static int isoLevel = 7;

	static int[] volDim = { 500, 500, 5 };

	static String path_label = "/volumes/labels/neuron_ids";

	// /** tiny hdf5 for test - dummy values */
//	 static String path_label = "/volumes/labels/small_neuron_ids";
//	 int isoLevel = 2;
//	 int[] volDim = {3, 3, 3};

	static float[] voxDim = { 1f, 1f, 1f };

	float smallx = 0.0f;

	float smally = 0.0f;

	float smallz = 0.0f;

	float bigx = 0.0f;

	float bigy = 0.0f;

	float bigz = 0.0f;

	static PrintWriter writer = null;

	PrintWriter writer2 = null;

	static float maxAxisVal = 0;

	/**
	 * This method loads the hdf file
	 */
	public static void loadData()
	{
		System.out.println( "Opening labels from " + path );
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
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println( "no label dataset '" + path_label + "' found" );
		}

		volumeLabels = labels.get( 0 ).getImage( 0 );
	}

	public static void main( String[] args ) throws Exception
	{
		MarchingCubeApplication viewer = new MarchingCubeApplication( "Marching cube", 800, 600 );
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
				writer = new PrintWriter( "vertices_.txt", "UTF-8" );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			setRenderer( Renderer.Factory.createRenderer( getHub(), getApplicationName(), getScene(), getWindowWidth(),
					getWindowHeight() ) );
			getHub().add( SceneryElement.Renderer, getRenderer() );

			final Box hull = new Box( new GLVector( 50.0f, 50.0f, 50.0f ), true );
			hull.getMaterial().setDiffuse( new GLVector( 0.5f, 0.5f, 0.5f ) );
			hull.getMaterial().setDoubleSided( true );
			getScene().addChild( hull );

			final Material material = new Material();
			material.setAmbient( new GLVector( 0.1f * ( 1 ), 1.0f, 1.0f ) );
			material.setDiffuse( new GLVector( 0.1f * ( 1 ), 0.0f, 1.0f ) );
			material.setSpecular( new GLVector( 0.1f * ( 1 ), 0f, 0f ) );

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
				getScene().addChild( lights[ i ] );

			Mesh neuron = new Mesh();
			neuron.setMaterial( material );
			neuron.setName( "neuron" );
			neuron.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );
//			neuron.setScale( new GLVector( 1.0f, 1.0f, 10.0f ) );
			getScene().addChild( neuron );

			new Thread()
			{
				public void run()
				{

					marchingCube( neuron, material, getScene(), cam );
				}
			}.start();
//			levelOfDetails( neuron, getScene(), cam );
		}
	}

	private static void marchingCube( Mesh neuron, Material material, Scene scene, Camera cam )
	{
		viewer.Mesh m = new viewer.Mesh();

		int x = ( int ) ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) + 1 ) / 32;
		int y = ( int ) ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) + 1 ) / 32;
		int z = ( int ) ( ( volumeLabels.max( 2 ) - volumeLabels.min( 2 ) ) + 1 ) / 32;

		System.out.println( "division: " + x + " " + y + " " + z );

		x = ( x >= 7 ) ? 7 * 32 : x * 32;
		y = ( y >= 7 ) ? 7 * 32 : y * 32;
		z = ( z >= 7 ) ? 7 * 32 : z * 32;

		System.out.println( "partition size 1: " + x + " " + y + " " + z );

		x = ( x == 0 ) ? 1 : x;
		y = ( y == 0 ) ? 1 : y;
		z = ( z == 0 ) ? 1 : z;

		System.out.println( "zero verification: " + x + " " + y + " " + z );

		int[] partitionSize = new int[] { x, y, z };
		System.out.println( " final partition size: " + x + " " + y + " " + z );

		List< int[] > offsets = new ArrayList<>();
		util.VolumePartitioner partitioner = new util.VolumePartitioner( volumeLabels, partitionSize );
		List< RandomAccessibleInterval< LabelMultisetType > > subvolumes = partitioner.dataPartitioning( offsets );

//		subvolumes.clear();
//		subvolumes.add( volumeLabels );
//		offsets.set( 0, new int[] { 0, 0, 0 } );

		System.out.println( "starting executor..." );
		CompletionService< viewer.Mesh > executor = new ExecutorCompletionService< viewer.Mesh >(
				Executors.newWorkStealingPool() );

		List< Future< viewer.Mesh > > resultMeshList = new ArrayList<>();

		float maxX = voxDim[ 0 ] * ( volDim[ 0 ] - 1 );
		float maxY = voxDim[ 1 ] * ( volDim[ 1 ] - 1 );
		float maxZ = voxDim[ 2 ] * ( volDim[ 2 ] - 1 );

		maxAxisVal = Math.max( maxX, Math.max( maxY, maxZ ) );
		System.out.println( "maxX " + maxX + " maxY: " + maxY + " maxZ: " + maxZ + " maxAxisVal: " + maxAxisVal );

		System.out.println( "creating callables..." );
//		for ( int voxSize = 32; voxSize > 0; voxSize /= 2 )
//		{
//			voxDim[0] = voxSize;
//			voxDim[1] = voxSize;
//			voxDim[2] = voxSize;

		for ( int i = 0; i < subvolumes.size(); i++ )
		{
			System.out.println( "dimension: " + subvolumes.get( i ).dimension( 0 ) + "x" + subvolumes.get( i ).dimension( 1 )
					+ "x" + subvolumes.get( i ).dimension( 2 ) );
			volDim = new int[] { ( int ) subvolumes.get( i ).dimension( 0 ), ( int ) subvolumes.get( i ).dimension( 1 ),
					( int ) subvolumes.get( i ).dimension( 2 ) };
			MarchingCubesCallable callable = new MarchingCubesCallable( subvolumes.get( i ), volDim, offsets.get( i ), voxDim, true, isoLevel,
					false );
			System.out.println( "callable: " + callable );
			System.out.println( "input " + subvolumes.get( i ) );
			Future< viewer.Mesh > result = executor.submit( callable );
			resultMeshList.add( result );
		}
//		}

		Future< viewer.Mesh > completedFuture = null;
		System.out.println( "waiting results..." );

		while ( resultMeshList.size() > 0 )
		{
			// block until a task completes
			try
			{
				completedFuture = executor.take();
				System.out.println( "task " + completedFuture + " is ready: " + completedFuture.isDone() );
			}
			catch ( InterruptedException e1 )
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			resultMeshList.remove( completedFuture );

			// get the mesh, if the task was able to create it
			try
			{
				m = completedFuture.get();
				System.out.println( "getting mesh" );
			}
			catch ( InterruptedException | ExecutionException e )
			{
				Throwable cause = e.getCause();
				System.out.println( "Mesh creation failed: " + cause );
				e.printStackTrace();
				break;
			}

			// a mesh was created, so update the existing mesh
			System.out.println( "updating mesh " );
			updateMesh( m, neuron );
			neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
			neuron.recalculateNormals();
			neuron.setDirty( true );

		}

		System.out.println( "size of mesh " + verticesArray.length );

		writer.close();
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
		System.out.println( "previous size of vertices: " + verticesArray.length );

		int numberOfTriangles = m.getNumberOfTriangles();
		System.out.println( "number of triangles: " + numberOfTriangles );
		verticesArray = new float[ numberOfTriangles * 3 * 3 ];

		System.out.println( "size of verticesArray: " + numberOfTriangles * 3 * 3 );

		float[][] vertices = m.getVertices();
		int[] triangles = m.getTriangles();

		float[] point0 = new float[ 3 ];
		float[] point1 = new float[ 3 ];
		float[] point2 = new float[ 3 ];
		float sx = 0, sy = 0, bx = 0, by = 0;
		int v = 0;

		for ( int i = 0; i < numberOfTriangles; i++ )
		{
			long id0 = triangles[ i * 3 ];
			long id1 = triangles[ i * 3 + 1 ];
			long id2 = triangles[ i * 3 + 2 ];

			point0 = vertices[ ( int ) id0 ];
			point1 = vertices[ ( int ) id1 ];
			point2 = vertices[ ( int ) id2 ];

			verticesArray[ v++ ] = point0[ 0 ];
			if ( verticesArray[ v - 1 ] < sx )
				sx = verticesArray[ v - 1 ];
			if ( verticesArray[ v - 1 ] > bx )
				bx = verticesArray[ v - 1 ];

			verticesArray[ v++ ] = point0[ 1 ];
			if ( verticesArray[ v - 1 ] < sy )
				sy = verticesArray[ v - 1 ];
			if ( verticesArray[ v - 1 ] > by )
				by = verticesArray[ v - 1 ];

			verticesArray[ v++ ] = point0[ 2 ];

			verticesArray[ v++ ] = point1[ 0 ];
			if ( verticesArray[ v - 1 ] < sx )
				sx = verticesArray[ v - 1 ];
			if ( verticesArray[ v - 1 ] > bx )
				bx = verticesArray[ v - 1 ];

			verticesArray[ v++ ] = point1[ 1 ];
			if ( verticesArray[ v - 1 ] < sy )
				sy = verticesArray[ v - 1 ];
			if ( verticesArray[ v - 1 ] > by )
				by = verticesArray[ v - 1 ];

			verticesArray[ v++ ] = point1[ 2 ];

			verticesArray[ v++ ] = point2[ 0 ];
			if ( verticesArray[ v - 1 ] < sx )
				sx = verticesArray[ v - 1 ];
			if ( verticesArray[ v - 1 ] > bx )
				bx = verticesArray[ v - 1 ];

			verticesArray[ v++ ] = point2[ 1 ];
			if ( verticesArray[ v - 1 ] < sy )
				sy = verticesArray[ v - 1 ];
			if ( verticesArray[ v - 1 ] > by )
				by = verticesArray[ v - 1 ];

			verticesArray[ v++ ] = point2[ 2 ];
		}

		System.out.println( "vsize: " + verticesArray.length );
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
		System.out.println( "previous size of vertices: " + verticesArray.length );
		int vertexCount = verticesArray.length;

		int numberOfTriangles = m.getNumberOfTriangles();
		System.out.println( "number of triangles: " + numberOfTriangles );

		// resize array to fit the new mesh
		verticesArray = Arrays.copyOf( verticesArray, ( numberOfTriangles * 3 * 3 + vertexCount ) );
		System.out.println( "size of verticesArray: " + ( numberOfTriangles * 3 * 3 + vertexCount ) );

		float[][] vertices = m.getVertices();
		int[] triangles = m.getTriangles();

		float[] point0 = new float[ 3 ];
		float[] point1 = new float[ 3 ];
		float[] point2 = new float[ 3 ];
		int v = 0;

		for ( int i = 0; i < numberOfTriangles; i++ )
		{
			long id0 = triangles[ i * 3 ];
			long id1 = triangles[ i * 3 + 1 ];
			long id2 = triangles[ i * 3 + 2 ];

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
		System.out.println( "vsize: " + verticesArray.length );
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
//					System.out.println(" camera position: " + cam.getPosition().get( 0 ) + ":" + cam.getPosition().get( 1 ) + ":" + cam.getPosition().get( 2 ));
//					System.out.println(" mesh position: " + neuron.getPosition().get( 0 ) + ":" + neuron.getPosition().get( 1 ) + ":" + neuron.getPosition().get( 2 ));
//					System.out.println( "distance to camera: " + diff );
//					System.out.println( "dists - 4: " + dist3 + " 2: " + dist2 + " 1: " + dist1 );
//					if ( diff < 6 && diff >= 3 && dist3 )
//					{
//						voxDim = new float[] { 1.0f, 1.0f, 1.0f };
//						System.out.println( "updating mesh dist4" );
//						System.out.println( "position before: " + neuron.getPosition() );
//						marchingCube( neuron, neuron.getMaterial(), scene, cam );
//						System.out.println( "position after: " + neuron.getPosition() );
//
//						dist3 = false;
//						dist2 = true;
//						dist1 = true;
//					}

//					else if ( diff < 3 && diff >= 2 && dist2 )
//					{
//						voxDim = new float[] { 1.0f, 1.0f, 1.0f };
//						System.out.println( "updating mesh dist2" );
//						marchingCube( neuron, neuron.getMaterial(), scene, cam );
//						dist2 = false;
//						dist3 = true;
//						dist1 = true;
//					}
//					else if ( diff < 2 && diff >= 1 && dist1 )
//					{
//						voxDim = new float[] { 0.5f, 0.5f, 0.5f };
//						System.out.println( "updating mesh dist1" );
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
						e.printStackTrace();
					}
				}
			}
		};
		neuronPositionThread.start();
	}
}
