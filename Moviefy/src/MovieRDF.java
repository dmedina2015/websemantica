import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import org.apache.jena.ontology.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;



public class MovieRDF {
	
	public static final String ontologyFile="movieontology.owl";
	public static final String importFile = "c:\\Users\\dmedina\\Downloads\\title.basics.tsv\\new.txt";
	//public static final String importFile = "/Users/daniel/Downloads/new.tsv";

	/* Method for loading Ontology Model*/
	public static OntModel loadOntology(String path) {
		System.out.print("Loading ontology...");
		OntModel ont = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		ont.read(path);
		System.out.println("OK");
		return ont;
	}
	
	
    	
	public static void main(String[] args) throws IOException {
		//Set debug level
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		
		//Load Movie Ontology into a OntModel
		OntModel ontologyModel= loadOntology("movieontology.owl");
		
		//Load NameSpaces from ontology
		String owlNS= OWL.getURI();
		String dcNS=DC_11.getURI();
		String xsdNS=XSD.getURI();
		String owl2NS=OWL2.getURI();
		String rdfNS = RDF.getURI();
		String rdfsNS=RDFS.getURI();
		String dctermsNS=DCTerms.getURI();
		String dboNS = ontologyModel.getNsPrefixURI("ontology");
		String wwwNS= ontologyModel.getNsPrefixURI("www");
		String movieontologyNS = ontologyModel.getNsPrefixURI("movieontology");
		String movieontology2NS = ontologyModel.getNsPrefixURI("movieontology2");
		String imdbNS = "http://imdb.com/"; 
		
		//Create model to store movie collection
		Model moviesModel=ModelFactory.createDefaultModel();
		
		//Set NameSpaces into the new model
		moviesModel.setNsPrefix("rdf", rdfNS);
		moviesModel.setNsPrefix("xsd", xsdNS);
		moviesModel.setNsPrefix("dcterms", dctermsNS);
		moviesModel.setNsPrefix("www", wwwNS);
		moviesModel.setNsPrefix("movieontology",movieontologyNS);
		moviesModel.setNsPrefix("movieontology2", movieontology2NS);
		moviesModel.setNsPrefix("imdb",imdbNS); // Create arbitrary NS for IMDB
		
		//Insert Movies	
		System.out.print("Importing movies...");
		String line;
		long i=0;
		// Load classes and properties from ontology model to link to individual movies
		OntClass classMovie = ontologyModel.getOntClass(wwwNS+"Movie");
		OntProperty propRuntime = ontologyModel.getOntProperty(movieontologyNS+"runtime");
		OntProperty propTitle=ontologyModel.getOntProperty(movieontologyNS + "title");
		OntProperty propReleaseDate = ontologyModel.getOntProperty(movieontologyNS+"releasedate");
		OntProperty propGenre = ontologyModel.getOntProperty(movieontologyNS+"belongsToGenre");
		
	//	try (BufferedReader br = new BufferedReader(new FileReader(importFile))) { 
		try (BufferedReader br = new BufferedReader(new InputStreamReader (new FileInputStream(importFile),"UTF-8"))) {  
			//For each line, adds a movie and its properties
			while (((line = br.readLine()) != null)&&(i<=3)) { 
				i++;
				String[] movieProps = line.split("\t"); //Splits line in properties values
				
				//Create individual movie and adds it and its properties into the model (movies collection)
				Resource individualMovie = moviesModel.createResource(imdbNS+movieProps[0]);
				
				//Set RDF:Type to a www:Movie
				individualMovie.addProperty(RDF.type, classMovie);
				
				//Set Title
				individualMovie.addProperty(propTitle,moviesModel.createTypedLiteral(new String(movieProps[2])))
					.addProperty(DCTerms.title,moviesModel.createTypedLiteral(new String(movieProps[2])));
				if (!movieProps[7].contentEquals("\\N"))
					individualMovie.addProperty(propRuntime,moviesModel.createTypedLiteral(new Integer(movieProps[7])));
					String s = individualMovie.getProperty(propTitle).toString();
				
				// Set release date (imdb only provides year)
				if (!movieProps[5].contentEquals("\\N")) {
					Calendar calendar = Calendar.getInstance();
					calendar.set(new Integer(movieProps[5]), 0, 1,0,0,0); // IMDB only provides year, so all dates are 01/Jan 00h00m00s
					individualMovie.addProperty(propReleaseDate,moviesModel.createTypedLiteral(calendar));
					}
				
				//Set Genres
				if(!movieProps[8].contentEquals("\\N")) {
					String[] movieGenres = movieProps[8].split(",");
					for (String genre: movieGenres) {
						Resource ontGenre = ontologyModel.getIndividual(movieontologyNS+genre);
						individualMovie.addProperty(propGenre, ontGenre);
					}
				}
		    }
			System.out.println("OK");
			System.out.println("Movies imported: " + i);
		}
		
		 /*/Export movie collection in RDF
		System.out.print("Exporting RDF...");
		BufferedWriter writer = new BufferedWriter(new FileWriter("c:\\Users\\dmedina\\Downloads\\outputRDF.rdf"));
		moviesModel.write(writer,"RDF/Xml");
		writer.close();
		System.out.println("OK"); //*/
		
		
		/*/Queries
		System.out.print("Starting query...");
		String NL = System.getProperty("line.separator");
		String prefix1 = "PREFIX movieontology: <" + movieontologyNS + ">" + NL;
		String prefix2 = "PREFIX www: <" + wwwNS + ">" +NL;
		String prefix3 = "PREFIX imdb: <" + imdbNS + ">" +NL;
		String prolog = prefix1 + prefix2 + prefix3;
		String queryString = prolog + 
				"SELECT ?movie ?title WHERE {" + NL
				+ "?movie a www:Movie." + NL
				+ "?movie movieontology:belongsToGenre movieontology:Documentary." + NL
				+ "?movie movieontology:title ?title." + NL
				+ "}";
						
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query,moviesModel)){
			ResultSet rs=qexec.execSelect();
			System.out.println("OK");
			System.out.println("Results: ");
			ResultSetFormatter.out(System.out,rs,query);
		} //*/
		moviesModel.write(System.out,"Turtle");
	}

}