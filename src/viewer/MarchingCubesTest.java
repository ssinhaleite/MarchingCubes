package viewer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.FloatBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.BeforeClass;
import org.junit.Test;

import bdv.bigcat.ui.ARGBConvertedLabelsSource;
import bdv.bigcat.ui.AbstractARGBConvertedLabelsSource;
import bdv.bigcat.ui.ModalGoldenAngleSaturatedARGBStream;
import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.LabelMultisetType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import cleargl.GLVector;
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
import net.imglib2.view.Views;

import util.HDF5Reader;
/**
 * Unit test for marching cubes
 * 
 * @author vleite
 */
public class MarchingCubesTest
{
	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	protected static int setupId = 0;

	final static protected int[] cellDimensions = new int[] { 64, 64, 8 };

	/** color generator for composition of loaded segments and canvas */
	protected static ModalGoldenAngleSaturatedARGBStream colorStream;

	/** loaded segments */
	final protected static ArrayList< H5LabelMultisetSetupImageLoader > labels = new ArrayList<>();

	/** compositions of labels and canvas that are displayed */
	final protected static ArrayList< AbstractARGBConvertedLabelsSource > convertedLabels = new ArrayList<>();

	Timestamp begin = new Timestamp( System.currentTimeMillis() );

	Timestamp end = new Timestamp( System.currentTimeMillis() );

	float[] verticesArray = new float[ 0 ];

	float[] normalsArray = new float[ 0 ];

	/** big hdf5 for test - whole sample B */
//	 static String path = "data/sample_B.augmented.0.hdf";
//	 int isoLevel = 73396;
//	 int isoLevel = 1854;
//	 int[] volDim = {2340, 1685, 153};

	/** small hdf5 for test - subset from sample B */
	static String path = "data/sample_B_20160708_frags_46_50.hdf";
	int isoLevel = 7;
	int[] volDim = { 500, 500, 5 };

	static String path_label = "/volumes/labels/neuron_ids";

//	/** tiny hdf5 for test - dummy values */
//	static String path_label = "/volumes/labels/small_neuron_ids";
//	int isoLevel = 2;
//	int[] volDim = {3, 3, 3};

//	float[] voxDim = { 10f, 10f, 10f };
	float[] voxDim = { 1f, 1f, 1f };

	float smallx = 0.0f;

	float smally = 0.0f;

	float smallz = 0.0f;

	float bigx = 0.0f;

	float bigy = 0.0f;

	float bigz = 0.0f;

	PrintWriter writer = null;

	PrintWriter writer2 = null;

	float maxAxisVal = 0;

	// float previousDiff = 0.0f;

