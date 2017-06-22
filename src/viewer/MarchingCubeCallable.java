package viewer;

import java.util.concurrent.Callable;

import bdv.labels.labelset.LabelMultisetType;
import marchingCubes.MarchingCubesRAI;
import net.imglib2.RandomAccessibleInterval;

public class MarchingCubeCallable implements Callable< Mesh >
{

	RandomAccessibleInterval< LabelMultisetType > input;

	float[] voxDim;

	int[] volDim;

	boolean isExact;

	int level;

	public MarchingCubeCallable( RandomAccessibleInterval< LabelMultisetType > input, float[] voxDim, int[] volDim, boolean isExact, int level )
	{
		this.input = input;
		this.voxDim = voxDim;
		this.volDim = volDim;
		this.isExact = isExact;
		this.level = level;
	}

	@Override
	public Mesh call() throws Exception
	{
		System.out.println("call " + input);
		MarchingCubesRAI mc_rai = new MarchingCubesRAI();
		Mesh m = mc_rai.generateSurface2( input, voxDim, volDim, isExact, level );

		return m;
	}
}
