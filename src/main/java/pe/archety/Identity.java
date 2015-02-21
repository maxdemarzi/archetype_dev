package pe.archety;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import org.apache.commons.validator.routines.EmailValidator;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.api.exceptions.schema.UniqueConstraintViolationKernelException;
import org.neo4j.kernel.impl.util.Charsets;
import org.neo4j.server.database.CypherExecutor;
import org.neo4j.shell.kernel.apps.cypher.Cypher;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public class Identity {

    public static boolean isValidEmail(String email) {
        return EmailValidator.getInstance().isValid(email);
    }

    public static boolean isValidMD5(String hash) {
        return hash.matches("[a-f0-9]{32}");
    }

    public static String calculateHash(String input) {
        HashFunction hf = Hashing.md5();
        HashCode hc = hf.newHasher()
                .putString(input.toLowerCase(), Charsets.UTF_8)
                .hash();
        return hc.toString();
    }

    public static String getHash(String email, String hash) {
        if (hash.isEmpty()){
            if(email.isEmpty()){
                throw pe.archety.Exception.missingQueryParameters;
            } else {
                if(isValidEmail(email)) {
                    return Identity.calculateHash(email);
                } else {
                    throw Exception.invalidEmailParameter;
                }
            }
        } else {
            hash = hash.toLowerCase();
            if(Identity.isValidMD5(hash)){
                return hash;
            } else {
                throw Exception.invalidMD5HashParameter;
            }
        }
    }

    public static Node getIdentityNode(String hash, GraphDatabaseService db) {
        Node identity = IteratorUtil.singleOrNull(
                db.findNodesByLabelAndProperty(Labels.Identity, "hash", hash));
        if(identity != null) {
            return identity;
        } else {
            throw Exception.identityNotFound;
        }
    }

    private static final String MERGE_IDENTITY_CYPHER = "MERGE (n:Identity {hash: {hash}}) RETURN n";

    public static Node createIdentityNode(String hash, CypherExecutor cypherExecutor) {
       ExecutionEngine engine = cypherExecutor.getExecutionEngine();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put( "hash", hash );

        ResourceIterator<Node> resultIterator;
        resultIterator = engine.execute( MERGE_IDENTITY_CYPHER, parameters ).columnAs( "n" );
        return resultIterator.next();
    }

}