	/**
	 * This method load the hdf file
	 */
	@BeforeClass
	public static void loadData()
	{
		System.out.println( "Opening labels from " + path );
		final IHDF5Reader reader = HDF5Factory.openForReading( path );

		/* labels */
		if ( reader.exists( path_label ) )
		{
			try
			{
				HDF5Reader.readLabels(reader, path_label);
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

	@Test
	public void testExample() throws Exception
	{
		MarchingCubeApplication viewer = new MarchingCubeApplication( "Marching cube", 800, 600 );
		viewer.main();
	}

	private class MarchingCubeApplication extends SceneryDefaultApplication
	{
		public MarchingCubeApplication( String applicationName, int windowWidth, int windowHeight )
		{
			super( applicationName, windowWidth, windowHeight, false );
		}

		@Override
		public void init()
		{
			try
			{
				writer = new PrintWriter( "vertices_.txt", "UTF-8" );
//				writer2 = new PrintWriter( "normals_.txt", "UTF-8" );
			}
			catch ( IOException e )
			{
				// do something
			}

			setRenderer( Renderer.Factory.createRenderer( getHub(), getApplicationName(), getScene(), getWindowWidth(),
					getWindowHeight() ) );
			getHub().add( SceneryElement.Renderer, getRenderer() );

			final Material material = new Material();
			material.setAmbient( new GLVector( 0.1f * ( 1 ), 1.0f, 1.0f ) );
			material.setDiffuse( new GLVector( 0.1f * ( 1 ), 0.0f, 1.0f ) );
			material.setSpecular( new GLVector( 0.1f * ( 1 ), 0f, 0f ) );
			// material.setDoubleSided(true);

			final Camera cam = new DetachedHeadCamera();

			cam.perspectiveCamera( 50f, getWindowHeight(), getWindowWidth(), 0.1f, 1000.0f );
			cam.setActive( true );
			getScene().addChild( cam );

			PointLight[] lights = new PointLight[ 4 ];

			for ( int i = 0; i < lights.length; i++ )
			{
				lights[ i ] = new PointLight();
				lights[ i ].setEmissionColor( new GLVector( 1.0f, 1.0f, 1.0f ) );
				lights[ i ].setIntensity( 100.2f * 5 );
				lights[ i ].setLinear( 0.0f );
				// lights[ i ].showLightBox();
			}

			lights[ 0 ].setPosition( new GLVector( 1.0f, 0f, -1.0f / ( float ) Math.sqrt( 2.0 ) ) );
			lights[ 1 ].setPosition( new GLVector( -1.0f, 0f, -1.0f / ( float ) Math.sqrt( 2.0 ) ) );
			lights[ 2 ].setPosition( new GLVector( 0.0f, 1.0f, 1.0f / ( float ) Math.sqrt( 2.0 ) ) );
			lights[ 3 ].setPosition( new GLVector( 0.0f, -1.0f, 1.0f / ( float ) Math.sqrt( 2.0 ) ) );

			for ( int i = 0; i < lights.length; i++ )
				getScene().addChild( lights[ i ] );

			Mesh neuron = new Mesh();
			neuron.setMaterial( material );
			neuron.setGeometryType( GeometryType.POINTS );
			neuron.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );

			marchingCube( neuron, material, getScene(), cam );

			// levelOfDetails( neuron, getScene(), cam );
		}
	}

	protected static void readLabels( final IHDF5Reader reader, final String labelDataset ) throws IOException
	{
		/* labels */
		final H5LabelMultisetSetupImageLoader labelLoader = new H5LabelMultisetSetupImageLoader( reader, null,
				labelDataset, setupId++, cellDimensions );

		/* converted labels */
		final ARGBConvertedLabelsSource convertedLabelsSource = new ARGBConvertedLabelsSource( setupId++, labelLoader,
				colorStream );

		labels.add( labelLoader );
		convertedLabels.add( convertedLabelsSource );
	}

	private void marchingCube( Mesh neuron, Material material, Scene scene, Camera cam )
	{

		boolean first = true;
		viewer.Mesh m = new viewer.Mesh();
		List< RandomAccessibleInterval< LabelMultisetType > > subvolumes = dataPartitioning();

//		subvolumes.clear();
//		subvolumes.add( volumeLabels );

		System.out.println( "starting executor..." );
		CompletionService< viewer.Mesh > executor = new ExecutorCompletionService< viewer.Mesh >( Executors.newWorkStealingPool() );

		List< Future< viewer.Mesh > > resultMeshList = new ArrayList<>();

		float maxX = voxDim[ 0 ] * ( volDim[ 0 ] - 1 );
		float maxY = voxDim[ 1 ] * ( volDim[ 1 ] - 1 );
		float maxZ = voxDim[ 2 ] * ( volDim[ 2 ] - 1 );
		maxAxisVal = Math.max( maxX, Math.max( maxY, maxZ ) );
		System.out.println( "maxX " + maxX + " maxY: " + maxY + " maxZ: " + maxZ + " maxAxisVal: " + maxAxisVal );

		System.out.println( "creating callables..." );
		for ( int i = 0; i < subvolumes.size(); i++ )
		{
			System.out.println( "dimension: " + subvolumes.get( i ).dimension( 0 ) + "x" + subvolumes.get( i ).dimension( 1 ) + "x" + subvolumes.get( i ).dimension( 2 ) );
			volDim = new int[] { ( int ) subvolumes.get( i ).dimension( 0 ), ( int ) subvolumes.get( i ).dimension( 1 ), ( int ) subvolumes.get( i ).dimension( 2 ) };
			MarchingCubeCallable callable = new MarchingCubeCallable( subvolumes.get( i ), volDim, voxDim, true, isoLevel, true );
			System.out.println( "callable: " + callable );
			System.out.println( "input " + subvolumes.get( i ) );
			Future< viewer.Mesh > result = executor.submit( callable );
			resultMeshList.add( result );
		}

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
//			Mesh n = new Mesh();
//			neuron.setMaterial( material );
//			n.setPosition( new GLVector( resultMeshList.size() - 4, resultMeshList.size() - 4, 0 ) );
			System.out.println( "updating mesh " );
			// neuron.setGeometryType(GeometryType.POINTS);
			updateMesh( m, neuron );

			if ( first )
			{
				scene.addChild( neuron );
				first = false;
			}

			cam.setPosition( new GLVector( ( ( bigx - smallx ) / 2 ), ( ( bigy - smally ) / 2 ), 5.0f ) );
			System.out.println( "camera position: " + ( ( bigx - smallx ) / 2 ) + ":" + ( ( bigy - smally ) / 2 ) + ":" + 5.0f );

			System.out.println( "size of mesh " + verticesArray.length );
		}

		writer.close();
//		writer2.close();
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
		normalsArray = new float[ numberOfTriangles * 3 * 3 ];

		System.out.println( "size of verticesArray: " + numberOfTriangles * 3 * 3 );

		float[][] vertices = m.getVertices();
		int[] triangles = m.getTriangles();

		float[] point0 = new float[ 3 ];
		float[] point1 = new float[ 3 ];
		float[] point2 = new float[ 3 ];
		float sx = 0, sy = 0, bx = 0, by = 0;
		int v = 0, n = 0;

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

		sx /= maxAxisVal;
		sy /= maxAxisVal;
		bx /= maxAxisVal;
		by /= maxAxisVal;

		if ( sx < smallx )
			smallx = sx;
		if ( sy < smally )
			smally = sy;

		if ( bx > bigx )
			bigx = bx;
		if ( by > bigy )
			bigy = by;

		neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
		neuron.recalculateNormals();
		neuron.setDirty( true );
	}

	/**
	 * this method assumes that the data is processed blockwise, this way,
	 * everytime this method is called it add more vertices and normal
	 * information to the already existing arrays.
	 * 
	 * @param m
	 *            mesh information to be converted in a mesh for scenery
	 * @param neuron
	 *            scenery mesh that will receive the information
	 */
	public void updateMesh( viewer.Mesh m, Mesh neuron )
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

		float sx = 0, sy = 0, bx = 0, by = 0;

		for ( int i = 0; i < numberOfTriangles; i++ )
		{
			long id0 = triangles[ i * 3 ];
			long id1 = triangles[ i * 3 + 1 ];
			long id2 = triangles[ i * 3 + 2 ];

			point0 = vertices[ ( int ) id0 ];
			point1 = vertices[ ( int ) id1 ];
			point2 = vertices[ ( int ) id2 ];

//			writer.println(point0[ 0 ]);
//			writer.println(point0[ 1 ]);
//			writer.println(point0[ 2 ]);
//			writer.println(point1[ 0 ]);
//			writer.println(point1[ 1 ]);
//			writer.println(point1[ 2 ]);
//			writer.println(point2[ 0 ]);
//			writer.println(point2[ 1 ]);
//			writer.println(point2[ 2 ]);

			verticesArray[ vertexCount + v++ ] = point0[ 0 ];
			if ( verticesArray[ vertexCount + v - 1 ] < sx )
				sx = verticesArray[ vertexCount + v - 1 ];
			if ( verticesArray[ vertexCount + v - 1 ] > bx )
				bx = verticesArray[ vertexCount + v - 1 ];

			verticesArray[ vertexCount + v++ ] = point0[ 1 ];
			if ( verticesArray[ vertexCount + v - 1 ] < sy )
				sy = verticesArray[ vertexCount + v - 1 ];
			if ( verticesArray[ vertexCount + v - 1 ] > by )
				by = verticesArray[ vertexCount + v - 1 ];

			verticesArray[ vertexCount + v++ ] = point0[ 2 ];

			verticesArray[ vertexCount + v++ ] = point1[ 0 ];
			if ( verticesArray[ vertexCount + v - 1 ] < sx )
				sx = verticesArray[ vertexCount + v - 1 ];
			if ( verticesArray[ vertexCount + v - 1 ] > bx )
				bx = verticesArray[ vertexCount + v - 1 ];

			verticesArray[ vertexCount + v++ ] = point1[ 1 ];
			if ( verticesArray[ vertexCount + v - 1 ] < sy )
				sy = verticesArray[ vertexCount + v - 1 ];
			if ( verticesArray[ vertexCount + v - 1 ] > by )
				by = verticesArray[ vertexCount + v - 1 ];

			verticesArray[ vertexCount + v++ ] = point1[ 2 ];

			verticesArray[ vertexCount + v++ ] = point2[ 0 ];
			if ( verticesArray[ vertexCount + v - 1 ] < sx )
				sx = verticesArray[ vertexCount + v - 1 ];
			if ( verticesArray[ vertexCount + v - 1 ] > bx )
				bx = verticesArray[ vertexCount + v - 1 ];

			verticesArray[ vertexCount + v++ ] = point2[ 1 ];
			if ( verticesArray[ vertexCount + v - 1 ] < sy )
				sy = verticesArray[ vertexCount + v - 1 ];
			if ( verticesArray[ vertexCount + v - 1 ] > by )
				by = verticesArray[ vertexCount + v - 1 ];

			verticesArray[ vertexCount + v++ ] = point2[ 2 ];

		}

		// omp parallel for
		System.out.println( "vsize: " + verticesArray.length );
		for ( int i = vertexCount; i < verticesArray.length; ++i )
		{
			verticesArray[ i ] /= maxAxisVal;
			writer.println( verticesArray[ i ] );
		}

		sx /= maxAxisVal;
		sy /= maxAxisVal;
		bx /= maxAxisVal;
		by /= maxAxisVal;

		if ( sx < smallx )
			smallx = sx;
		if ( sy < smally )
			smally = sy;

		if ( bx > bigx )
			bigx = bx;
		if ( by > bigy )
			bigy = by;

		neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
		neuron.recalculateNormals();
		neuron.setDirty( true );
	}

