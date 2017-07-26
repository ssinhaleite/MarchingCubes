package util;

import com.beust.jcommander.Parameter;

/**
 * Parameters used to select the file, dataset and neuron id.
 * 
 * @author vleite
 *
 */
public class Parameters
{
	@Parameter( names = { "--file", "-f" }, description = "input file path (hdf5)" )
	public String filePath = "";

	@Parameter( names = { "--label", "-l" }, description = "label dataset name" )
	public String labelDatasetPath = "/volumes/labels/neuron_ids";

	@Parameter( names = { "--neuronId", "-id" }, description = "foreground value" )
	public Integer foregroundValue = 1;

	@Parameter( names = "--help", help = true )
	private boolean help;
}
