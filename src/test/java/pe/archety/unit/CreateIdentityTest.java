package pe.archety.unit;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import org.neo4j.test.TestGraphDatabaseFactory;
import pe.archety.*;
import pe.archety.Exception;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static pe.archety.TestObjects.*;

public class CreateIdentityTest {
    private ArchetypeService service;
    private GraphDatabaseService db;

    public void populate(GraphDatabaseService db) {
        try (Transaction tx = db.beginTx()) {
            Node identity = db.createNode(Labels.Identity);
            identity.setProperty("hash", validMD5Hash2);
            tx.success();
        }
    }

    @Before
    public void setUp() throws URISyntaxException {
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        service = new ArchetypeService();
        service.migrate(db);
        populate(db);
    }

    @After
    public void tearDown() {
        db.shutdown();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldCreateIdentityByEmail() throws IOException {
        Response response = service.createIdentity(validEmail, "", db);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldCreateIdentityByHash() throws IOException {
        Response response = service.createIdentity("", validMD5Hash, db);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldGetDuplicateIdentities() throws IOException {
        Response response = service.createIdentity("", validMD5Hash2, db);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldGetIdentityNotCreatedException() throws IOException {
        thrown.expect(pe.archety.Exception.class);
        thrown.expectMessage(Exception.identityNotCreated.getMessage());
        db.shutdown();
        service.createIdentity("", validMD5Hash, db);
    }
}