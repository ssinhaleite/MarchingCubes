# MarchingCubes
Marching cubes implementation that uses scenery as visualization interface.

# Dependencies

## MVN
You need download and instal maven. [Instructions](https://maven.apache.org/download.cgi#Installation).

## BigCat

You need to clone the bigcat repository:
``` git clone https://github.com/ssinhaleite/bigcat.git ```

and install it:
``` mvn clean install```

# Compile

Download the code:
``` git clone https://github.com/ssinhaleite/MarchingCubes.git```

and

## generate a fat jar
``` mvn clean compile assembly:single```

# Run 

Using default parameters and data from resources:

from main folder:
```java -jar target/MarchingCubes-0-SNAPSHOT-jar-with-dependencies.jar```

Using your own parameters:
```java -jar target/MarchingCubes-0-SNAPSHOT-jar-with-dependencies.jar <parameters list> ```

## Parameters:

| Option | Description |
|--------|-------------|
|`--file` or `-f` | input file path (hdf5) |
|`--label` or `-l` | label dataset name: volume with ids |
|`--neuronId` or `-id` | id of the subject that will be used to generate the mesh |
