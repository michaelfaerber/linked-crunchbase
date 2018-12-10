package utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class MappingUtility {
	private final static String TYPE = "type";
	private final static String SAMEAS = "owl:sameAs";
	private HashMap<String, String> organizationMapping;
	private HashMap<String, String> personMapping;
	
	public MappingUtility(InputStream inputStream, InputStream inputStream2) {

		try {
			readMappings(inputStream, inputStream2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Iterates recursively over the whole JSON structure and inserts the mapping
	 */
	public void insertMapping(JSONObject json) {
		if (json.has(TYPE)) {
			String type = json.getString(TYPE);
			String sameAs = "";
			if (!json.isNull("permalink")) {
				String permalink = json.getString("permalink")+ "#id";
				if (type.equals("Organization")) {
					sameAs = organizationMapping.get(permalink);
				}
				else if(type.equals("Person")) {
					sameAs = personMapping.get(permalink);
				}
			}
			if ((sameAs != null) && (!sameAs.isEmpty())) {
				json.put(SAMEAS, sameAs);
			}
		}
		
		Iterator<String> keyIterator = json.keys();
		while (keyIterator.hasNext()) {
			String key = keyIterator.next();
			try {
				Object object = json.get(key);
				if (object instanceof JSONObject) {
					this.insertMapping((JSONObject)object);
				} else if (object instanceof JSONArray) {
					JSONArray array = (JSONArray) object;
					Iterator<Object> arrayIterator = array.iterator();
					while (arrayIterator.hasNext()) {
						Object jso = arrayIterator.next();
						if (jso instanceof JSONObject) {
							this.insertMapping((JSONObject)jso);
						}
					}
				}
			} catch (JSONException e) {
			}
		}
	}
	
	public String getOrganizationMapping(String identifier) {
		return organizationMapping.get(identifier);
	}
	
	public String getPersonMapping(String identifier) {
		return personMapping.get(identifier);
	}
	
	
	private void readMappings(InputStream inputStreamMappings1, InputStream inputStreamMappings2) throws IOException {
		/** organization mappings **/
		organizationMapping = new HashMap<>();
		BufferedReader br;
		String line;
		int startIndex = 46; // length of http://linked-crunchbase.org/api/organization/ organization-mappings.nt file. Need to change when 
							// the mapping file has new address for organizations corresponding to my project
		br = new BufferedReader(new InputStreamReader(inputStreamMappings1));
		String tmp;
		while ((line = br.readLine()) != null) {
			tmp = line.trim();
			if (!tmp.isEmpty()) {
				String[] splittedStrings = tmp.split("\\s+"); // split by whitespace: <http://linked-crunchbase.org/api/organization/email-ideas-llc#id> <http://www.w3.org/2002/07/owl#sameAs> <http://dbpedia.org/resource/Email_Ideas> .
				String cbEntity = splittedStrings[0]; 
				String dbpEntity = splittedStrings[2];
				dbpEntity = dbpEntity.substring(1, dbpEntity.length()-1); // remove braces < >
				cbEntity = cbEntity.substring(1, cbEntity.length()-1); // remove braces < >
				cbEntity = cbEntity.substring(startIndex); // remove the prefix http://linked-crunchbase.org/api/organization/
				organizationMapping.put(cbEntity, dbpEntity);
			}
		}
		br.close();
		br = null;
		
		/** person mappings **/
		personMapping = new HashMap<>();
		line = null;
		startIndex = 40; // length of "http://linked-crunchbase.org/api/people/" in people-mappings.nt file . Need to change when 
							// the mapping file has new address for organizations corresponding to my project
		br = new BufferedReader(new InputStreamReader(inputStreamMappings2));
		tmp = null;
		while ((line = br.readLine()) != null) {
			tmp = line.trim();
			if (!tmp.isEmpty()) {
				String[] splittedStrings = tmp.split("\\s+"); // split by whitespace: <http://linked-crunchbase.org/api/organization/email-ideas-llc#id> <http://www.w3.org/2002/07/owl#sameAs> <http://dbpedia.org/resource/Email_Ideas> .
				String cbEntity = splittedStrings[0]; 
				String dbpEntity = splittedStrings[2];
				dbpEntity = dbpEntity.substring(1, dbpEntity.length()-1); // remove braces < >
				cbEntity = cbEntity.substring(1, cbEntity.length()-1); // remove braces < >
				cbEntity = cbEntity.substring(startIndex); // remove the prefix http://linked-crunchbase.org/api/organization/
				organizationMapping.put(cbEntity, dbpEntity);
			}

		}
		br.close();
		br = null;
	}
	
}
