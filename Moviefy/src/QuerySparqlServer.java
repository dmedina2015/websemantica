import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;

public class QuerySparqlServer {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String queryString = "PREFIX : <http://www.movieontology.org/2009/11/09/movieontology.owl#>\n" + 
				"PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#>\n" + 
				"PREFIX dbpedia: <http://dbpedia.org/ontology/>\n" + 
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + 
				"\n" + 
				"SELECT ?movie ?actor_name ?birthDate \n" + 
				"WHERE {\n" + 
				"  ?movie mo:title \"War Dogs\".\n" + 
				"  ?movie mo:hasActor ?actor .\n" + 
				"  ?actor foaf:name ?actor_name .\n" + 
				"  ?actor foaf:birthDate ?birthDate .\n" +
				"}\n" + 
				"LIMIT 25";                     
        Query query = QueryFactory.create(queryString) ;

        System.out.println(queryString);

        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://moviefy.ddns.net:3030/teste/sparql", query);        
        ResultSet results = qexec.execSelect();
        ResultSetFormatter.out(System.out, results, query) ;
	}

}
