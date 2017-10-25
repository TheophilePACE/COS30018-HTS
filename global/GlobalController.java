/** ----------------------------------------------------------------- */
/**    GlobalController											      */
/** ----------------------------------------------------------------- */

package global;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import apiWrapper.HttpClient;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

import brokerAgent.BrokerAgentController;
import homeAgent.HomeController;


import java.util.Map;


public class GlobalController {
	private static String HOME_AGENT_ADDRESS = "homeAgent";
	private static long CYCLE_TIME = 10000;
	private static int PORT = 1099;
	private static String HOST = null;
	private static String BROKER_AGENT_ADDRESS = "brokerAgent";
	private static String API_URL = "http://localhost:3001/api";

	private static String getSettings() {
		log("getting settings from "+API_URL);
		HttpClient httpc = new HttpClient(API_URL);
		try {
			return httpc.getSettings();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private static String resetDB() {
		HttpClient httpc = new HttpClient(API_URL);
		try {
			return httpc.resetMongoDB();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
    public static void getEnvApiUrl () {
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet()) {
            System.out.format("%s=%s%n",
                              envName,
                              env.get(envName));
        }
        if(env.containsKey("API_URL")) {
        API_URL = (String) env.get("API_URL");
        }
         }

	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		getEnvApiUrl(); 
		String settings =  getSettings();
		System.out.println("SETTINGS : "+settings);
		JSONObject jsonSettings = new JSONObject(settings);
		CYCLE_TIME= (int) (jsonSettings.get("CYCLE_TIME"));
		PORT = (int) (jsonSettings.get("JADE_PORT"));
		//reset the mongodb to avoid mixing with older records
		String reset = resetDB();
		log("RESET OF MPNGODB" +reset);
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
			homeController.createAppliance("applianceAgent2",API_URL,"fluctuating_load");
			homeController.createGeneration("generationAgent1",API_URL);
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
}
