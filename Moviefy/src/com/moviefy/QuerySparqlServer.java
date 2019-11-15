package com.moviefy;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resources;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
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
	
	
	public static List<String> loadViewedTitles(String path) throws IOException{
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
				"  ?movie mo:releasedate ?date. \n" +
				"  BIND (YEAR(?date) as ?year). \n" +
				"}\n" +
				"ORDER BY DESC(?year) \n" +
				"LIMIT 1";
		queryString = 
				"PREFIX : <http://www.movieontology.org/2009/11/09/movieontology.owl#>\n" + 
				"PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#>\n" +  
				"SELECT ?movie \n" + 
				"WHERE {\n" + 
				"  VALUES ?titles { \"" + movieName + "\" \"" + movieName + "\"@EN \"" + movieName + "\"@BR } \n" +
				"  ?movie mo:title ?titles.\n" +
				"  ?movie mo:releasedate ?date. \n" +
				"  BIND (YEAR(?date) as ?year). \n" +
				"}\n" +
				"ORDER BY DESC(?year) \n" +
				"LIMIT 1";
        Query query = QueryFactory.create(queryString) ;
        ResultSet results = null;
        
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://moviefy.ddns.net:3030/imdb/sparql", query)){        
        	results = qexec.execSelect();
        	if (!results.hasNext()) return null;
        	else return results.next().get("movie").asResource();
        }
	}
	
	public static Model buildViewingModel(String userName, List<String> viewedTitles) throws IOException {
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
		
		viewedTitles.forEach((movieTitle) -> {
			Resource movie = movieFromTitle(movieTitle,"");
			//if (movie==null) movie = movieFromTitle(movieTitle,"@BR"); //If doesnt find original title, try portuguese.
			//if (movie==null) movie = movieFromTitle(movieTitle,"@US"); //If doesnt find portuguese, try english.
			if (movie!=null) user.addProperty(propView, movie);
		});
		return m;
	}
	
	
	public static List<String> queryViewedMovies(Model m){
		String queryString = 
				 "PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#> \n"
				+ "PREFIX mvfyOnt: <http://moviefy.com/ontology/> \n"
				+ "PREFIX mvfyRes: <http://movify.com/resources/> \n"
				+ "PREFIX imdb: <http://imdb.com/> \n"
				+ "SELECT ?movie\n"
				+ "	WHERE {\n" 
				+ "		?someone mvfyOnt:viewed ?movie . \n"
				+ " }\n";
				
		List<String> moviesViewed = new ArrayList<>();
        Query query = QueryFactory.create(queryString) ;
        ResultSet results = null;
        
        try (QueryExecution qexec = QueryExecutionFactory.create(query,m)){        
        	results = qexec.execSelect();
        	//ResultSetFormatter.out(System.out,results,query);
        	while (results.hasNext()) {
        		moviesViewed.add(results.next().get("movie").toString().replaceAll("http://imdb.com/", "imdb:"));
        	}
        }
        return moviesViewed;
	}
	
	public static List<Resource> queryTopGenres(Model m, int i){
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
				+ "	LIMIT " + i;
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
	
	public static List<Resource> queryTopActors(Model m, int i){
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
				+ "	LIMIT " + i;
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
	
	public static List<Resource> queryTopDirectors(Model m, int i){
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
				+ "	LIMIT " + i;
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
	
	public static LinkedHashMap<String,List<String>> queryMoviesByPreferences(List<Resource> topGenres, List<Resource> topActors, List <Resource> topDirectors, int minorYear){
		//Define Top Genres Strings
		AtomicReference<String> genres = new AtomicReference<>("");
		AtomicReference<String> actors = new AtomicReference<>("");
		AtomicReference<String> directors = new AtomicReference<>("");
		
		topGenres.forEach(genre -> {
			String oldValue = genres.get();
			genres.set(oldValue+"mo:"+genre.getLocalName() + " ");
		});
		
		topActors.forEach(actor -> {
			String oldValue = actors.get();
			actors.set(oldValue+"imdb:"+actor.getLocalName() + " ");
		});
		
		topDirectors.forEach(director -> {
			String oldValue = directors.get();
			directors.set(oldValue+"imdb:"+director.getLocalName() + " ");
		});
		
		
		//Define Query Preambul
		String queryPreambule =
		
				  "PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#> \n"
				+ "PREFIX mvfyOnt: <http://moviefy.com/ontology/> \n"
				+ "PREFIX mvfyRes: <http://movify.com/resources/> \n"
				+ "PREFIX imdb: <http://imdb.com/> \n";
		
		String querySelect =
			  "SELECT DISTINCT ?movie ?titleStr ?year ?rating ?runtime ?thumbnail WHERE{ \n"
			+ "	{ \n"
			+ "		SELECT ?movie WHERE{\n"
			+ "			?movie mo:belongsToGenre ?genre. \n"
			+ "			VALUES ?genre { " + genres + " } \n"
			+ "			?movie mo:hasActor ?actor. \n"
			+ "			VALUES ?actor { " + actors + " } \n"
			+ "		} \n"
			+ "	} \n"
			+ "	UNION \n"
			+ "	{ \n"
			+ "		SELECT ?movie WHERE{ \n"
			+ "			?movie mo:belongsToGenre ?genre. \n"
			+ "			VALUES ?genre { " + genres + " } \n"
			+ "			?movie mo:hasDirector ?director. \n"
			+ "			VALUES ?director { " + directors + " } \n"
			+ "		} \n"
			+ "	} \n"
			+ " OPTIONAL {?movie mo:hasThumbnail ?thumbnail.} \n"
			+ "	?movie mo:imdbrating ?rating. \n"
			+ " ?movie mo:runtime ?runtime. \n"
			+ "	?movie mo:releasedate ?date. \n"
			+ "	?movie mo:title ?title. \n"
			+ " BIND(YEAR(?date) as ?year).\n"
			+ " BIND(STR(?title) as ?titleStr).\n"
			+ "	FILTER (LANG(?title)=\"BR\") \n"
			+ "	FILTER (?year>" + minorYear + "). \n"
			+ "	} \n"
			+ "	ORDER BY DESC(?rating) \n";
			
		//System.out.println(queryString);
		LinkedHashMap <String,List<String>> suggestedMovies = new LinkedHashMap<String,List<String>>();
        Query query = QueryFactory.create(queryPreambule+querySelect) ;
        ResultSet results = null;
        
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://moviefy.ddns.net:3030/imdb/sparql", query)){         	
        	results = qexec.execSelect();
        	//ResultSetFormatter.out(System.out,results,query);
        	while (results.hasNext()) {
        		QuerySolution nextSolution = results.next();
        		List<String> movieParameters = new ArrayList<String>();
        		movieParameters.add(nextSolution.get("titleStr").toString());
        		movieParameters.add(nextSolution.get("year").toString().substring(0, 4));
        		movieParameters.add(nextSolution.get("runtime").toString().substring(0, nextSolution.get("runtime").toString().indexOf("^")));
        		movieParameters.add(nextSolution.get("rating").toString().substring(0, 3));
        		if(nextSolution.get("thumbnailBR")!=null) movieParameters.add(nextSolution.get("thumbnail").toString());
        		suggestedMovies.put(nextSolution.get("movie").toString().replaceAll("http://imdb.com/", "imdb:"),movieParameters);   
        	}
        }
        return suggestedMovies;
	}
	
	
	public static LinkedHashMap<String,List<String>> getSuggestions(String userName, List<String> viewedTitles, int maxGenres, int maxActors, int maxDirectors) throws IOException {
		Model m = buildViewingModel(userName,viewedTitles);
		List<String> moviesViewed = queryViewedMovies(m);
		List<Resource> g = queryTopGenres(m, maxGenres);
		List<Resource> a = queryTopActors(m, maxActors);
		List<Resource> d = queryTopDirectors(m, maxDirectors);
		LinkedHashMap<String,List<String>> outMovies = queryMoviesByPreferences(g,a,d, 2010);
	
		moviesViewed.forEach(viewed ->{
			outMovies.remove(viewed);
		});
		
		return outMovies;
	}

	public static void main(String[] args) throws IOException {
		//Set debug level
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		
		//Load viewed titles from CSV
		List<String> viewedTitles = loadViewedTitles("/Users/daniel/Downloads/NetflixViewingHistory.csv");

		//Call for suggestions
		LinkedHashMap<String,List<String>> suggestions = getSuggestions("Daniel Medina",viewedTitles,3,5,5);
		
		//Print Suggestions
		suggestions.forEach((movieKey,movieData) ->{
			System.out.println(movieKey+ " ==> "+ movieData);
		});
	}
	

}
