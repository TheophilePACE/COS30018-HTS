/** ----------------------------------------------------------------- */
/**    GlobalController											      */
/** ----------------------------------------------------------------- */

package global;

import java.net.*;

import apiWrapper.HttpClient;

import java.io.*;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

import brokerAgent.BrokerAgentController;
import homeAgent.HomeController;

public class GlobalController {
	private static String HOME_AGENT_ADDRESS = "homeAgent";
	private static long CYCLE_TIME = 10000;
	private static int PORT = 1099;
	private static String HOST = null;
	private static String BROKER_AGENT_ADDRESS = "brokerAgent";
	private static String API_URL = "http://localhost:3001/api";

	private static String getSettings() {
		HttpClient httpc = new HttpClient(API_URL);
		try {
			return httpc.getSettings();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		/*String settings =  getSettings();					******UNCOMMENT FOR API SETTINGS*********
		System.out.println("SETTINGS : "+settings);
		JSONObject jsonSettings = new JSONObject(settings);
		CYCLE_TIME= (int) (jsonSettings.get("CYCLE_TIME"));
		PORT = (int) (jsonSettings.get("JADE_PORT"));*/
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();

		// Launch the Main Container (with the administration GUI on top) listening on port 1099
		log(": Launching the platform Main Container on port "+ PORT +" ...");
		ProfileImpl pMain = new ProfileImpl(HOST, PORT, null,true);
		pMain.setParameter(Profile.GUI, "true");
		@SuppressWarnings("unused")
		ContainerController mainCtrl = rt.createMainContainer(pMain);

		//Launch homecontainer and agents
		HomeController homeController = new HomeController(HOME_AGENT_ADDRESS,CYCLE_TIME,rt,HOST,PORT,BROKER_AGENT_ADDRESS);
		try {
			homeController.createHomeAgent(API_URL);
			homeController.createAppliance("applianceAgent1",API_URL,"base_load");
			homeController.createAppliance("applianceAgent2",API_URL,"fluctutating_load");
			homeController.createGeneration("generationAgent1");
		} catch (Exception e) {
			log(e.toString());
		}
		
		//Launch a broker and retailers
		BrokerAgentController brokerAgentController = new BrokerAgentController(rt, HOST, PORT, API_URL);
		brokerAgentController.createRetailerAgent("retailAgent1", "Retail Agent", "GloBird Energy","20.8");
		brokerAgentController.createRetailerAgent("retailAgent2", "Retail Agent", "Origin","23.56");
		brokerAgentController.createRetailerAgent("retailAgent3", "Retail Agent", "Pacific Hydro","16.1");
		brokerAgentController.createBrokerAgent();
	}
	
	private static String log(String s) {
		String toPrint = "[" + HomeController.class.getName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
	
	public static String executePost(String targetURL, String urlParameters) {
	  HttpURLConnection connection = null;

	  try {
	    //Create connection
	    URL url = new URL(targetURL);
	    connection = (HttpURLConnection) url.openConnection();
	    connection.setRequestMethod("POST");
	    connection.setRequestProperty("Content-Type", 
	        "application/x-www-form-urlencoded");

	    connection.setRequestProperty("Content-Length", 
	        Integer.toString(urlParameters.getBytes().length));
	    connection.setRequestProperty("Content-Language", "en-US");  

	    connection.setUseCaches(false);
	    connection.setDoOutput(true);

	    //Send request
	    DataOutputStream wr = new DataOutputStream (
	        connection.getOutputStream());
	    wr.writeBytes(urlParameters);
	    wr.close();

	    //Get Response  
	    InputStream is = connection.getInputStream();
	    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	    StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
	    String line;
	    while ((line = rd.readLine()) != null) {
	      response.append(line);
	      response.append('\r');
	    }
	    rd.close();
	    return response.toString();
	  } catch (Exception e) {
	    e.printStackTrace();
	    return null;
	  } finally {
	    if (connection != null) {
	      connection.disconnect();
	    }
	  }
	}
}
