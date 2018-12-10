package utilities;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONHelper {
	/**
	 * moves recursivley the content of keyName one step up and removes the key
	 * @param json
	 * @param keyName
	 */
	public static void removeAndMoveUp(JSONObject json, String keyName) {
		if (json.has(keyName)) {
			JSONObject keyObjects = json.getJSONObject(keyName);
			Iterator<String> keyIterator = keyObjects.keys();
			while (keyIterator.hasNext()) {
				String key = keyIterator.next();
				json.put(key, keyObjects.get(key));
			}
			json.remove(keyName);
		}
		
		Iterator<String> keyIterator = json.keys();
		while (keyIterator.hasNext()) {
			String key = keyIterator.next();
			try {
				Object object = json.get(key);
				if (object instanceof JSONObject) {
					JSONHelper.removeAndMoveUp((JSONObject)object, keyName);
				} else if (object instanceof JSONArray) {
					JSONArray array = (JSONArray) object;
					Iterator<Object> arrayIterator = array.iterator();
					while (arrayIterator.hasNext()) {
						Object jso = arrayIterator.next();
						if (jso instanceof JSONObject) {
							JSONHelper.removeAndMoveUp((JSONObject)jso, keyName);
						}
					}
				}
			} catch (JSONException e) {
			}
		}
	}
	
	/**
	 * Sets a the keyName with the @id tag. The backwardSlashes are important to resolve URLs correctly
	 * @param json
	 * @param keyName
	 * @param backward sth like ../, ../../, or just an empty string
	 */
	public static void useAsID(JSONObject json, String keyName, String backward) {
		if (json.has(keyName)) {
			String id = URIEncoder.encodeURIPath(backward+json.getString(keyName));
			json.put("@id", id+"#id");
			
			JSONObject documentLicense = new JSONObject();
			documentLicense.put("@id", id);
			documentLicense.put("cc:license", "http://creativecommons.org/licenses/by-nc/4.0/");
			json.put("foaf:page", documentLicense);
		}
		
		Iterator<String> keyIterator = json.keys();
		while (keyIterator.hasNext()) {
			String key = keyIterator.next();
			try {
				Object object = json.get(key);
				if (object instanceof JSONObject) {
					JSONHelper.useAsID((JSONObject)object, keyName, backward);
				} else if (object instanceof JSONArray) {
					JSONArray array = (JSONArray) object;
					Iterator<Object> arrayIterator = array.iterator();
					while (arrayIterator.hasNext()) {
						Object jso = arrayIterator.next();
						if (jso instanceof JSONObject) {
							JSONHelper.useAsID((JSONObject)jso, keyName, backward);
						}
					}
				}
				
			} catch (JSONException e) {
				
			}
			
		}
	}

	
	/**
	 * This replaces the value of the key with backwardSlashes and the URI-Encoded version of the url.
	 * This is important to resolve urls correctly
	 * @param json
	 * @param keyName
	 * @param backwardSlashes
	 */
	public static void addPrefixToAllValuesOfKey(JSONObject json, String keyName,
			String backwardSlashes) {
		if (json.has(keyName)) {
			json.put(keyName, URIEncoder.encodeURIPath(backwardSlashes+json.getString(keyName)));
		}
		
		Iterator<String> keyIterator = json.keys();
		while (keyIterator.hasNext()) {
			String key = keyIterator.next();
			try {
				Object object = json.get(key);
				if (object instanceof JSONObject) {
					JSONHelper.addPrefixToAllValuesOfKey((JSONObject)object, keyName, backwardSlashes);
				} else if (object instanceof JSONArray) {
					JSONArray array = (JSONArray) object;
					Iterator<Object> arrayIterator = array.iterator();
					while (arrayIterator.hasNext()) {
						Object jso = arrayIterator.next();
						if (jso instanceof JSONObject) {
							JSONHelper.addPrefixToAllValuesOfKey((JSONObject)jso, keyName, backwardSlashes);
						}
					}
				}
			} catch (JSONException e) {
			}
		}
		
	}
}
