package com.moviefy;
import java.util.*;
import java.util.Map.*;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.system.Txn;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

public class test {

	public static void main(String[] args){
		RDFConnection conn0 = RDFConnectionRemote.create()
	            .destination("http://moviefy.ddns.net:3030/imdb/")
	            .queryEndpoint("sparql")
	            // Set a specific accept header; here, sparql-results+json (preferred) and text/tab-separated-values
	            // The default is "application/sparql-results+json, application/sparql-results+xml;q=0.9, text/tab-separated-values;q=0.7, text/csv;q=0.5, application/json;q=0.2, application/xml;q=0.2, */*;q=0.1" 
	            //.acceptHeaderSelectQuery("application/sparql-results+json, application/sparql-results+xml;q=0.9")
	            .build();
	        
	        Query query = QueryFactory.create("PREFIX imdb: <http://imdb.com/> DESCRIBE imdb:imdb:tt4846340");

	        // Whether the connection can be reused depends on the details of the implementation.
	        // See example 5.
	        /*try ( RDFConnection conn = conn0 ) { 
	            conn.queryResultSet(query, ResultSetFormatter::out);
	        }*/
	        
	        /*try ( RDFConnection conn = conn0 ) {
	            Txn.executeWrite(conn, ()-> {
	               conn.update("PREFIX imdb: <http://imdb.com/> \n"
	               		+ "PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#> \n"
	               		+ "INSERT DATA { imdb:tt4846340 mo:hasTest \"Test 2\" }" ) ;
	               }) ;
	        }*/
	        String updateStr = "PREFIX imdb: <http://imdb.com/> \n"
               		+ "PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#> \n"
               		+ "INSERT DATA { imdb:tt4846340 mo:hasTest \"Test 2\" }";
	        
	        CredentialsProvider credsProvider = new BasicCredentialsProvider();
			Credentials credentials = new UsernamePasswordCredentials("admin", "JenaUnicamp@2019");
			credsProvider.setCredentials(AuthScope.ANY, credentials);
			HttpClient httpclient = HttpClients.custom()
			    .setDefaultCredentialsProvider(credsProvider)
			    .build();
			HttpOp.setDefaultHttpClient(httpclient);
			
	
	        UpdateRequest update  = UpdateFactory.create(updateStr);
	        UpdateProcessor qexec = UpdateExecutionFactory.createRemote(update, "http://localhost:3030/imdb/update",httpclient);
	        qexec.execute();

}
}
