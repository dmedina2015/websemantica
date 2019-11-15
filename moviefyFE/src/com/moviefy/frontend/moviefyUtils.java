package com.moviefy.frontend;

import java.io.IOException;

import java.net.URLEncoder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class moviefyUtils {
	
	public static String getThumbnail(String movieName, int year) throws IOException {
	    
		String url = "https://www.bing.com/images/search?scope=images&q=" + URLEncoder.encode(movieName,"UTF-8") + "+" + year + "+poster&qft=+filterui:aspect-tall&FORM=IRFLTR";
		
		Document doc = Jsoup.connect(url).get();
		Elements thumbs = doc.select(".mimg");
		int i=0;
		while (thumbs.size()>i)
		{
			String thumb_src = thumbs.get(i).attr("src");
			String thumb_data_src = thumbs.get(i).attr("data-src");

			if(thumb_src.contains("https://"))
				return thumb_src;
			else if(thumb_data_src.contains("https://"))
				return thumb_data_src;
			else
				i++;
		}
		return null;
	}
}
