# JOPA OntoDriver Integration

This document describes the changes that were necessary in order to implement the OntoDriver interface on top of a
Tentris store.

## Patches

### SPARQL Repository Adaptation

RDF4J requests XML result formats by default. JOPA uses the unit separator `U+001F` as the `GROUP_CONCAT` separator
(`GroupConcatQueryModifier.GROUP_CONCAT_SEPARATOR`). The store echoes that character back inside the concatenated
literal, but XML 1.0 does not permit C0 control characters, so the XML results parser rejected the response and the
driver failed.

To avoid this, the driver introduces `TentrisSparqlRepository`, which requests JSON for both tuple and boolean query
results. The same repository also enables RDF4J's quad mode, so that unscoped reads report the context each statement
originates from.

### Tentris Storage Connection

#### Context loss

`SPARQLConnection` passes the contexts of a scoped `findStatements` call as `default-graph-uri`, which merges them into
the default graph of the query. As a consequence, the returned statements carry no context.

Reading all statements scoped to `<http://example.org/graph-a>` results in the following request:

```sparql
SELECT * WHERE { ?s ?p ?o . OPTIONAL { GRAPH ?ctx { ?s ?p ?o } } }
```

```
default-graph-uri=http://example.org/graph-a
```

The requested context is merged into the default graph of the query, and the dataset declares no named graph at all.
The pattern `?s ?p ?o` therefore matches the triples of `graph-a`, whereas `GRAPH ?ctx { ?s ?p ?o }` never matches.
`?ctx` remains unbound and every statement is returned without a context, even though the caller asked for one
specific graph.

The driver therefore reads each requested context separately and retags every resulting statement with the context it
was read from, so that the returned statements carry their context again.

#### Remove operations

By design, a removal from the default graph has to unfold into a removal from the merged view of the default graph and
all named graphs available on the store. The inherited remove operations only deleted from the default graph, which is
not aligned with the behaviour of the other stores.

Removing a single statement that carries no context results in the following update:

```sparql
DELETE DATA
{
    <http://example.org/s> <http://example.org/p> <http://example.org/o> .
}
```

Under `default-graph-mode = "union"` a read of the default graph answers from the union over all named graphs, so a copy of the triple residing in any named
graph survives the removal and remains visible afterwards. The removal of a property value behaves the same way, the
only difference being that it is expressed as a `DELETE WHERE` over a pattern instead of as `DELETE DATA`.

Both remove operations therefore had to be adapted: the driver resolves all named contexts, adds the default graph
itself, and passes the expanded collection to the corresponding operation of the parent class. The update above is thus
sent as one command per graph within a single transaction:

```sparql
DELETE DATA { GRAPH <http://example.org/graph-a> { <http://example.org/s> <http://example.org/p> <http://example.org/o> . } } ;
DELETE DATA { GRAPH <http://example.org/graph-b> { <http://example.org/s> <http://example.org/p> <http://example.org/o> . } } ;
DELETE DATA { <http://example.org/s> <http://example.org/p> <http://example.org/o> . }
```

#### Checking for endpoint information

This integration is only correct if the underlying store is configured such that querying the default graph yields the
union over all named graphs. In order to fail fast instead of silently returning incomplete results, the connector
verifies this before the repository is initialised. A plain GET on the query endpoint returns the SPARQL 1.1 service
description of the store, which advertises the setting as `sd:feature sd:UnionDefaultGraph`.

### Tentris Storage Connector

During initialisation of the underlying repository, the connector binds it to `TentrisSparqlRepository` in order to
obtain the adapted SPARQL endpoint handling described above. Because Tentris does not support basic authentication yet,
the connector additionally performs a session based login and forwards the resulting session cookie with every request,
so that authentication works as expected.

## Important

Using this driver requires an appropriate configuration in the `tentris-server-config.toml` file that Tentris is started with.

### Generate a default configuration file

```sh
tentris create-default-config > tentris-server-config.toml
```

Edit the generated file and set the `default-graph-mode` entry to `default-graph-mode = "union"`.

### Start the server with your configuration

```sh
tentris --config tentris-server-config.toml serve
```

## Configs

Tentris defines its own configuration parameters for the host, the port and, for authentication, the username and the
password:

| Parameter                                  | Description                        |
|--------------------------------------------|------------------------------------|
| `cz.cvut.kbss.ontodriver.tentris.host`     | Host of the Tentris server         |
| `cz.cvut.kbss.ontodriver.tentris.port`     | Port of the Tentris server         |
| `cz.cvut.jopa.dataSource.tentris.username` | Username used for the login        |
| `cz.cvut.jopa.dataSource.tentris.password` | Password used for the login        |

### Other Configs that can be used with the driver

Beyond its own parameters, the Tentris OntoDriver declares which further configuration parameters it reads. These are
taken over from the generic and the RDF4J driver configuration:

- `cz.cvut.kbss.ontodriver.connection-auto-commit`
- `cz.cvut.kbss.ontodriver.rdf4j.load-all-threshold`
- `cz.cvut.kbss.ontodriver.rdf4j.reconnect-attempts`
- `cz.cvut.kbss.ontodriver.rdf4j.connection-request-timeout`
- `cz.cvut.kbss.ontodriver.rdf4j.max-connections`
