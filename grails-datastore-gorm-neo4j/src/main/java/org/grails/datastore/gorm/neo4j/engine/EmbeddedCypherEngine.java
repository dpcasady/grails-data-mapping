package org.grails.datastore.gorm.neo4j.engine;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Stack;

/**
 * CypherEngine implementation backed by {@link ExecutionEngine}
 */
public class EmbeddedCypherEngine implements CypherEngine {

    private static Logger log = LoggerFactory.getLogger(EmbeddedCypherEngine.class);
    private ExecutionEngine executionEngine;
    private GraphDatabaseService graphDatabaseService;

    private static ThreadLocal<Stack<Transaction>> transactionStackThreadLocal = new ThreadLocal<Stack<Transaction>>() {
        @Override
        protected Stack<Transaction> initialValue() {
            return new Stack<Transaction>();
        }
    };

    public EmbeddedCypherEngine(GraphDatabaseService graphDatabaseService) {
        this(graphDatabaseService, new ExecutionEngine(graphDatabaseService));
    }

    public EmbeddedCypherEngine(GraphDatabaseService graphDatabaseService, ExecutionEngine executionEngine) {
        this.graphDatabaseService = graphDatabaseService;
        this.executionEngine = executionEngine;
    }

    @Override
    public CypherResult execute(String cypher, Map params) {
        log.info("running cypher {}", cypher);
        log.info("   with params {}", params);
        return new EmbeddedCypherResult(params != null ?
            executionEngine.execute(cypher, params) :
            executionEngine.execute(cypher)
        );
    }

    @Override
    public CypherResult execute(String cypher) {
        return execute(cypher, null);
    }

    @Override
    public void beginTx() {
        Stack transactionStack = transactionStackThreadLocal.get();
        transactionStack.push(graphDatabaseService.beginTx());
    }

    @Override
    public void commit() {
        Transaction tx = transactionStackThreadLocal.get().pop();
        tx.success();
        tx.close();
    }

    @Override
    public void rollback() {
        Transaction tx = transactionStackThreadLocal.get().pop();
        tx.failure();
        tx.close();
    }

}
