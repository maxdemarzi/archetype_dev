package pe.archety;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Path("/service")
public class ArchetypeService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final PathFinder<org.neo4j.graphdb.Path> SHORTEST_PATH_LINKS_LEVEL_ONE =
        GraphAlgoFactory.shortestPath(
                PathExpanders.forTypeAndDirection(RelationshipTypes.LINKS, Direction.BOTH),
                1);



    @GET
    @Path("/helloworld")
    public String helloWorld() {
        return "Hello World!";
    }

    @GET
    @Path("/warmup")
    public String warmUp(@Context GraphDatabaseService db) {
        try ( Transaction tx = db.beginTx()) {
            for ( Node n : GlobalGraphOperations.at(db).getAllNodes()) {
                n.getPropertyKeys();
                for ( Relationship relationship : n.getRelationships()) {
                    relationship.getPropertyKeys();
                    relationship.getStartNode();
                }
            }
        }
        return "Warmed up and ready to go!";
    }

    @GET
    @Path("/migrate")
    public String migrate(@Context GraphDatabaseService db) {
        boolean migrated;
        try (Transaction tx = db.beginTx()) {
            migrated = db.schema().getConstraints().iterator().hasNext();
        }

        if (migrated){
            return "Already Migrated!";
        } else {
            // Perform Migration
            try (Transaction tx = db.beginTx()) {
                Schema schema = db.schema();
                schema.constraintFor(Labels.Identity)
                        .assertPropertyIsUnique("hash")
                        .create();
                schema.constraintFor(Labels.Page)
                        .assertPropertyIsUnique("url")
                        .create();
                tx.success();
            }
            // Wait for indexes to come online
            try (Transaction tx = db.beginTx()) {
                Schema schema = db.schema();
                schema.awaitIndexesOnline(1, TimeUnit.DAYS);
            }
            return "Migrated!";
        }
    }

    @GET
    @Path("/identity")
    public Response getIdentity(@DefaultValue("") @QueryParam("email") String email,
                                @DefaultValue("") @QueryParam("md5hash") String hash,
                                @Context GraphDatabaseService db) throws IOException {
        hash = Identity.getHash(email, hash);
        try ( Transaction tx = db.beginTx() ) {
            final Node identity = Identity.getIdentityNode(hash, db);

            return Response.ok(objectMapper.writeValueAsString(
                Collections.singletonMap("identity", (String)identity.getProperty("hash")))).build();
        }
    }

    @GET
    @Path("/identity/likes")
    public Response getIdentityLikes(@DefaultValue("") @QueryParam("email") String email,
                                    @DefaultValue("") @QueryParam("md5hash") String hash,
                                    @Context GraphDatabaseService db) throws IOException {
        hash = Identity.getHash(email, hash);

        try ( Transaction tx = db.beginTx() ) {
            final Node identity = Identity.getIdentityNode(hash, db);

            ArrayList<String> results = new ArrayList<>();
            for (Relationship likes : identity.getRelationships(Direction.OUTGOING, RelationshipTypes.LIKES)) {
                results.add((String) likes.getEndNode().getProperty("url"));
            }
            return Response.ok(objectMapper.writeValueAsString(results)).build();
        }
    }

    @GET
    @Path("/identity/hates")
    public Response getIdentityHates(@DefaultValue("") @QueryParam("email") String email,
                                     @DefaultValue("") @QueryParam("md5hash") String hash,
                                     @Context GraphDatabaseService db) throws IOException {
        hash = Identity.getHash(email, hash);

        try ( Transaction tx = db.beginTx() ) {
            final Node identity = Identity.getIdentityNode(hash, db);

            ArrayList<String> results = new ArrayList<>();
            for (Relationship hates : identity.getRelationships(Direction.OUTGOING, RelationshipTypes.HATES)) {
                results.add((String) hates.getEndNode().getProperty("url"));
            }
            return Response.ok(objectMapper.writeValueAsString(results)).build();
        }
    }

    @GET
    @Path("/identity/knows")
    public Response getIdentityKnows(@DefaultValue("") @QueryParam("email") String email,
                                     @DefaultValue("") @QueryParam("md5hash") String hash,
                                     @Context GraphDatabaseService db) throws IOException {
        hash = Identity.getHash(email, hash);

        try ( Transaction tx = db.beginTx() ) {
            final Node identity = Identity.getIdentityNode(hash, db);

            ArrayList<String> results = new ArrayList<>();
            for (Relationship knows : identity.getRelationships(Direction.OUTGOING, RelationshipTypes.KNOWS)) {
                results.add((String) knows.getEndNode().getProperty("hash"));
            }
            return Response.ok(objectMapper.writeValueAsString(results)).build();
        }
    }

    @POST
    @Path("/identity")
    public Response createIdentity(@DefaultValue("") @QueryParam("email") String email,
                                   @DefaultValue("") @QueryParam("md5hash") String hash,
                                   @Context GraphDatabaseService db) throws IOException {
        hash = Identity.getHash(email, hash);

        try ( Transaction tx = db.beginTx() ) {
            Node identity = IteratorUtil.singleOrNull(
                    db.findNodesByLabelAndProperty(Labels.Identity, "hash", hash));
            if (identity == null) {
                identity = db.createNode(Labels.Identity);
                identity.setProperty("hash", hash);
            }
            tx.success();
        } catch (Throwable t){
            // If it is not a duplicate, then something else went wrong
            if (!(t instanceof ConstraintViolationException)) {
                throw Exception.identityNotCreated;
            }
        }

        return Response.ok().build();
    }

    @GET
    @Path("/page")
    public Response getPage(@DefaultValue("") @QueryParam("url") String url,
                                @Context GraphDatabaseService db) throws IOException {
        if(url.isEmpty()) {
            throw Exception.missingQueryParameters;
        }

        try ( Transaction tx = db.beginTx() ) {
            final Node page = Page.getPageNode(url, db);

            return Response.ok(objectMapper.writeValueAsString(
                    Collections.singletonMap("url", (String)page.getProperty("url")))).build();
        }
    }

    @GET
    @Path("/page/links")
    public Response getPageLinks(@DefaultValue("") @QueryParam("url") String url,
                                     @Context GraphDatabaseService db) throws IOException {
        url = Page.getPageURL(url);
        try ( Transaction tx = db.beginTx() ) {
            final Node page = Page.getPageNode(url, db);


            ArrayList<String> results = new ArrayList<>();
            for (Relationship links : page.getRelationships(Direction.OUTGOING, RelationshipTypes.LINKS)) {
                results.add((String) links.getEndNode().getProperty("url"));
            }
            return Response.ok(objectMapper.writeValueAsString(results)).build();
        }
    }

    @POST
    @Path("/page")
    public Response createPage(@DefaultValue("") @QueryParam("url") String url,
                               @Context GraphDatabaseService db) throws IOException {
        url = Page.getPageURL(url);

        try ( Transaction tx = db.beginTx() ) {
            Node page = IteratorUtil.singleOrNull(
                    db.findNodesByLabelAndProperty(Labels.Page, "url", url));
            if (page == null) {
                page = db.createNode(Labels.Page);
                page.setProperty("url", url);
            }
            tx.success();
        } catch (Throwable t){
            // If it is not a duplicate, then something else went wrong
            if (!(t instanceof ConstraintViolationException)) {
                throw Exception.pageNotCreated;
            }
        }

        return Response.ok().build();
    }

    @POST
    @Path("/page/links")
    public Response createPageLinks(@DefaultValue("") @QueryParam("url") String url,
                                    @DefaultValue("") @QueryParam("link") String link,
                                 @Context GraphDatabaseService db) throws IOException {
        url = Page.getPageURL(url);
        link = Page.getPageURL(link);

        if(Page.isValidPageLink(url, link)){
            try ( Transaction tx = db.beginTx() ) {
                final Node page = Page.getPageNode(url, db);
                final Node page2 = Page.getPageNode(link, db);

                tx.acquireWriteLock(page);
                tx.acquireWriteLock(page2);

                if (SHORTEST_PATH_LINKS_LEVEL_ONE.findSinglePath(page, page2) == null) {
                    page.createRelationshipTo(page2, RelationshipTypes.LINKS);
                    tx.success();
                }
            }
        }
        return Response.ok().build();
    }

}
