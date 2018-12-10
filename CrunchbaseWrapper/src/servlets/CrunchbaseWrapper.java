package servlets;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import utilities.Authentication;
import utilities.JSONLDHelper;
import utilities.MappingUtility;
import utilities.SPARQLLoader;

/**
 * Servlet implementation class CrunchbaseWrapper
 */
@WebServlet("/api/*")
public class CrunchbaseWrapper extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final boolean SPARQLendpoint = false; // introduced 2018
	private static final String CB_API_BASE_URI = "https://api.crunchbase.com/v3.1";

	// local testing
//private static final String PUBLIC_URL = "http://localhost:8081/CrunchbaseWrapper/api/";
//private static final String PUBLIC_CONTEXT = "http://localhost:8081/CrunchbaseWrapper/context.jsonld";
	// productive use
//	private static final String PUBLIC_URL = "http://corse.informatik.uni-freiburg.de:8080/CrunchbaseWrapper/api/";
//	private static final String PUBLIC_CONTEXT = "http://corse.informatik.uni-freiburg.de:8080/CrunchbaseWrapper/context.jsonld";
	private static final String PUBLIC_URL = "http://linked-crunchbase.org/api/";
	private static final String PUBLIC_CONTEXT = "http://linked-crunchbase.org/context.jsonld";

	private OkHttpClient client;
	private JSONLDHelper jsonldHelper;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CrunchbaseWrapper() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String endpoint = request.getRequestURI().replaceFirst(request.getContextPath(), ""); // extract api endpoint
																								// from
																								// query
		endpoint = endpoint.substring(4, endpoint.length()); // remove "/api"

		// determine accept header
		String requestAccept = request.getHeader("Accept");
		if (requestAccept == null) {
			// set default here
			requestAccept = "application/ld+json";
		}

		MappingUtility mu = (MappingUtility) getServletContext().getAttribute(Listener.MAPPING);

		if (request.getHeader("Authorization") == null) {
			if (SPARQLendpoint) {
				// load sparql stuff
				if (requestAccept.contains("json")) {
					response.getWriter().println(SPARQLLoader.loadTriple(endpoint, mu, "JSON-LD"));
					response.setCharacterEncoding("UTF-8");
					response.setContentType("application/ld+json");
				} else {
					response.getWriter().println(SPARQLLoader.loadTriple(endpoint, mu, "N-TRIPLES"));
					response.setCharacterEncoding("UTF-8");
					response.setContentType("text/turtle");
				}
			} else {
				/** use no SPARQL endpoint: **/
				if (requestAccept.contains("json")) {
//					response.getWriter().println(SPARQLLoader.loadTriple(endpoint, mu, "JSON-LD"));
					response.setCharacterEncoding("UTF-8");
					response.setContentType("application/ld+json");
					response.getWriter().println(addSameAsMappingPredicate(endpoint, mu, "JSON-LD"));
				} else {
//					response.getWriter().println(SPARQLLoader.loadTriple(endpoint, mu, "N-TRIPLES"));
					response.setCharacterEncoding("UTF-8");
					response.setContentType("text/turtle");
					response.getWriter().println(addSameAsMappingPredicate(endpoint, mu, "TTL"));
				}
			}
		} else {
			String apikey = Authentication.getAPIKey(request);

			jsonldHelper = new JSONLDHelper(PUBLIC_CONTEXT, CB_API_BASE_URI, PUBLIC_URL, mu);
			client = new OkHttpClient();
			client.setConnectTimeout(30, TimeUnit.SECONDS); // maybe increase,
															// crunchbase isnt
															// always fast
			String query = endpoint;
			if (request.getQueryString() != null) {
				query += "?" + request.getQueryString();
			}

			HttpUrl url = HttpUrl.parse(CB_API_BASE_URI + query);

			url = url.newBuilder().addQueryParameter("user_key", apikey).build();
			// finally build api request for crunchbase
			Request apiRequest = new Request.Builder().url(url).build();
			// get api response
			Response apiResponse = client.newCall(apiRequest).execute();

			// set response header of this servlet
			response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
			response.setStatus(apiResponse.code());
			response.setCharacterEncoding("UTF-8");

			String result = apiResponse.body().string();
			// System.out.println(result);

			if (apiResponse.code() == 200) { // errorhandling
				// content negotiation
				if (requestAccept.contains("text/turtle") || requestAccept.contains("application/x-turtle")) {
					// do rdf
					response.setContentType("text/turtle");
					response.getWriter().write(jsonldHelper.json2rdf(result, endpoint));
					// System.out.println("DidRDF");
				} else if (requestAccept.contains("application/ld+json") || requestAccept.contains("*/*")) {
					// do jsonld
					response.setContentType("application/ld+json");
					response.getWriter().write(jsonldHelper.json2jsonld(result, endpoint));
					System.out.println("DidJSONLD");
				} else {
					// just display the json from crunchbase
					response.setContentType("application/json");
					response.getWriter().write(result);
				}
			} else if (apiResponse.code() == 401) {
				Authentication.requireAuthentication(request, response);
				return;
			} else {
				response.getWriter().write(result);
			}
		}
	}

	/**
	 * this function generates the baseURI for the RDF transformation from the given
	 * endpoint and public URL
	 * 
	 * @param endpoint
	 * @return
	 */
	private static String getBaseURI(String endpoint) {
		if (endpoint.endsWith("/")) {
			endpoint = endpoint.substring(0, endpoint.lastIndexOf("/"));
		}
		return PUBLIC_URL + "api" + endpoint.substring(0, endpoint.lastIndexOf("/") + 1);
	}

	/**
	 * Takes the given permalink, processes it a bit, checks whether it's an
	 * organization or a person, then grabs the appropriate mapping for it and
	 * outputs it with a SAMEAS predicate into a model which is transformed into the
	 * wanted form of type and returned as such in string representation
	 * 
	 * @param permalink link to be processed
	 * @param mu        MappingUtility object used for the organization/people
	 *                  mapping
	 * @param type      what kind of output type is chosen (e.g. "RDF/XML",
	 *                  "RDF/XML-ABBREV", "N-TRIPLE", "TURTLE", (and "TTL") or "N3")
	 * @return String representation of <?permalink> <sameAs> <?mappedObject>
	 *         triple
	 */
	public static String addSameAsMappingPredicate(String permalink, MappingUtility mu, String type) {
		final Model model = ModelFactory.createDefaultModel();
		String identifier = permalink.substring(permalink.lastIndexOf("/") + 1) + "#id"; // sth like "facebook#id"
		String uri = SPARQLLoader.CBWPREFIX + permalink + "#id";

		String mapping = null;
		if (identifier.startsWith("/organization")) {
			mapping = mu.getOrganizationMapping(identifier);
		} else if (identifier.startsWith("/people")) {
			mapping = mu.getPersonMapping(identifier);
		}

		// Add a SAMEAS triple
		final Resource cbEntity = model.createResource(uri);
		final Resource cbDoc = model.createResource(mapping);
		cbEntity.addProperty(SPARQLLoader.sameAs, cbDoc);
		StringWriter out = new StringWriter();
		model.write(out, type);
		return out.toString();

	}

}
