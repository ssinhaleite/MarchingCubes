package marchingCubes;

import java.util.concurrent.Callable;

import bdv.labels.labelset.LabelMultisetType;
import marchingCubes.MarchingCubes.ForegroundCriterion;
import net.imglib2.RandomAccessibleInterval;
import util.Mesh;

public class MarchingCubesCallable implements Callable< Mesh >
{
	/** volume data */
	RandomAccessibleInterval< LabelMultisetType > volume;

	/** volume dimension */
	int[] volDim;

	/** offset to positioning the vertices in global coordinates */
	int[] offset;

	/** marching cube voxel dimension */
	int[] cubeSize;

	/**
	 * defines if the criterion that will be used to generate the mesh
	 */
	ForegroundCriterion criterion;

	/** the value to match the criterion */
	int foregroundValue;

	/**
	 * indicates if it is to use the implementation directly with RAI (false) or
	 * if we must copy the data for an array first (true)
	 */
	boolean copyToArray;

	public MarchingCubesCallable( RandomAccessibleInterval< LabelMultisetType > input, int[] volDim, int[] offset, int[] cubeSize, ForegroundCriterion criterion, int level, boolean usingRAI )
	{
		this.volume = input;
		this.volDim = volDim;
		this.offset = offset;
		this.cubeSize = cubeSize;
		this.criterion = criterion;
		this.foregroundValue = level;
		this.copyToArray = usingRAI;
	}

	@Override
	public Mesh call() throws Exception
	{
		MarchingCubes mc_rai = new MarchingCubes();
		Mesh m = mc_rai.generateMesh( volume, volDim, offset, cubeSize, criterion, foregroundValue, copyToArray );

		return m;
	}
}
