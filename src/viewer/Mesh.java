package viewer;

/**
 * the primary structure used to pass around the components of a mesh
 */
public class Mesh
{
	/**
	 * the number of vertices and normals
	 */
	int numberOfVertices;

	/**
	 * the vertices positions as an array of points
	 */
	float[][] vertices;

	Mesh( int vertexCount, float[][] verticesArray, int trianglesCount, int[] trianglesArray )
	{
		numberOfVertices = vertexCount;
		vertices = verticesArray;
	}

	public Mesh()
	{}

	public int getNumberOfVertices()
	{
		return numberOfVertices;
	}

	public void setNumberOfVertices( int nVertices )
	{
		numberOfVertices = nVertices;
	}

	public float[][] getVertices()
	{
		return vertices;
	}

	public void setVertices( float[][] verticesArray )
	{
		vertices = verticesArray;
	}
}
