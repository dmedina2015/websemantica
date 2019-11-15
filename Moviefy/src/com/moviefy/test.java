package com.moviefy;
import java.util.*;
import java.util.Map.*;

public class test {

	public static void main(String[] args){
		HashMap<String,Integer>map=new HashMap<String, Integer>();
        map.put("Ação", 50);
        map.put("Comédia", 60);
        map.put("Drama", 30);
        
        map.put("Comédia",40);
        
        int maxValueInMap=(Collections.max(map.values()));  // This will return max value in the Hashmap
        for (Entry<String, Integer> entry : map.entrySet()) {  // Itrate through hashmap
            if (entry.getValue()==maxValueInMap) {
                System.out.println(entry.getKey());     // Print the key with max value
            }
        }

}
}
