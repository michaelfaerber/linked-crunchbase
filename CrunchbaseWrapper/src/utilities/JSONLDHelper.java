package utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;

import org.apache.jena.atlas.json.io.parser.JSONParser;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.lang.CollectorStreamTriples;
import org.json.JSONObject;

import com.hp.hpl.jena.graph.Triple;

/**
 * This class is the core of our 
 * @author Carsten
 *
 */
public class JSONLDHelper {
	private static final String PREFIX = "cbw:"; // for fetching data
	private String context;
	private String baseURI;
	private String baseURIRDF;
	
	private MappingUtility mu;
	
	public JSONLDHelper(String context, String cbApiBaseUri, String baseURIRDF, MappingUtility mu) {
		this.mu = mu;
		this.context = context;
		this.baseURI = cbApiBaseUri;
		this.baseURIRDF = baseURIRDF;
	}
	
	/**
	 * This Method converts a given JSON string to JSON-LD
	 * @param json JSON String
	 * @param endpoint the called API-Endpoint
	 * @param addMapping if true, add mapping to known types
	 * @return
	 */
	public String json2jsonld(String json, String endpoint) {
		System.out.println("Endpoint " + endpoint);
		String id = endpoint;
		// JSON as given from Crunchbase
		JSONObject jsonObject = new JSONObject(json);
		
		// output jsonld object
		JSONObject jsonld = new JSONObject();

		jsonld.put("@context", context);
		
		int slashCount = id.length() - id.replace("/", "").length();
		String backwardSlashes = ""; // this is used to indicate the api depth. a dirty solution, but it works...
//		if (slashCount == 1) {
			backwardSlashes = "./";
			id = backwardSlashes +id.substring(1);
//		} else if (slashCount == 2) {
//			backwardSlashes = "../";
//			id = backwardSlashes +id.substring(1); //permalink as ID
//		} else if (slashCount == 3) {
//			backwardSlashes = "../../";
//			id = backwardSlashes + id.substring(1, id.lastIndexOf("/"));
//		}
		jsonld.put("@id", URIEncoder.encodeURIPath(id)+"#id");
		JSONObject documentLicense = new JSONObject();
		System.out.println(URIEncoder.encodeURIPath(id));
		documentLicense.put("@id", URIEncoder.encodeURIPath(id));
		documentLicense.put("cc:license", "http://creativecommons.org/licenses/by-nc/4.0/");
		jsonld.put("foaf:page", documentLicense);
		
		// get relevant json data from response
		JSONObject data = jsonObject.getJSONObject("data");
		for (String prop: JSONObject.getNames(data)) {
			jsonld.put(prop, data.get(prop));
		}
		
		if (jsonld.has("paging")) {
			JSONObject paging = jsonld.getJSONObject("paging");
			if (!paging.isNull("next_page_url")) {
				if (paging.has("number_of_pages") && paging.has("current_page")) {
					if (paging.getInt("current_page") < paging.getInt("number_of_pages")) {
						String pagingURI = backwardSlashes + paging.getString("next_page_url").replace(baseURI+"/", "/");
						jsonld.put(PREFIX+"next_page_url", URIEncoder.encodeURIPath(pagingURI));
						//System.out.print("PagingUrl" + URIEncoder.encodeURIPath(pagingURI));
					}
				}
			}
			//by ali for key_set_url part1
			if (!paging.isNull("key_set_url")) {
						String keySetURI = backwardSlashes + paging.getString("key_set_url").replace(baseURI+"/", "/");
						jsonld.put(PREFIX+"key_set_url", URIEncoder.encodeURIPath(keySetURI));
						//System.out.print("KeySetUrl" + URIEncoder.encodeURIPath(keySetURI));
			}
			//End of by ali for key_set_url part1
			
			jsonld.put("total_items", paging.getInt("total_items"));
			jsonld.remove("paging"); // To test Ali
		}
	
		
		if (jsonld.has("items")) { // for facebook/news
			String relationship = endpoint.substring(endpoint.lastIndexOf("/")+1);
			jsonld.put(relationship, jsonld.get("items")); // use API prefix to avoid confusion: api_news contains a link to news, news the blanknodes with actual news
			jsonld.remove("items");
			JSONObject newObject = new JSONObject();
			newObject.put("@id", backwardSlashes + endpoint.substring(1));
			newObject.put("total_items", jsonld.get("total_items"));
			jsonld.remove("total_items");
			
			if (jsonld.has(PREFIX+"next_page_url")) {
				newObject.put(PREFIX+"next_page_url", jsonld.get(PREFIX+"next_page_url"));
				jsonld.remove(PREFIX+"next_page_url");
			}
			jsonld.put(PREFIX+relationship, newObject);
		} else if (jsonld.has("relationships")) {
			JSONObject relationships = jsonld.getJSONObject("relationships");
			Iterator<String> relations = relationships.keys();
			while (relations.hasNext()) {
				String key = relations.next();
				JSONObject relationship = relationships.getJSONObject(key);
				if (relationship.has("paging")) {
					JSONObject paging = relationship.getJSONObject("paging");
					int totalItems = paging.getInt("total_items");
					String firstPageURL = paging.getString("first_page_url").replace(baseURI+"/", "");
					JSONObject newObject = new JSONObject();
					newObject.put("@id", backwardSlashes + firstPageURL);
					newObject.put("total_items", totalItems);
					jsonld.put(PREFIX+key, newObject);
				}
				
				if (relationship.has("items")) {
					jsonld.put(key, relationship.get("items"));
				}
				if (relationship.has("item")) {
					jsonld.put(key, relationship.get("item"));
				}
			}
			jsonld.remove("relationships");
		}
		
		// get every child in "relationships" and move it one step up
		JSONHelper.removeAndMoveUp(jsonld, "relationships");
		// get every child in "properties" and move it one step up
		JSONHelper.removeAndMoveUp(jsonld, "properties");
		// use the api_path as "@id" in json-ld
		JSONHelper.useAsID(jsonld, "api_path", backwardSlashes);
		// add "backwardslashes" to every api_path, so it is relative to the right url
		JSONHelper.addPrefixToAllValuesOfKey(jsonld, "api_path", backwardSlashes);
		//JSONHelper.addPrefixToAllValuesOfKey(jsonld, "next_page_url", backwardSlashes);//By Ali
		
		mu.insertMapping(jsonld);
		
		return jsonld.toString(2);
	}
	
