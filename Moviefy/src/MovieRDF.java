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
	public static boolean insertMovie(String movieID,OntModel movieOnt){
		//Load namespaces (prefixes URIs)
		String moNS = movieOnt.getNsPrefixURI("www");
		String imdbNS = movieOnt.getNsPrefixURI("imdb");
		String movieontologyNS = movieOnt.getNsPrefixURI("movieontology");
		
		//Load movie class from Ontology
		OntClass movieClass = movieOnt.getOntClass(moNS + "Movie");
		OntProperty runtimeProp = movieOnt.getOntProperty(movieontologyNS+"runtime");
		
		//Create individual movie
		Individual movie = movieOnt.createIndividual(imdbNS + movieID,movieClass);
		movie.addProperty(runtimeProp, "100^^xsd:int");
		return true;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		Model myModel=loadOntology("movieontology.owl");
		myModel.setNsPrefix("imdb", "http://imdb.org/");
		insertMovie("Matrix",(OntModel) myModel);
		myModel.write(System.out,"Turtle");
	}

}