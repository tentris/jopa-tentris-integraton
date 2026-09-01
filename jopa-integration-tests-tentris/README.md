# Tentris OntoDriver Integration Tests

To enable the tests, set the following variables in `src/test/resources/config.properties`:

- cz.cvut.kbss.ontodriver.tentris.host
- cz.cvut.kbss.ontodriver.tentris.port
- cz.cvut.kbss.ontodriver.tentris.query-endpoint
- cz.cvut.kbss.ontodriver.tentris.update-endpoint
- cz.cvut.jopa.dataSource.tentris.username
- cz.cvut.jopa.dataSource.tentris.password

Setting the query-endpoint only enforces both update and query endpoint to point to the same underlying endpoint.

For using authentication username and password have to be set properly to interact with the underlying datasource.