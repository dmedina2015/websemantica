import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resources;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;

public class QuerySparqlServer {
	
	public static String stripAccents(String s) 
	{
	    s = Normalizer.normalize(s, Normalizer.Form.NFD);
	    s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
	    return s;
	}
	
	
	public static List<String> loadViewedMovies(String path) throws IOException{
		List<String> movies = new ArrayList<>();
		FileInputStream fis = new FileInputStream(path);
		InputStreamReader isr = new InputStreamReader (fis);
		try (BufferedReader br = new BufferedReader(isr)) {  
			String line;
			br.readLine(); // Drops header line
			while ((line = br.readLine()) != null) { 
				String movie = line.split("\",")[0].substring(1); //Get movie name
				movies.add(movie);
			}
		}
		return movies; //return movies collection
	}
	
	
	public static Resource movieFromTitle(String movieName, String lang){
		String queryString = 
				"PREFIX : <http://www.movieontology.org/2009/11/09/movieontology.owl#>\n" + 
				"PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#>\n" +  
				"SELECT ?movie \n" + 
				"WHERE {\n" + 
				"  ?movie mo:title \"" + movieName + "\"" + lang + " .\n" +
				"}\n" +
				"LIMIT 1";
        Query query = QueryFactory.create(queryString) ;
        ResultSet results = null;
        
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://moviefy.ddns.net:3030/imdb/sparql", query)){        
        	results = qexec.execSelect();
        	if (!results.hasNext()) return null;
        	else return results.next().get("movie").asResource();
        }
	}
	
	public static Model buildViewingModel(String userName) throws IOException {
		String mvfyOntNS = "http://moviefy.com/ontology/";
		String mvfyResNS = "http://moviefy.com/resources/";
		String imdbNS = "http://imdb.com/";
		
		Model m = ModelFactory.createDefaultModel();
		Property propView = m.createProperty(mvfyOntNS+"viewed");
		
		m.setNsPrefix("mvfyOnt", mvfyOntNS);
		m.setNsPrefix("mvfyRes", mvfyResNS);
		m.setNsPrefix("foaf", FOAF.getURI());
		m.setNsPrefix("imdb", imdbNS);
		
		Resource user = m.createResource(mvfyResNS+stripAccents(userName.replaceAll(" ", "")));
		user.addProperty(RDF.type, FOAF.Person);
		
		List<String> filmes = loadViewedMovies("/Users/daniel/Downloads/NetflixViewingHistory.csv");
		filmes.forEach((movieTitle) -> {
			Resource movie = movieFromTitle(movieTitle,"");
			if (movie==null) movie = movieFromTitle(movieTitle,"@BR"); //If doesnt find original title, try portuguese.
			if (movie==null) movie = movieFromTitle(movieTitle,"@US"); //If doesnt find portuguese, try english.
			if (movie!=null) user.addProperty(propView, movie);
		});
		return m;
	}
	
	
	public static List<Resource> queryTopGenres(Model m){
		String queryString = 
				 "PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#> \n"
				+ "PREFIX mvfyOnt: <http://moviefy.com/ontology/> \n"
				+ "PREFIX mvfyRes: <http://movify.com/resources/> \n"
				+ "PREFIX imdb: <http://imdb.com/> \n"
				+ "SELECT ?genre (COUNT(?genre) AS ?cgenre)\n"
				+ "	WHERE {\n" 
				+ "		?someone mvfyOnt:viewed ?movie . \n"
				+ "		SERVICE <http://moviefy.ddns.net:3030/imdb/sparql>{ \n"
				+ " 		?movie mo:title ?movietitle. \n"
				+ "			?movie mo:belongsToGenre ?genre. \n"
				+ "		}\n"
				+ "	}\n"
				+ "	GROUP BY ?genre \n"
				+ "	ORDER BY DESC(?cgenre) \n"
				+ "	LIMIT 3";
		List<Resource> genres = new ArrayList<>();
        Query query = QueryFactory.create(queryString) ;
        ResultSet results = null;
        
        try (QueryExecution qexec = QueryExecutionFactory.create(query,m)){        
        	results = qexec.execSelect();
        	//ResultSetFormatter.out(System.out,results,query);
        	while (results.hasNext()) {
        		genres.add(results.next().get("genre").asResource());
        	}
        }
        return genres;
	}
	
	public static List<Resource> queryTopActors(Model m){
		String queryString = 
				 "PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#> \n"
				+ "PREFIX mvfyOnt: <http://moviefy.com/ontology/> \n"
				+ "PREFIX mvfyRes: <http://movify.com/resources/> \n"
				+ "PREFIX imdb: <http://imdb.com/> \n"
				+ "PREFIX foaf: <"+FOAF.getURI()+"> \n"
				
				+ "SELECT ?actor ?actorname (COUNT(?actor) AS ?cactor)\n"
				+ "	WHERE {\n" 
				+ "		?someone mvfyOnt:viewed ?movie . \n"
				+ "		SERVICE <http://moviefy.ddns.net:3030/imdb/sparql>{ \n"
				+ "			?movie mo:hasActor ?actor. \n"
				+ "			?actor foaf:name ?actorname. \n"	
				+ "		}\n"
				+ "	}\n"
				+ "	GROUP BY ?actor ?actorname \n"
				+ "	ORDER BY DESC(?cactor) \n"
				+ "	LIMIT 3";
		List<Resource> actors = new ArrayList<>();
        Query query = QueryFactory.create(queryString) ;
        ResultSet results = null;
        
        try (QueryExecution qexec = QueryExecutionFactory.create(query,m)){        
        	results = qexec.execSelect();
        	//ResultSetFormatter.out(System.out,results,query);
        	while (results.hasNext()) {
        		actors.add(results.next().get("actor").asResource());
        	}
        }
        return actors;
	}
	
	public static List<Resource> queryTopDirectors(Model m){
		String queryString = 
				 "PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#> \n"
				+ "PREFIX mvfyOnt: <http://moviefy.com/ontology/> \n"
				+ "PREFIX mvfyRes: <http://movify.com/resources/> \n"
				+ "PREFIX imdb: <http://imdb.com/> \n"
				+ "PREFIX foaf: <"+FOAF.getURI()+"> \n"
				
				+ "SELECT ?director ?directorname (COUNT(?director) AS ?cdirector)\n"
				+ "	WHERE {\n" 
				+ "		?someone mvfyOnt:viewed ?movie . \n"
				+ "		SERVICE <http://moviefy.ddns.net:3030/imdb/sparql>{ \n"
				+ "			?movie mo:hasDirector ?director. \n"
				+ "			?director foaf:name ?directorname. \n"	
				+ "		}\n"
				+ "	}\n"
				+ "	GROUP BY ?director ?directorname \n"
				+ "	ORDER BY DESC(?cdirector) \n"
				+ "	LIMIT 3";
		List<Resource> directors = new ArrayList<>();
        Query query = QueryFactory.create(queryString) ;
        ResultSet results = null;
        
        try (QueryExecution qexec = QueryExecutionFactory.create(query,m)){        
        	results = qexec.execSelect();
        	//ResultSetFormatter.out(System.out,results,query);
        	while (results.hasNext()) {
        		directors.add(results.next().get("director").asResource());
        	}
        }
        return directors;
	}
	
	public static List<Resource> queryMoviesByGenres(Model m,List<Resource> topGenres, List<Resource> topActors){
		String topGenre1 = "mo:" + topGenres.get(0).getLocalName();
		String topGenre2 = topGenre1;
		String topGenre3 = topGenre1;
		
		String topActor1 = "imdb:" + topActors.get(1).getLocalName();
		
		if(topGenres.size()>1) topGenre2 = "mo:" + topGenres.get(1).getLocalName();
		if(topGenres.size()>2) topGenre3 = "mo:" + topGenres.get(2).getLocalName();
		
		String queryString = 
				 "PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#> \n"
				+ "PREFIX mvfyOnt: <http://moviefy.com/ontology/> \n"
				+ "PREFIX mvfyRes: <http://movify.com/resources/> \n"
				+ "PREFIX imdb: <http://imdb.com/> \n"
				
				+ "SELECT DISTINCT ?movie ?movietitle ?releaseyear WHERE{ \n"
				+ " ?movie mo:belongsToGenre " + topGenre1 + "," + topGenre2 + /*"," +  topGenre3 +*/ ".\n"
				+ " ?movie mo:hasActor " + topActor1 +". \n"
				+ " ?movie mo:title ?movietitle .\n"
				+ " ?movie mo:releasedate ?date. \n"
				+ " BIND (YEAR(?date) as ?releaseyear) \n"
				+ " FILTER (?releaseyear > 2000) \n"
				+ " FILTER (LANG(?movietitle)=\"BR\") \n"
				+ "}"
				+ "	GROUP BY ?movie ?movietitle ?releaseyear \n"
				+ " ORDER BY DESC(?releaseyear) \n"
				+ "	LIMIT 10";
		//System.out.println(queryString);
		List<Resource> suggestedMovies = new ArrayList<>();
        Query query = QueryFactory.create(queryString) ;
        ResultSet results = null;
        
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://moviefy.ddns.net:3030/imdb/sparql", query)){         	results = qexec.execSelect();
        	ResultSetFormatter.out(System.out,results,query);
        	while (results.hasNext()) {
        		suggestedMovies.add(results.next().get("movie").asResource());
        	}
        }
        return suggestedMovies;
	}
	

	public static void main(String[] args) throws IOException {
		//Set debug level
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		
		Model m = buildViewingModel("Daniel Medina");
		//m.write(System.out,"Turtle");
		
		List<Resource> g = queryTopGenres(m);
		
		g.forEach((genre)->{
			//System.out.println(genre.getURI());
		});
		
		List<Resource> a = queryTopActors(m);
		a.forEach((actor)->{
			//System.out.println(actor.getURI());
		});
		
		List<Resource> d = queryTopDirectors(m);
		d.forEach((director)->{
			//System.out.println(director.getURI());
		});
		
		List<Resource> outMovies = queryMoviesByGenres(m,g,a);
		outMovies.forEach((outMovie)->{
			System.out.println(outMovie.getURI());
		});
	}
	

}
