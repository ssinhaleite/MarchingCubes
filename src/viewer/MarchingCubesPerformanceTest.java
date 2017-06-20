package viewer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.ArrayList;

import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.LabelMultisetType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import marchingCubes.CountingVoxels;
import marchingCubes.MarchingCubesRAI;
import net.imglib2.RandomAccessibleInterval;
import util.HDF5Reader;

/**
 * Unit test for marching cubes
 * 
 * @author vleite
 */
public class MarchingCubesPerformanceTest
{
	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	static Timestamp begin = new Timestamp( System.currentTimeMillis() );

	static Timestamp end = new Timestamp( System.currentTimeMillis() );

	static float[] verticesArray = null;

	static float[] normalsArray = null;

	// hdf file to use on test
	static String path = "data/sample_B_20160708_frags_46_50.hdf";

	static String path_label = "/volumes/labels/neuron_ids";

	static int[] volDim = { 500, 500, 5 };

	static String filename = "";

	public static void main( String[] args ) throws IOException
	{
		final IHDF5Reader reader = HDF5Factory.openForReading( path );
		/** loaded segments */
		ArrayList< H5LabelMultisetSetupImageLoader > labels = null;

		if ( reader.exists( path_label ) )
		{
			labels = HDF5Reader.readLabels( reader, path_label );
		}

		volumeLabels = labels.get( 0 ).getImage( 0 );

		int isoLevel = 0;
		for ( int size = 32; size > 0; size /= 2 )
		{
			filename = "mc_specific" + size + ".txt";
			PrintStream fileStream = null;
			try
			{
				fileStream = new PrintStream( filename );
			}
			catch ( FileNotFoundException e1 )
			{
				e1.printStackTrace();
			}
			System.setOut( fileStream );
			
			float[] voxDim = { size, size, size };

			for ( int i = 0; i < 27; i++ )
			{
				isoLevel = i + 1;
				System.out.println( "MarchingCubes isolevel: " + isoLevel );
				MarchingCubesRAI mc_rai = new MarchingCubesRAI();
//				CountingVoxels mc_rai = new CountingVoxels();

//				begin = new Timestamp( System.currentTimeMillis() );
				viewer.Mesh m = mc_rai.generateSurface( volumeLabels, voxDim, volDim, true, isoLevel );
//				end = new Timestamp( System.currentTimeMillis() );
//				System.out.println( "complete time for generating mesh: " + ( end.getTime() - begin.getTime() ) );

//				begin = new Timestamp( System.currentTimeMillis() );
//				int numberOfTriangles = m.getNumberOfTriangles();
//				verticesArray = new float[ numberOfTriangles * 3 * 3 ];
//				normalsArray = new float[ numberOfTriangles * 3 * 3 ];
//
//				float[][] vertices = m.getVertices();
//				float[][] normals = m.getNormals();
//				int[] triangles = m.getTriangles();
//
//				float[] point0 = new float[ 3 ];
//				float[] point1 = new float[ 3 ];
//				float[] point2 = new float[ 3 ];
//				int v = 0, n = 0;
//				for ( int t = 0; t < numberOfTriangles; t++ )
//				{
//					long id0 = triangles[ t * 3 ];
//					long id1 = triangles[ t * 3 + 1 ];
//					long id2 = triangles[ t * 3 + 2 ];
//
//					point0 = vertices[ ( int ) id0 ];
//					point1 = vertices[ ( int ) id1 ];
//					point2 = vertices[ ( int ) id2 ];
//
//					verticesArray[ v++ ] = point0[ 0 ];
//					verticesArray[ v++ ] = point0[ 1 ];
//					verticesArray[ v++ ] = point0[ 2 ];
//					verticesArray[ v++ ] = point1[ 0 ];
//					verticesArray[ v++ ] = point1[ 1 ];
//					verticesArray[ v++ ] = point1[ 2 ];
//					verticesArray[ v++ ] = point2[ 0 ];
//					verticesArray[ v++ ] = point2[ 1 ];
//					verticesArray[ v++ ] = point2[ 2 ];
//
//					point0 = normals[ ( int ) id0 ];
//					point1 = normals[ ( int ) id1 ];
//					point2 = normals[ ( int ) id2 ];
//
//					normalsArray[ n++ ] = point0[ 0 ];
//					normalsArray[ n++ ] = point0[ 1 ];
//					normalsArray[ n++ ] = point0[ 2 ];
//					normalsArray[ n++ ] = point1[ 0 ];
//					normalsArray[ n++ ] = point1[ 1 ];
//					normalsArray[ n++ ] = point1[ 2 ];
//					normalsArray[ n++ ] = point2[ 0 ];
//					normalsArray[ n++ ] = point2[ 1 ];
//					normalsArray[ n++ ] = point2[ 2 ];
//				}
//				end = new Timestamp( System.currentTimeMillis() );
//				System.out.println( "time for generating arrays: " + ( end.getTime() - begin.getTime() ) );
//
//				System.out.println( "number of vertices and normals: " + numberOfTriangles * 3 * 3 );
			}
		}
	}
}
