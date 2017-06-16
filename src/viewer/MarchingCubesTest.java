package viewer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
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
import graphics.scenery.Material;
import graphics.scenery.Mesh;
import graphics.scenery.PointLight;
import graphics.scenery.SceneryDefaultApplication;
import graphics.scenery.SceneryElement;
import graphics.scenery.backends.Renderer;
import marchingCubes.MarchingCubes_ThreeDViewer;
import marchingCubes.MarchingCubes_funkeyRAI;
import marchingCubes.MarchingCubes_ilastikRAI;
import marchingCubes.MarchingCubes_shibbyRAI;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.Facet;
import net.imagej.ops.geom.geom3d.mesh.TriangularFacet;
import net.imagej.ops.geom.geom3d.mesh.Vertex;
import net.imglib2.RandomAccessibleInterval;

/**
 * Unit test for marching cubes
 * 
 * @author vleite
 */
public class MarchingCubesTest
{
	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	protected static int setupId = 0;

	final static protected int[] cellDimensions = new int[] { 4, 4, 40 };

	/** color generator for composition of loaded segments and canvas */
	protected static ModalGoldenAngleSaturatedARGBStream colorStream;

	/** loaded segments */
	final protected static ArrayList< H5LabelMultisetSetupImageLoader > labels = new ArrayList<>();

	/** compositions of labels and canvas that are displayed */
	final protected static ArrayList< AbstractARGBConvertedLabelsSource > convertedLabels = new ArrayList<>();

	Timestamp begin = new Timestamp( System.currentTimeMillis() );

	Timestamp end = new Timestamp( System.currentTimeMillis() );

	float[] verticesArray = null;

	float[] normalsArray = null;

	// hdf file to use on test
	static String path = "data/sample_B_20160708_frags_46_50.hdf";

	// static String path = "data/sample_B.augmented.0.hdf";
	static String path_label = "/volumes/labels/neuron_ids";

	// float[] voxDim = {10f, 10f, 10f};
	float[] voxDim = { 0.5f, 0.5f, 0.5f };

	// static String path_label = "/volumes/labels/small_neuron_ids";
	// int isoLevel = 2;
	// int[] volDim = {3, 3, 3};

	int isoLevel = 7;

	int[] volDim = { 500, 500, 5 };

	// int isoLevel = 73396;
	// int isoLevel = 1854;
	// int[] volDim = {2340, 1685, 153};

	// float previousDiff = 0.0f;

