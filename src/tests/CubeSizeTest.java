package tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.ArrayList;

import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.LabelMultisetType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import marchingCubes.MarchingCubes;
import net.imglib2.RandomAccessibleInterval;
import util.HDF5Reader;

/**
 * Unit test for marching cubes
 * 
 * @author vleite
 */
public class CubeSizeTest
{
	private static RandomAccessibleInterval< LabelMultisetType > volumeLabels = null;

	static Timestamp begin = new Timestamp( System.currentTimeMillis() );

	static Timestamp end = new Timestamp( System.currentTimeMillis() );

	// hdf file to use on test
	static String path = "data/sample_B_20160708_frags_46_50.hdf";

	static String path_label = "/volumes/labels/neuron_ids";

	static int[] volDim = { 500, 500, 5 };

	static int[] offsets = { 0, 0, 0 };

	static String filename = "";

	static MarchingCubes.ForegroundCriterion criterion = MarchingCubes.ForegroundCriterion.EQUAL;

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

		int foregroundValue = 0;
		for ( int size = 128; size >= 1; size /= 2 )
		{
			filename = "mc_cubeSize_" + size + ".txt";
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

			int[] cubeSize = { size, size, size };
			for ( int i = 0; i < 27; i++ )
			{
				foregroundValue = i + 1;
				System.out.println( "MarchingCubes isolevel: " + foregroundValue );
				MarchingCubes mc = new MarchingCubes();
				begin = new Timestamp( System.currentTimeMillis() );
				mc.generateMesh( volumeLabels, volDim, offsets, cubeSize, criterion, foregroundValue, true );
				end = new Timestamp( System.currentTimeMillis() );
				System.out.println( "complete time for generating mesh: " + ( end.getTime() - begin.getTime() ) );
			}
		}
	}
}