	private List< RandomAccessibleInterval< LabelMultisetType > > dataPartitioning()
	{
		List< RandomAccessibleInterval< LabelMultisetType > > parts = new ArrayList< RandomAccessibleInterval< LabelMultisetType > >();

		int numberOfParts = 4; // TODO: find a way to determine this value
								// according to the size of the volume and voxel
		// TODO: find a way to divide the volume automatically

		RandomAccessibleInterval< LabelMultisetType > first =
				Views.interval( volumeLabels, new long[] { volumeLabels.min( 0 ), ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 - 1, volumeLabels.min( 2 ) },
						new long[] { volumeLabels.max( 0 ), volumeLabels.max( 1 ), volumeLabels.max( 2 ) } );

		System.out.println( "first - from: " + volumeLabels.min( 0 ) + "x" + ( ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 ) - 1 ) + "x" + volumeLabels.min( 2 ) +
				" to: " + volumeLabels.max( 0 ) + "x" + volumeLabels.max( 1 ) + "x" + volumeLabels.max( 2 ) );

		RandomAccessibleInterval< LabelMultisetType > second =
				Views.interval( volumeLabels, new long[] { volumeLabels.min( 0 ), volumeLabels.min( 1 ), volumeLabels.min( 2 ) },
						new long[] { volumeLabels.max( 0 ), ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 ) + 1, volumeLabels.max( 2 ) } );

		System.out.println( "second - from: " + volumeLabels.min( 0 ) + "x" + volumeLabels.min( 1 ) + "x" + volumeLabels.min( 2 ) +
				" to: " + volumeLabels.max( 0 ) + "x" + ( ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 ) + 1 ) + "x" + volumeLabels.max( 2 ) );

