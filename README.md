archetype_extension
===================

High Performance Neo4j Sample Service

1. Build it:

        mvn clean package

2. Copy target/archetype_extension-1.0-SNAPSHOT.jar to the plugins/ directory of your Neo4j server.

3. Download external dependencies to the plugins/ directory of your Neo4j server:
        
        curl -O http://central.maven.org/maven2/commons-validator/commons-validator/1.4.0/commons-validator-1.4.0.jar
        curl -O http://central.maven.org/maven2/com/google/guava/guava/18.0/guava-18.0.jar
        
4. Configure Neo4j by adding a line to conf/neo4j-server.properties:

        org.neo4j.server.thirdparty_jaxrs_classes=pe.archety=/v1

5. Start Neo4j server.

6. Check that it is installed correctly over HTTP:

        :GET /ext/v1/helloworld

7. Warm up the database (optional):

        :GET /ext/v1/warmup



