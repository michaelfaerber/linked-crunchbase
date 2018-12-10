package utilities;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class URIEncoder {
	/**
	 * this function encodes a given URI Path, but keeps the URL Structure and URL Parameters
	 */
	public static String encodeURIPath(String path)  {
		String delimiter = "/";
		StringBuilder sb = new StringBuilder();
		for (String str : path.split(delimiter)) {
			try {
				sb.append(URLEncoder.encode(str, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sb.append(delimiter);
		}
		
		String encodedURIPath = sb.toString();
		if (!path.endsWith(delimiter)) {
			encodedURIPath = encodedURIPath.substring(0, encodedURIPath.length()-1);
		}
		encodedURIPath = encodedURIPath.replace("%3F", "?");
		encodedURIPath = encodedURIPath.replace("%3D", "=");
		encodedURIPath = encodedURIPath.replace("%26", "&");
		//Change 5 for next_page_url
		encodedURIPath = encodedURIPath.replace("%2520", "%20");
		//End of Change 5
		return encodedURIPath;
	}

}