//		RandomAccessibleInterval< LabelMultisetType > first =
//				Views.interval( volumeLabels, new long[] { volumeLabels.min( 0 ), volumeLabels.min( 1 ), volumeLabels.min( 2 ) },
//						new long[] { ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) / 2 ) + 1, ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 ) + 1, volumeLabels.max( 2 ) } );
//
//		System.out.println( "first - from: " + volumeLabels.min( 0 ) + "x" + volumeLabels.min( 1 ) + "x" + volumeLabels.min( 2 ) +
//				" to: " + ( ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) / 2 ) + 1 ) + "x" + ( ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 ) + 1 ) + "x" + volumeLabels.max( 2 ) );
//
//		RandomAccessibleInterval< LabelMultisetType > second =
//				Views.interval( volumeLabels, new long[] { ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) / 2 ) - 1, volumeLabels.min( 1 ), volumeLabels.min( 2 ) },
//						new long[] { volumeLabels.max( 0 ), ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 ) + 1, volumeLabels.max( 2 ) } );
//
//		System.out.println( "second - from: " + ( ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) / 2 ) - 1 ) + "x" + volumeLabels.min( 1 ) + "x" + volumeLabels.min( 2 ) +
//				" to: " + volumeLabels.max( 0 ) + "x" + ( ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 ) + 1 ) + "x" + volumeLabels.max( 2 ) );
//
//		RandomAccessibleInterval< LabelMultisetType > third =
//				Views.interval( volumeLabels, new long[] { volumeLabels.min( 0 ), ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 ) - 1, volumeLabels.min( 2 ) },
//						new long[] { ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) / 2 ) + 1, volumeLabels.max( 1 ), volumeLabels.max( 2 ) } );
//
//		System.out.println( "third - from: " + volumeLabels.min( 0 ) + "x" + ( ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 ) - 1 ) + "x" + volumeLabels.min( 2 ) +
//				" to: " + ( ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) / 2 ) + 1 ) + "x" + volumeLabels.max( 1 ) + "x" + volumeLabels.max( 2 ) );
//
//		RandomAccessibleInterval< LabelMultisetType > forth =
//				Views.interval( volumeLabels, new long[] { ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) / 2 ) - 1, ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 ) - 1, volumeLabels.min( 2 ) },
//						new long[] { volumeLabels.max( 0 ), volumeLabels.max( 1 ), volumeLabels.max( 2 ) } );
//
//		System.out.println( "forth - from: " + ( ( ( volumeLabels.max( 0 ) - volumeLabels.min( 0 ) ) / 2 ) - 1 ) + "x" + ( ( ( volumeLabels.max( 1 ) - volumeLabels.min( 1 ) ) / 2 ) - 1 ) + "x" + volumeLabels.min( 2 ) +
//				" to: " + volumeLabels.max( 0 ) + "x" + volumeLabels.max( 1 ) + "x" + volumeLabels.max( 2 ) );

		parts.add( first );
		parts.add( second );
