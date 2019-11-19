package com.moviefy.frontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import com.moviefy.QuerySparqlServer;
import com.moviefy.frontend.moviefyUtils;


/**
 * Servlet implementation class upload
 */


@WebServlet("/upload")
public class upload extends HttpServlet {
	
	
	
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public upload() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//doGet(request, response);
		
		response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        InputStream fis = null;
        List<String> movies = new ArrayList<>();
        
        try {


            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            
            if (isMultipart) {

                List<FileItem> items = new ServletFileUpload(
                        new DiskFileItemFactory()).parseRequest(request);
                for (FileItem item : items) {
                    if (!item.isFormField()) {
                        fis = item.getInputStream();

                        InputStreamReader isr = new InputStreamReader (fis);
                		try (BufferedReader br = new BufferedReader(isr)) {  
                			String line;
                			br.readLine(); // Drops header line
                			while ((line = br.readLine()) != null) { 
                				String movie = line.split("\",")[0].substring(1); //Get movie name
                				if (!movie.contains(": Season") && (!movie.contains(": Temporada"))) movies.add(movie);
                			}
                		}
                    }
                }
            }
            
            

        } catch (FileUploadException e) {
            throw new ServletException("Cannot parse multipart request.", e);
        } catch (Exception e) {
            throw new ServletException("", e);
        }
        finally {
        	
        }
        
        SimpleDateFormat formatter= new SimpleDateFormat("MM/dd/yyyy 'at' HH:mm:ss z");
        System.out.println("Starting Query on " + formatter.format(new Date(System.currentTimeMillis())));

        System.out.println(movies.size() + " movies found on Netflix file.");
        LinkedHashMap<String,List<String>> sugestoes = QuerySparqlServer.getSuggestions("Daniel Medina", movies, 3, 5, 3);
        System.out.println("Query finished on " + formatter.format(new Date(System.currentTimeMillis())));
        System.out.println("Starting fetching images on " + formatter.format(new Date(System.currentTimeMillis())));
        
        
        
        
        out.println("<div class=\"album py-5 bg-light\">");
        out.println("<div class=\"center\"> <b>As recomendações de filmes que temos para você estão mostradas abaixo. </b><br>Depois não deixe de nos contar o que achou delas respondendo a 3 perguntas, não demora nem um minuto. \n"
        		+ "  <br><button class=\"btn btn-primary\" type=\"button\" data-toggle=\"collapse\" data-target=\"#collapseExample\" aria-expanded=\"false\" aria-controls=\"collapseExample\">\n" + 
        		"    Responder\n" + 
        		"  </button>\n" +  
        		"<div class=\"collapse\" id=\"collapseExample\">\n" + 
        		"  <div class=\"card card-body\">\n" +
        		"<iframe src=\"https://docs.google.com/forms/d/e/1FAIpQLSfB10e1laPVVMmCi6ux5Rwib6ozJImuL2bwHDBHB9-UnLTEgg/viewform?embedded=true\" width=\"640\" height=\"921\" frameborder=\"0\" marginheight=\"0\" marginwidth=\"0\">Carregando…</iframe>" +
        		"  </div>\n" + 
        		"</div>"
        		+ "</div>");
        out.println("<div class=\"container\">");
        out.println("<div class=\"row\">");
        
        
        sugestoes.forEach((movie,movieProps) ->{
        	String img = null;
        	if (movieProps.size()==5) img=movieProps.get(4);
        	
        	if (img==null) {
				try {
					img = moviefyUtils.getThumbnail(movieProps.get(0),Integer.valueOf(movieProps.get(1)));
					if (img!=null)
						if (img!="null")
							QuerySparqlServer.insertThumbnailTriple(movie, img);
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	
        	out.println("<div class=\"col-md-3\">");
        	out.println("<div class=\"card mb-4 shadow-sm\">");
        	out.println("<img class=\"card-img-top\" src=\"" + img + "\" data-holder-rendered=\"true\" style=\"height: 148%; width: 100%; display: block;\">");
        	out.println("<div class=\"card-body\">");
        	out.println("<p class=\"card-text\">" + movieProps.get(0) + "<br>" + movieProps.get(1) + "<br>" + movieProps.get(3) + "</p>");
        	out.println("<div class=\"d-flex justify-content-between align-items-center\">");
        	out.println("<small class=\"text-muted\">" + movieProps.get(2) + " mins</small>");
        	out.println("</div>");
        	out.println("</div>");
        	out.println("</div>");
        	out.println("</div>");
        });
        
        System.out.println("Finished fetching images on " + formatter.format(new Date(System.currentTimeMillis())));
        
        
        out.println("</div>");
        out.println("</div>");
        out.println("</div>");
        out.flush();
	}

}