	public String json2rdf(String json, String endpoint) {
		CollectorStreamTriples sink = new CollectorStreamTriples();
		//System.out.println("This is Json:");
		//System.out.println("This is Json:" +json);
		try {
		String jsonld = this.json2jsonld(json, endpoint);
		ByteArrayInputStream jsonldStream = new ByteArrayInputStream(jsonld.getBytes());
		RDFDataMgr.parse(sink, jsonldStream, baseURIRDF, Lang.JSONLD);
		OutputStream out = new ByteArrayOutputStream();
		Collection<Triple> triples = sink.getCollected();
		RDFDataMgr.writeTriples(out, triples.iterator());
		return out.toString();
		}
		catch (Exception e) {//By Ali to handle invalid urls in news summary
			String jsonld = this.json2jsonld(json, endpoint);
			JSONObject jsonObject = new JSONObject(jsonld);
			jsonObject.remove("news");
			String jsonldObjectString = jsonObject.toString(2);
			ByteArrayInputStream jsonldStream = new ByteArrayInputStream(jsonldObjectString.getBytes());
			RDFDataMgr.parse(sink, jsonldStream, baseURIRDF, Lang.JSONLD);
			OutputStream out = new ByteArrayOutputStream();
			Collection<Triple> triples = sink.getCollected();
			RDFDataMgr.writeTriples(out, triples.iterator());
			return out.toString();
		}
		
		
		
		
	}
	
}
