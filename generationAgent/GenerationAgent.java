/** ----------------------------------------------------------------- */
/**   Generation Agent                                                */
/**   Generates a consistent amount of energy in kwH and returns      */
/**   accrued energy when asked.  The generation amount depends on    */
/**   a historical PV generation pattern retrieved in VIC, Australia  */
/** ----------------------------------------------------------------- */

package generationAgent;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.json.JSONObject;
import org.supercsv.cellprocessor.*;
import org.supercsv.cellprocessor.ift.*;
import org.supercsv.io.*;
import org.supercsv.prefs.*;
import org.json.*;

import apiWrapper.HttpClient;


@SuppressWarnings("serial")
public class GenerationAgent extends Agent {
	double generationCapacity = 2.5; 
	private HttpClient httpClient;
	private String API_URL;
	
	private double getProduction() {
		double [] productionPattern = null; //initialize consumption array
		
		try {
			productionPattern = readCSVData("PV_generation"); 
		} catch (Exception e) {
			System.out.println("CSV read in unsuccessful");
			e.printStackTrace();
		}
		double productionHourly = productionPattern[timeStep - (int) (timeStep/168)*168] * generationCapacity;
		return round(productionHourly * -1,3);
	}
	private String HOME_AGENT_ADDRESS;
	private String serviceType;
	private String serviceName;
	private int timeStep;
	private MessageTemplate energyBalanceMessageTemplate;

	protected void setup() {
		Object[] args = getArguments();
		if (args == null || args.length == 0) {
			throw new Error("Generation Agent needs arguments!!!");
		}
		HOME_AGENT_ADDRESS = args[0].toString();
		serviceType = args[1].toString();
		serviceName = args[2].toString();
		API_URL= args[3].toString();
		httpClient = new HttpClient(API_URL);

		registerService(serviceType, serviceName);
		log("created: "+serviceName+" -> "+serviceName);

		//template for a Message type request from the homeagent
		energyBalanceMessageTemplate = MessageTemplate.and( MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST)),
				MessageTemplate.MatchSender(new AID(HOME_AGENT_ADDRESS,AID.ISLOCALNAME)) );

		// Add Ticker Behaviour, rate is input arg
		addBehaviour(new AchieveREResponder(this, energyBalanceMessageTemplate) {				
			@Override
			protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
				log("Request received from "+request.getSender().getLocalName()+". Request is "+request.getContent());
				// get current time step
				JSONObject req = getRequestContent(request);
				timeStep = req.getInt("time");
				updateSettings();
				log("Sending generation data with settings : generationCapacity = " + generationCapacity);
				ACLMessage productionMessageResponse = request.createReply();
				productionMessageResponse.setPerformative(ACLMessage.INFORM);
				String contentJSON = "{'consumption':" + getProduction() +",unit:'kWh','consumptionType':'PV_generation'}";
				productionMessageResponse.setContent(contentJSON);
				return productionMessageResponse;
			}

			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
				log("Action successfully performed");
				ACLMessage inform = request.createReply();
				inform.setPerformative(ACLMessage.INFORM);
				return inform;
			}
		});
		log("Waiting for production requests...");
	}
	

	// Method to register the service
	void registerService(String type,String name)
	{
		ServiceDescription sd  = new ServiceDescription();
		sd.setType(type);
		sd.setName(name);
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		dfd.addServices(sd); 

		try {  
			DFService.register(this, dfd );  
		}
		catch (FIPAException fe) { fe.printStackTrace(); }
	}

	protected void takeDown() {
		log("Preparing to die");
		// do cleanup
	}
	
	private JSONObject getRequestContent(ACLMessage request) {
		return new JSONObject(request.getContent());
	}
	
	private String log(String s) {
		String toPrint = "[" + getLocalName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
	//Method to retrieve consumption/production data from CSV file
	private double [] readCSVData(String target) throws Exception{
		final String CSV_FILENAME = "Total_Data.csv"; 
		double [] data = new double [168];
		ICsvMapReader mapReader = null;
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("Total_Data.csv");
		try {
			mapReader = new CsvMapReader(new InputStreamReader(in), CsvPreference.STANDARD_PREFERENCE);

			final String[] header = mapReader.getHeader(true); 
			final CellProcessor[] processors = getProcessors();

			Map<String, Object> customerMap;
			int i = 0;
			while( (customerMap = mapReader.read(header, processors)) != null ) {
				data[i] = (double) customerMap.get(target);
				i++;
			}
		}
		finally {
			if( mapReader != null ) {
				mapReader.close();
			}
		}	
		return data;
	}
	// Method to define cell data type
	private static CellProcessor [] getProcessors() {

		final CellProcessor[] processors = new CellProcessor [] {
				new ParseInt(),//hourly time step
				new ParseDouble(),//relative baseload energy consumption
				new ParseDouble(),//relative fluctuating energy consumption
				new ParseDouble() //relative PV production
		};
		return processors;
	}
	private static double round (double value, int precision) {
	    int scale = (int) Math.pow(10, precision);
	    return (double) Math.round(value * scale) / scale;
	}
	private void updateSettings() {
		try {
			String settings = httpClient.getSettings();
			JSONObject jsonSettings = new JSONObject(settings);
			generationCapacity =  (jsonSettings.getDouble("generationCapacity")); 
			} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
