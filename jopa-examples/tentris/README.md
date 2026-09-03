# Jopa Tentris Integration How-To

This guide describes how to set up Tentris and run JOPA with Tentris support. To use JOPA with the Tentris integration, the Tentris binary must be installed and the Tentris server must be running.

## Install Tentris Binary

Install the Tentris binary by running:

```sh
curl --proto https --tlsv1.2 -sSf https://raw.githubusercontent.com/tentris/tentris/refs/heads/main/install.sh | sh
```

## Run Tentris

```sh
tentris init

tentris serve
```
For more information about using Tentris, see the Tentris documentation(https://docs.tentris.io).

## Tentris OntoDriver Configuration

To configure the Tentris OntoDriver, specify the following system properties using the -D option when invoking Java:

- cz.cvut.kbss.ontodriver.tentris.host
- cz.cvut.kbss.ontodriver.tentris.port
- cz.cvut.jopa.dataSource.tentris.username (optional)
- cz.cvut.jopa.dataSource.tentris.password (optional)

For example:

```sh
java -cp ... \ 
    -Dcz.cvut.kbss.ontodriver.tentris.host=https://dbpedia.data.dice-research.org \
    -Dcz.cvut.kbss.ontodriver.tentris.port=443
```

## Enabling Authentication

Follows later

## Running Examples

First, build and install the Tentris examples:

```sh
mvn -pl jopa-examples/tentris -am install -q -DskipTests
```

Then, generate the classpath for the examples:

```sh
mvn -pl jopa-examples/tentris dependency:build-classpath \          
    -Dmdep.outputFile=/tmp/tentris-examples-classpath.txt -q
```

Finally, run an example using:

```sh
java -cp "jopa-examples/tentris/target/classes:$(cat /tmp/tentris-examples-cp.txt)" cz.cvut.kbss.jopa.TentrisOWL
```

Replace the class name with the example you want to run:
- cz.cvut.kbss.jopa.TentrisOWL
- cz.cvut.kbss.jopa.TentrisNativeQuery
- cz.cvut.kbss.jopa.TentrisTypedQuery
- cz.cvut.kbss.jopa.TentrisSparqlResultSetMapping
- cz.cvut.kbss.jopa.TentrisSOQL