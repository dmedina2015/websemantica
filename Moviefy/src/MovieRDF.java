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
	public static boolean insertMovie(String URI,OntModel movieOnt){
		String moNS = movieOnt.getNsPrefixURI("www");
		String imdbNS = movieOnt.getNsPrefixURI("imdb");
		OntClass movieClass = movieOnt.getOntClass(moNS + "Movie");
		Individual movie = movieClass.createIndividual(imdbNS+URI);
		movieOnt.write(System.out);
		return true;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		Model myModel=loadOntology("movieontology.owl");
		myModel.setNsPrefix("imdb", "http://imdb.org/");
		System.out.println(myModel.getNsPrefixURI("www"));
		//myModel.write(System.out);
		insertMovie("Matrix",(OntModel) myModel);
	}

}