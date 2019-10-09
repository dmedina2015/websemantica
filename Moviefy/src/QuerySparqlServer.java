import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;

public class QuerySparqlServer {
	
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
	
	public static List<String> genresFromMovie(String movieName){
		String queryString = 
				"PREFIX : <http://www.movieontology.org/2009/11/09/movieontology.owl#>\n" + 
				"PREFIX mo: <http://www.movieontology.org/2009/10/01/movieontology.owl#>\n" +  
				"SELECT ?movie ?genre \n" + 
				"WHERE {\n" + 
				"  ?movie mo:title \"" + movieName + "\".\n" +
				"  ?movie mo:belongsToGenre ?genre. \n" +
				"}";
        Query query = QueryFactory.create(queryString) ;
        ResultSet results = null;
        List<String> genres = new ArrayList<>();
        
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://moviefy.ddns.net:3030/imdb/sparql", query)){        
        	results = qexec.execSelect();
        	if (!results.hasNext()) return null;
        	
        	while (results.hasNext()) {
            	genres.add(results.next().get("genre").asResource().getURI());
            }
        	qexec.close();
        }
        
        
		return genres;
	}
	
	public static HashMap<String,Integer> prefGenres(List<String> movies){
		HashMap<String,Integer> genresCounting = new HashMap<String, Integer>();
		movies.forEach((title) -> {
			List<String> titleGenres = genresFromMovie(title);
			if (titleGenres!=null) {
				titleGenres.forEach((genre) -> {
					//System.out.println(title + " // " + genre);
					if(genresCounting.get(genre)!=null)
						genresCounting.put(genre,genresCounting.get(genre)+1);
					else
						genresCounting.put(genre,1);
				});
			}
		});
		return genresCounting;
	}

	public static void main(String[] args) throws IOException {
		List<String> filmes = loadViewedMovies("/Users/daniel/Downloads/NetflixViewingHistory.csv");
		System.out.println(filmes.toString());
		
		HashMap<String,Integer> generos = prefGenres(filmes);
		System.out.println(generos.toString());
		System.out.println("TERMINOU");
		
		
		
	}

}
