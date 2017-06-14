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

	/**
	 * the normal direction of each vertex as an array of points
	 */
	float[][] normals;

	/**
	 * the number of faces
	 */
	public int numberOfTriangles;

	/**
	 * the triangles given by 3 vertex indices (length = numberOfTriangles * 3)
	 */
	long[] triangles;

	Mesh( int vertexCount, float[][] verticesArray, float[][] normalsArray, int trianglesCount, long[] trianglesArray )
	{
		numberOfVertices = vertexCount;
		vertices = verticesArray;
		normals = normalsArray;
		numberOfTriangles = trianglesCount;
		triangles = trianglesArray;
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
	
	public int getNumberOfTriangles()
	{
		return numberOfTriangles;
	}

	public void setNumberOfTriangles( int ntriangles )
	{
		numberOfTriangles= ntriangles;
	}

	public float[][] getVertices()
	{
		return vertices;
	}

	public void setVertices(float[][] verticesArray)
	{
		vertices = verticesArray;
	}

	
	public float[][] getNormals()
	{
		return normals;
	}
	
	public void setNormals(float[][] normalsArray)
	{
		normals = normalsArray;
	}

	public long[] getTriangles()
	{
		return triangles;
	}
	
	public void setTriangles( long[] trianglesArray)
	{
		triangles = trianglesArray;
	}

}
