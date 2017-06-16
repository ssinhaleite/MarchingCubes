package util;

import java.io.IOException;
import java.util.ArrayList;

import bdv.bigcat.ui.ModalGoldenAngleSaturatedARGBStream;
import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class HDF5Reader
{
	protected static int setupId = 0;

	final static protected int[] cellDimensions = new int[] { 4, 4, 40 };

	/** color generator for composition of loaded segments and canvas */
	protected static ModalGoldenAngleSaturatedARGBStream colorStream;

	public static ArrayList< H5LabelMultisetSetupImageLoader > readLabels( final IHDF5Reader reader, final String labelDataset ) throws IOException
	{
		final ArrayList< H5LabelMultisetSetupImageLoader > labels = new ArrayList<>();

		/* labels */
//		System.out.println( "readLabels starts" );
		final H5LabelMultisetSetupImageLoader labelLoader = new H5LabelMultisetSetupImageLoader( reader, null,
				labelDataset, setupId++, cellDimensions );

//		System.out.println( "labelLoader: " + labelLoader );
		labels.add( labelLoader );
		//		System.out.println( "readLabels ends" );
		
		return labels;
	}

}
