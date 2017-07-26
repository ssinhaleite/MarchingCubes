package util;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class Parameters
{
	@Parameter
	private List< String > parameters = new ArrayList<>();

	@Parameter( names = { "--file", "-f" }, description = "input file path (hdf5)" )
	public String filePath = "";

	@Parameter( names = { "--label", "-l" }, description = "label dataset name" )
	public String labelDatasetPath = "/volumes/labels/neuron_ids";

	@Parameter( names = { "--neuronId", "-id" }, description = "foreground value" )
	public Integer foregroundValue = 1;
}
