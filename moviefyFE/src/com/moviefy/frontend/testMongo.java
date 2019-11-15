package com.moviefy.frontend;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class testMongo {

	public static void main(String[] args) {
		final String uriString = "mongodb://localhost:8082";
	    MongoClient mongoClient = MongoClients.create(uriString);
	    MongoDatabase mongoDB = mongoClient.getDatabase("moviefyDB");
	    MongoCollection<Document> collection = mongoDB.getCollection("moviePosters");
	    
	    Document canvas = new Document("item", "canvas")
	            .append("qty", 100)
	            .append("tags", "cotton");

	    Document size = new Document("h", 28)
	            .append("w", 35.5)
	            .append("uom", "cm");
	    canvas.put("size", size);

	    collection.insertOne(canvas);
	}

}