	/**
	 * This method load the hdf file
	 */
	@BeforeClass
	public static void loadData()
	{

		System.out.println( "Opening labels from " + path );
		final IHDF5Reader reader = HDF5Factory.openForReading( path );
		System.out.println( "reader: " + reader );

		/* labels */
		if ( reader.exists( path_label ) )
		{
			System.out.println( "path exists " );
			try
			{
				readLabels( reader, path_label );
			}
			catch ( IOException e )
			{
				System.out.println( "exception!" );
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
			super( applicationName, windowWidth, windowHeight, true );
		}

		@Override
		public void init()
		{
			setRenderer( Renderer.Factory.createRenderer( getHub(), getApplicationName(), getScene(), getWindowWidth(),
					getWindowHeight() ) );
			getHub().add( SceneryElement.Renderer, getRenderer() );

			final Material material = new Material();
			material.setAmbient( new GLVector( 0.1f * ( 1 ), 1.0f, 1.0f ) );
			material.setDiffuse( new GLVector( 0.1f * ( 1 ), 0.0f, 1.0f ) );
			material.setSpecular( new GLVector( 0.1f * ( 1 ), 0f, 0f ) );

			Mesh neuron = new Mesh();
			neuron.setMaterial( material );
			neuron.setPosition( new GLVector( 0.0f, 0.0f + 0.2f * ( 0 ), 0.0f ) );

			marchingCube( "funkey" );

			neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
			neuron.setNormals( FloatBuffer.wrap( normalsArray ) );

			getScene().addChild( neuron );

			PointLight[] lights = new PointLight[ 4 ];

			for ( int i = 0; i < lights.length; i++ )
			{
				lights[ i ] = new PointLight();
				lights[ i ].setEmissionColor( new GLVector( 1.0f, 1.0f, 1.0f ) );
				lights[ i ].setIntensity( 100.2f * 5 );
				lights[ i ].setLinear( 0.0f );
			}

			lights[ 0 ].setPosition( new GLVector( 1.0f, 0f, -1.0f / ( float ) Math.sqrt( 2.0 ) ) );
			lights[ 1 ].setPosition( new GLVector( -1.0f, 0f, -1.0f / ( float ) Math.sqrt( 2.0 ) ) );
			lights[ 2 ].setPosition( new GLVector( 0.0f, 1.0f, 1.0f / ( float ) Math.sqrt( 2.0 ) ) );
			lights[ 3 ].setPosition( new GLVector( 0.0f, -1.0f, 1.0f / ( float ) Math.sqrt( 2.0 ) ) );

			for ( int i = 0; i < lights.length; i++ )
				getScene().addChild( lights[ i ] );

			final Camera cam = new DetachedHeadCamera();
			cam.setPosition( new GLVector( 0.0f, 0.0f, 5.0f ) );

			cam.perspectiveCamera( 50f, getWindowHeight(), getWindowWidth(), 0.1f, 1000.0f );
			cam.setActive( true );
			getScene().addChild( cam );

//			final Thread neuronPositionThread = new Thread()
//			{
//				@Override
//				public void run()
//				{
//					while ( true )
//					{
//						neuron.setNeedsUpdate( true );
//
//						float diff = cam.getPosition().minus( neuron.getPosition() ).magnitude();
//
//						if ( diff >= 5 && !( previousDiff >= 5 ) )
//						{
//							voxDim = new float[] { 10.0f, 10.0f, 10.0f };
//							marchingCube( "funkey" );
//							neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
//							neuron.setNormals( FloatBuffer.wrap( normalsArray ) );
//
//							previousDiff = diff;
//						}
//						else if ( diff >= 3 && !( previousDiff >= 3 ) )
//						{
//							voxDim = new float[] { 5.0f, 5.0f, 5.0f };
//							marchingCube( "funkey" );
//							neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
//							neuron.setNormals( FloatBuffer.wrap( normalsArray ) );
//
//							previousDiff = diff;
//						}
//						else if ( diff < 3 && !( previousDiff < 3 ) )
//						{
//							voxDim = new float[] { 1.0f, 1.0f, 1.0f };
//							marchingCube( "funkey" );
//							neuron.setVertices( FloatBuffer.wrap( verticesArray ) );
//							neuron.setNormals( FloatBuffer.wrap( normalsArray ) );
//
//							previousDiff = diff;
//						}
//
//						try
//						{
//							Thread.sleep( 20 );
//						}
//						catch ( InterruptedException e )
//						{
//							e.printStackTrace();
//						}
//					}
//				}
//			};
//			neuronPositionThread.start();
		}
	}

	protected static void readLabels( final IHDF5Reader reader, final String labelDataset ) throws IOException
	{
		/* labels */
//		System.out.println( "readLabels starts" );
		final H5LabelMultisetSetupImageLoader labelLoader = new H5LabelMultisetSetupImageLoader( reader, null,
				labelDataset, setupId++, cellDimensions );

//		System.out.println( "labelLoader: " + labelLoader );

		/* converted labels */
		final ARGBConvertedLabelsSource convertedLabelsSource = new ARGBConvertedLabelsSource( setupId++, labelLoader,
				colorStream );

//		System.out.println( "convertedLabelsSource: " + convertedLabelsSource );

		labels.add( labelLoader );
		convertedLabels.add( convertedLabelsSource );

//		System.out.println( "readLabels ends" );
	}

	private void marchingCube( String implementationType )
	{
		if ( implementationType.equals( "ilastik" ) )
		{
			System.out.println( "MarchingCubes - IlastikRAI" );
			MarchingCubes_ilastikRAI mc_ilastik = new MarchingCubes_ilastikRAI();

			begin = new Timestamp( System.currentTimeMillis() );
			viewer.Mesh m = null;
			try
			{
				m = mc_ilastik.march( volumeLabels, volDim[ 0 ], volDim[ 1 ], volDim[ 2 ], isoLevel );
			}
			catch ( FileNotFoundException e )
			{
				e.printStackTrace();
			}
			end = new Timestamp( System.currentTimeMillis() );
			System.out.println( "time for generating mesh: " + ( end.getTime() - begin.getTime() ) );

			begin = new Timestamp( System.currentTimeMillis() );
			int numberOfTriangles = m.getNumberOfTriangles();

			verticesArray = new float[ numberOfTriangles * 3 * 3 ];
			normalsArray = new float[ numberOfTriangles * 3 * 3 ];

			float[][] vertices = m.getVertices();
			float[][] normals = m.getNormals();
			int[] triangles = m.getTriangles();

			float[] point0 = new float[ 3 ];
			float[] point1 = new float[ 3 ];
			float[] point2 = new float[ 3 ];
			int v = 0, n = 0;
			for ( int i = 0; i < numberOfTriangles; i++ )
			{
				long id0 = triangles[ i * 3 ];
				long id1 = triangles[ i * 3 + 1 ];
				long id2 = triangles[ i * 3 + 2 ];

				point0 = vertices[ ( int ) id0 ];
				point1 = vertices[ ( int ) id1 ];
				point2 = vertices[ ( int ) id2 ];

				verticesArray[ v++ ] = point0[ 0 ] / 500;
				verticesArray[ v++ ] = point0[ 1 ] / 500;
				verticesArray[ v++ ] = point0[ 2 ] / 500;
				verticesArray[ v++ ] = point1[ 0 ] / 500;
				verticesArray[ v++ ] = point1[ 1 ] / 500;
				verticesArray[ v++ ] = point1[ 2 ] / 500;
				verticesArray[ v++ ] = point2[ 0 ] / 500;
				verticesArray[ v++ ] = point2[ 1 ] / 500;
				verticesArray[ v++ ] = point2[ 2 ] / 500;

				point0 = normals[ ( int ) id0 ];
				point1 = normals[ ( int ) id1 ];
				point2 = normals[ ( int ) id2 ];

				normalsArray[ n++ ] = point0[ 0 ] / 500;
				normalsArray[ n++ ] = point0[ 1 ] / 500;
				normalsArray[ n++ ] = point0[ 2 ] / 500;
				normalsArray[ n++ ] = point1[ 0 ] / 500;
				normalsArray[ n++ ] = point1[ 1 ] / 500;
				normalsArray[ n++ ] = point1[ 2 ] / 500;
				normalsArray[ n++ ] = point2[ 0 ] / 500;
				normalsArray[ n++ ] = point2[ 1 ] / 500;
				normalsArray[ n++ ] = point2[ 2 ] / 500;
			}
			end = new Timestamp( System.currentTimeMillis() );
			System.out.println( "time for generating arrays: " + ( end.getTime() - begin.getTime() ) );

			System.out.println( "number of vertices and normals: " + numberOfTriangles * 3 * 3 );

		}
		else if ( implementationType.equals( "shibby" ) )
		{
			System.out.println( "MarchingCubes - shibbyRAI" );
			MarchingCubes_shibbyRAI mc_shibby = new MarchingCubes_shibbyRAI();
			int offset = 0;
			begin = new Timestamp( System.currentTimeMillis() );
			ArrayList< float[] > output = mc_shibby.marchingCubes( volumeLabels, volDim, voxDim, isoLevel, offset );
			end = new Timestamp( System.currentTimeMillis() );
			System.out.println( "time for generating mesh: " + ( end.getTime() - begin.getTime() ) );

			begin = new Timestamp( System.currentTimeMillis() );
			verticesArray = new float[ output.size() * 3 ];
			int i = 0;
			for ( float[] floatV : output )
			{
				for ( float f : floatV )
				{
					verticesArray[ i++ ] = f;
					System.out.println( "vertice value: " + f );
				}
			}

			normalsArray = new float[ mc_shibby.getNormals().size() * 3 ];
			i = 0;
			for ( float[] floatV : mc_shibby.getNormals() )
			{
				for ( float f : floatV )
				{
					normalsArray[ i++ ] = f;
				}
			}

			end = new Timestamp( System.currentTimeMillis() );
			System.out.println( "time for generating arrays: " + ( end.getTime() - begin.getTime() ) );

			System.out.println( "number of vertices and normals: " + output.size() * 3 );
		}
		else if ( implementationType.equals( "threeDViewer" ) )
		{
			System.out.println( "MarchingCubes - ThreeDViewer" );
			MarchingCubes_ThreeDViewer mc_threeDViewer = new MarchingCubes_ThreeDViewer();

			begin = new Timestamp( System.currentTimeMillis() );
			DefaultMesh m = mc_threeDViewer.calculate( volumeLabels, isoLevel );
			end = new Timestamp( System.currentTimeMillis() );
			System.out.println( "time for generating mesh: " + ( end.getTime() - begin.getTime() ) );

			begin = new Timestamp( System.currentTimeMillis() );
			System.out.println( "Converting mesh to scenery mesh..." );

			List< Facet > facets = m.getFacets();
			verticesArray = new float[ facets.size() * 3 * 3 ];
			normalsArray = new float[ facets.size() * 3 * 3 ];

			int count = 0;
			List< Vertex > vertices;
			for ( Facet facet : facets )
			{
				TriangularFacet tri = ( TriangularFacet ) facet;
				vertices = tri.getVertices();
				Vector3D normal = tri.getNormal();
				for ( Vertex v : vertices )
				{
					for ( int d = 0; d < 3; d++ )
					{
						verticesArray[ count ] = ( float ) v.getDoublePosition( d );
						if ( d == 0 )
							normalsArray[ count ] = ( float ) normal.getX();
						else if ( d == 1 )
							normalsArray[ count ] = ( float ) normal.getY();
						else if ( d == 2 )
							normalsArray[ count ] = ( float ) normal.getZ();
						count++;
					}
				}
			}
			end = new Timestamp( System.currentTimeMillis() );
			System.out.println( "time for generating arrays: " + ( end.getTime() - begin.getTime() ) );
		}
		else if ( implementationType.equals( "funkey" ) )
		{
			System.out.println( "MarchingCubes - funkey" );
			MarchingCubes_funkeyRAI mc_funkey = new MarchingCubes_funkeyRAI();

			begin = new Timestamp( System.currentTimeMillis() );
			viewer.Mesh m = mc_funkey.generateSurface( volumeLabels, voxDim, volDim, true, isoLevel );
			end = new Timestamp( System.currentTimeMillis() );
			System.out.println( "time for generating mesh: " + ( end.getTime() - begin.getTime() ) );

			begin = new Timestamp( System.currentTimeMillis() );
			int numberOfTriangles = m.getNumberOfTriangles();

			verticesArray = new float[ numberOfTriangles * 3 * 3 ];
			normalsArray = new float[ numberOfTriangles * 3 * 3 ];

			float[][] vertices = m.getVertices();
			float[][] normals = m.getNormals();
			int[] triangles = m.getTriangles();

			float[] point0 = new float[ 3 ];
			float[] point1 = new float[ 3 ];
			float[] point2 = new float[ 3 ];
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
				verticesArray[ v++ ] = point0[ 1 ];
				verticesArray[ v++ ] = point0[ 2 ];
				verticesArray[ v++ ] = point1[ 0 ];
				verticesArray[ v++ ] = point1[ 1 ];
				verticesArray[ v++ ] = point1[ 2 ];
				verticesArray[ v++ ] = point2[ 0 ];
				verticesArray[ v++ ] = point2[ 1 ];
				verticesArray[ v++ ] = point2[ 2 ];

				point0 = normals[ ( int ) id0 ];
				point1 = normals[ ( int ) id1 ];
				point2 = normals[ ( int ) id2 ];

				normalsArray[ n++ ] = point0[ 0 ];
				normalsArray[ n++ ] = point0[ 1 ];
				normalsArray[ n++ ] = point0[ 2 ];
				normalsArray[ n++ ] = point1[ 0 ];
				normalsArray[ n++ ] = point1[ 1 ];
				normalsArray[ n++ ] = point1[ 2 ];
				normalsArray[ n++ ] = point2[ 0 ];
				normalsArray[ n++ ] = point2[ 1 ];
				normalsArray[ n++ ] = point2[ 2 ];
			}
			end = new Timestamp( System.currentTimeMillis() );
			System.out.println( "time for generating arrays: " + ( end.getTime() - begin.getTime() ) );
			System.out.println( "number of vertices and normals: " + numberOfTriangles * 3 * 3 );
		}
	}
}
