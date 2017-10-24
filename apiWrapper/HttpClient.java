package apiWrapper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class HttpClient {
	private String API_URL;
	private JSONObject jsonDefaultSettings = new JSONObject("{'API_URL': 'http://localhost:3001/api','JADE_PORT': 1099,'CYCLE_TIME': 20000,'yearlyConsumption': 4026.1,'consumptionGearing': 0.64,'generationCapacity': 2.5,'roundsLimit': 25,'maxBuyingPrice': 8,'minSellingPrice': 15}");
	private boolean useApi = true;
	private String sendPost(String urlStr, String dataJSON) throws Exception {
	    URL url = new URL(urlStr);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setConnectTimeout(5000);
	    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
	    conn.setDoOutput(true);
	    conn.setDoInput(true);
	    conn.setRequestMethod("POST");

	    OutputStream os = conn.getOutputStream();
	    os.write(dataJSON.getBytes("UTF-8"));
	    os.close();

	    // read the response
	    InputStream in = new BufferedInputStream(conn.getInputStream());
	    String result = new BufferedReader(new InputStreamReader(in)) .lines().collect(Collectors.joining("\n"));

	    in.close();
	    conn.disconnect();

	    return result;
	}
	private String sendGet(String urlStr) throws Exception {


	    URL url = new URL(urlStr);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setConnectTimeout(5000);
	    conn.setRequestMethod("GET");

	    // read the response
	    InputStream in = new BufferedInputStream(conn.getInputStream());
	    String result = new BufferedReader(new InputStreamReader(in)) .lines().collect(Collectors.joining("\n"));

	    in.close();
	    conn.disconnect();

	    return result;

	}

	public String sendConsumption(String dataJSON) throws Exception {
		if(!useApi) return "";
	    return sendPost(this.API_URL+"/consumptions", dataJSON);
	}
	public String sendPrice(String dataJSON) throws Exception {
		if(!useApi) return "";
	    return sendPost(this.API_URL+"/prices", dataJSON);
	}
	public String getSettings() throws Exception {
		if(!useApi) return jsonDefaultSettings.toString();

		return sendGet(this.API_URL+"/settings");
	}
	public String resetMongoDB() throws Exception {
		if(!useApi) return "";
	    return sendGet(this.API_URL+"/resetDB");
	}
	
	public HttpClient(String apiUrl) {
		this.API_URL=apiUrl;
	}
}
