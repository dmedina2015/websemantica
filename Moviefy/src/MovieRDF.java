import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Map;

import org.apache.jena.ontology.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;



public class MovieRDF {
	
	//Create constants for data files
	public static final String ontologyFile="movieontology.owl";
	public static final String movieImportFile= "moviesIMDB.tsv";
	public static final String ratingImportFile = "ratingsIMDB.tsv";
	public static final String peopleImportFile = "namesIMDB.tsv";
	public static final String principalsImportFile = "principalsIMDB.tsv";
	public static final String akasImportFile = "akas(US+BR)IMDB.tsv";
	
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
		String foafNS = FOAF.getURI();
		String dbpNS = "http://dbpedia.org/page/";
		ontologyModel.setNsPrefix("dbp", dbpNS);
		
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
		moviesModel.setNsPrefix("foaf", foafNS);
		moviesModel.setNsPrefix("dbo",dboNS);
		moviesModel.setNsPrefix("dbp", dbpNS);
		
		//Insert Movies	
		System.out.print("Importing movies...");
		
		// Load classes and properties from ontology model to link to individual movies
		OntClass classMovie = ontologyModel.getOntClass(wwwNS+"Movie");
		OntProperty propRuntime = ontologyModel.getOntProperty(movieontologyNS+"runtime");
		OntProperty propTitle=ontologyModel.getOntProperty(movieontologyNS + "title");
		OntProperty propReleaseDate = ontologyModel.getOntProperty(movieontologyNS+"releasedate");
		OntProperty propGenre = ontologyModel.getOntProperty(movieontologyNS+"belongsToGenre");
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader (MovieRDF.class.getResourceAsStream(movieImportFile)))) {  
			//For each line, adds a movie and its properties
			String line;
			long i=0;
			while ((line = br.readLine()) != null) { 
				i++;
				String[] movieProps = line.split("\t"); //Splits line in properties values
				
				//Create individual movie and adds it and its properties into the model (movies collection)
				Resource individualMovie = moviesModel.createResource(imdbNS+movieProps[0]);
				
				//Set RDF:Type to a www:Movie
				individualMovie.addProperty(RDF.type, classMovie);
				
				//Set Title
				individualMovie.addProperty(propTitle,moviesModel.createTypedLiteral(new String(movieProps[2])));
				//	.addProperty(DCTerms.title,moviesModel.createTypedLiteral(new String(movieProps[2])));
				if (!movieProps[7].contentEquals("\\N"))
					individualMovie.addProperty(propRuntime,moviesModel.createTypedLiteral(new Integer(movieProps[7])));
					String s = individualMovie.getProperty(propTitle).toString();
				
				// Set release date (imdb only provides year)
				if (!movieProps[5].contentEquals("\\N")) {
					Calendar calendar = Calendar.getInstance();
					calendar.clear();
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
			System.out.println("OK \t Movies imported: \t" + i);
		}
		
		//Populate AKAs
		System.out.print("Importing AKAs...");
		try (BufferedReader br = new BufferedReader(new InputStreamReader (MovieRDF.class.getResourceAsStream(akasImportFile)))) {  
			//Vars declaration
			String line;
			long i=0;
			
			//Load 'Title' property from ontology
			//OntProperty propTitle = ontologyModel.getOntProperty(movieontologyNS+"title");
			
			//For each line, adds region-specific title
			while ((line = br.readLine()) != null) {
				i++;
				String[] akaProps = line.split("\t"); //Splits line in properties values
				
				//Load individual movie from model to receive AKA
				Resource individualMovie = moviesModel.getResource(imdbNS+akaProps[0]);
				individualMovie.addProperty(propTitle, ResourceFactory.createLangLiteral(akaProps[1], akaProps[2]));		
			}
			System.out.println("OK \t AKAs imported: \t" + i);
		}
		
		//Populate ratings
		System.out.print("Importing ratings...");
		try (BufferedReader br = new BufferedReader(new InputStreamReader (MovieRDF.class.getResourceAsStream(ratingImportFile)))) {  
			//Vars declaration
			String line;
			long i=0;
			
			//Load 'IMDB Rating' property from ontology
			OntProperty propRating = ontologyModel.getOntProperty(movieontologyNS+"imdbrating");
			
			//For each line, adds IMDB rating for related movie
			while ((line = br.readLine()) != null) {
				i++;
				String[] ratingProps = line.split("\t"); //Splits line in properties values
				
				//Load individual movie from model to receive rating
				Resource individualMovie = moviesModel.getResource(imdbNS+ratingProps[0]);
				individualMovie.addProperty(propRating, moviesModel.createTypedLiteral(new Double(ratingProps[1])));		
			}
			System.out.println("OK \t Ratings imported: \t" + i);
		}
		
		//Populate people (actors and directors) into the model
		System.out.print("Importing people...");
		try (BufferedReader br = new BufferedReader(new InputStreamReader (MovieRDF.class.getResourceAsStream(peopleImportFile)))) {  
			//Vars declaration
			String line;
			long i=0;
			
			//Create and load classes from/to ontologyModel
			OntClass classDBOPerson = ontologyModel.getOntClass(dboNS+"Person");
			
			//Create and load properties
			OntProperty propBirthName = ontologyModel.createOntProperty(dboNS+"birthName");
			OntProperty propBirthYear = ontologyModel.createOntProperty(dboNS+"birthYear");
			OntProperty propDeathYear = ontologyModel.createOntProperty(dboNS+"deathYear");
			
			//For each line, adds a Person in the model
			while ((line = br.readLine()) != null) {
				i++;
				String[] peopleProps = line.split("\t"); //Splits line in properties values
				Resource person = moviesModel.createResource(imdbNS+peopleProps[0], FOAF.Person)
					.addProperty(RDF.type, classDBOPerson)
					.addProperty(propBirthName, peopleProps[1])
					.addProperty(FOAF.name, peopleProps[1]);
				
				if (!peopleProps[2].contentEquals("\\N")) {
					person.addProperty(propBirthYear, moviesModel.createTypedLiteral(new Integer (peopleProps[2])));
				}
				if (!peopleProps[3].contentEquals("\\N")) {
					person.addProperty(propDeathYear, moviesModel.createTypedLiteral(new Integer (peopleProps[3])));
				}
			}
			System.out.println("OK \t People imported: \t" + i);
		}
			
		
		//Link people (actors and directors) to existing movies
		System.out.print("Linking people to movies...");
		try (BufferedReader br = new BufferedReader(new InputStreamReader (MovieRDF.class.getResourceAsStream(principalsImportFile)))) {  
			//Vars declaration
			String line;
			long i=0;
			
			//Create and load classes from/to ontologyModel
			OntClass classActor = ontologyModel.getOntClass(dboNS+"Actor");
			OntClass classFilmDirector = ontologyModel.createClass(dbpNS+"Film_Director");
			
			//Create and load properties
			OntProperty propIsActorIn = ontologyModel.getOntProperty(movieontologyNS+"isActorIn");
			OntProperty propHasActor = ontologyModel.getOntProperty(movieontologyNS+"hasActor");
			OntProperty propIsDirectorOf = ontologyModel.getOntProperty(movieontologyNS+"isDirectorOf");
			OntProperty propHasDirector = ontologyModel.getOntProperty(movieontologyNS+"hasDirector");
			
			//Variable outside loop
			String [] principalsProps;
			Resource movie;
			Resource person;
		
			//For each line, link a Person to a Movie
			while ((line = br.readLine()) != null) {
				i++;
				principalsProps = line.split("\t"); //Splits line in properties values
				movie = moviesModel.getResource(imdbNS+principalsProps[0]);
				person = moviesModel.getResource(imdbNS+principalsProps[2]);
				if (principalsProps[3].equals("director")){
					movie.addProperty(propHasDirector, person);
					person.addProperty(propIsDirectorOf, movie);
				}
				else {
					movie.addProperty(propHasActor, person);
					person.addProperty(propIsActorIn, movie);
				}
				//if (i%1000==0)System.out.println(i);
			}
			System.out.println("OK \t Links imported: \t" + i);
		}
		
		//Export movie collection in RDF
		System.out.print("Exporting RDF...");
	//	BufferedWriter writer = new BufferedWriter(new FileWriter("c:\\Users\\dmedina\\Downloads\\outputIMDB.rdf"));
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/daniel/Downloads/outputIMDB.ttl"));
		moviesModel.write(writer,"Turtle");
		writer.close();
		System.out.println("OK"); //*/
		
		
		//Queries
		System.out.print("Starting query...");
		String NL = System.getProperty("line.separator");
		String prefix1 = "PREFIX movieontology: <" + movieontologyNS + ">" + NL;
		String prefix2 = "PREFIX www: <" + wwwNS + ">" +NL;
		String prefix3 = "PREFIX imdb: <" + imdbNS + ">" +NL;
		String prefix4 = "PREFIX foaf: <" + foafNS + ">" +NL;
		String prefix5 = "PREFIX xsd: <" + xsdNS + ">" + NL;
		String prolog = prefix1 + prefix2 + prefix3 + prefix4 + prefix5;
		String queryString = prolog + 
				"SELECT ?movie ?title ?runtime WHERE {" + NL
				+ "?movie a www:Movie." + NL
				//+ "?movie movieontology:belongsToGenre movieontology:Comedy." + NL
				//+ "?movie movieontology:title ?title." + NL
				+ "?movie movieontology:hasActor ?actor1 ." + NL
				+ "?actor1 foaf:name \"Laura Dern\" ." + NL
				+ "?movie movieontology:hasDirector ?director ." + NL
				+ "?director foaf:name \"Steven Spielberg\" ." + NL
				+ "?movie movieontology:title ?title ." + NL
				+ "?movie movieontology:runtime ?runtime ." + NL
			//	+ "?movie movieontology:belongsToGenre ?genre ." + NL
				+ "}";
						
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query,moviesModel)){
			ResultSet rs=qexec.execSelect();
			System.out.println("OK");
			System.out.println("Results: ");
			ResultSetFormatter.out(System.out,rs,query);
		} //*/
		//moviesModel.write(System.out,"Turtle");
	}

}