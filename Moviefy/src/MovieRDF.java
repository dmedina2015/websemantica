import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

public class MovieRDF {

	public static Model loadOntology(String path) {
		System.out.print("Loading ontology...");
		Model ont = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		ont.read(path);
		System.out.println("OK");
		return ont;
	}
	
	public static boolean insertMovie(OntModel movieOnt, OntClass movieClass, String resourceURI) {
		Individual movie = movieOnt.createIndividual(resourceURI,movieClass);
		if (movie==null) return false;
		return true;
		
	}
	
	public static boolean insertMovies(OntModel movieOnt) throws IOException {
		//Load namespaces (prefixes URIs)
		String moNS = movieOnt.getNsPrefixURI("www");
		String imdbNS = movieOnt.getNsPrefixURI("imdb");
		String movieontologyNS = movieOnt.getNsPrefixURI("movieontology");
		
		//Load movie class from Ontology
		OntClass movieClass = movieOnt.getOntClass(moNS + "Movie");
		OntProperty runtimeProp = movieOnt.getOntProperty(movieontologyNS+"runtime");
			
		//Create individual movie
		//movie.addProperty(runtimeProp, "100^^xsd:int");
		
		System.out.print("Importing movies...");
		
		Path path = Paths.get("/Users/daniel/Downloads/new.tsv");
			long lineCount = Files.lines(path).count();
			System.out.println("Total lines = " + lineCount);
			System.out.print(".........."+'\r');
		 String line;
		 long i=0;
			
    	try (BufferedReader br = new BufferedReader(new FileReader("/Users/daniel/Downloads/new.tsv"))) {
    	    while ((line = br.readLine()) != null) {
    	       i++;// process the line.
    	        if (i%100000==0) {
    	        	for (int j=0;j<(i*10/lineCount);j++) {
    	        		System.out.print("*");
    	        	}
    	        	System.out.print('\r');
    	        }
    	    	if (insertMovie(movieOnt,movieClass,imdbNS+line.substring(0,10))==false) System.out.println("PROBLEMA NA LINHA " + i);
    	    }
    	System.out.println("OK / i = " + i);
    	System.out.println("----------------------");
    	System.out.println("Movies imported: "+ movieClass.listInstances().toList().size());
		
    	/*List lista = movieClass.listInstances().toList();
        
        BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/daniel/Downloads/outputRDF.txt"));
        
    	for (int j=0;j<lista.size();j++) {
    		//System.out.println(lista.get(j));
    		writer.write((lista.get(j)).toString().substring(16)+"\n");
    	}
    	
        writer.close();*/
		
		
		
    	return true;
    	}
	}
    	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		Model myModel=loadOntology("movieontology.owl");
		myModel.setNsPrefix("imdb", "http://imdb.org/");
		insertMovies((OntModel) myModel);
		//myModel.write(System.out,"Turtle");
	}

}