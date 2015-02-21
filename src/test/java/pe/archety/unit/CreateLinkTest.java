package pe.archety.unit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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

public class CreateLinkTest {

    private ArchetypeService service;
    private GraphDatabaseService db;

    public void populate(GraphDatabaseService db) {
        try (Transaction tx = db.beginTx()) {

            Node page = db.createNode(Labels.Page);
            page.setProperty("url", validURL);

            Node page2 = db.createNode(Labels.Page);
            page2.setProperty("url", validLinkedURL);

            Node page3 = db.createNode(Labels.Page);
            page3.setProperty("url", validURL2);

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
    public void shouldCreateLink() throws IOException {
        Response response = service.createPageLinks(validURL, validLinkedURL, db);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldGetPageNotFoundException() throws IOException {
        thrown.expect(pe.archety.Exception.class);
        thrown.expectMessage(Exception.pageNotFound.getMessage());
        service.createPageLinks(validURL, validLinkedURL2, db);
    }

    @Test
    public void shouldGetPageNotFoundException2() throws IOException {
        thrown.expect(pe.archety.Exception.class);
        thrown.expectMessage(Exception.pageNotFound.getMessage());
        service.createPageLinks(validLinkedURL2, validURL, db);
    }

    @Test
    public void shouldGetWikipediaPagesNotLinkedException() throws IOException {
        thrown.expect(pe.archety.Exception.class);
        thrown.expectMessage(Exception.wikipediaPagesNotLinked.getMessage());
        service.createPageLinks(validURL, validURL2, db);
    }
}