//		parts.add( third );
//		parts.add( forth );

		return parts;
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

					float diff = cam.getPosition().minus( neuron.getPosition() ).magnitude();
					System.out.println( "distance to camera: " + diff );
					System.out.println( "dists - 4: " + dist3 + " 2: " + dist2 + " 1: " + dist1 );
					if ( diff < 4 && diff >= 3 && dist3 )
					{
						voxDim = new float[] { 5.0f, 5.0f, 5.0f };
						System.out.println( "updating mesh dist4" );
						System.out.println( "position before: " + neuron.getPosition() );
						marchingCube( neuron, neuron.getMaterial(), scene, cam );
						neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
						neuron.setNormals( FloatBuffer.wrap( normalsArray ) );
						neuron.setDirty( true );
						System.out.println( "position after: " + neuron.getPosition() );

						cam.setPosition( new GLVector( ( bigx - smallx ) / 2, ( bigy - smally ) / 2, diff ) );
						dist3 = false;
						dist2 = true;
						dist1 = true;
					}

					else if ( diff < 3 && diff >= 2 && dist2 )
					{
						voxDim = new float[] { 1.0f, 1.0f, 1.0f };
						System.out.println( "updating mesh dist2" );
						marchingCube( neuron, neuron.getMaterial(), scene, cam );
						neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
						neuron.setNormals( FloatBuffer.wrap( normalsArray ) );
						neuron.setDirty( true );

						cam.setPosition( new GLVector( ( bigx - smallx ) / 2, ( bigy - smally ) / 2, diff ) );
						dist2 = false;
						dist3 = true;
						dist1 = true;
					}
					else if ( diff < 2 && diff >= 1 && dist1 )
					{
						voxDim = new float[] { 0.5f, 0.5f, 0.5f };
						System.out.println( "updating mesh dist1" );
						marchingCube( neuron, neuron.getMaterial(), scene, cam );
						neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
						neuron.setNormals( FloatBuffer.wrap( normalsArray ) );
						neuron.setDirty( true );
						cam.setPosition( new GLVector( ( bigx - smallx ) / 2, ( bigy - smally ) / 2, diff ) );
						dist1 = false;
						dist2 = false;
						dist3 = false;
					}

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
