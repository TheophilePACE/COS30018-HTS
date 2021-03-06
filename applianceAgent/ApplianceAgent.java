/** ----------------------------------------------------------------- */
/**   Appliance Agent                                                 */
/** ----------------------------------------------------------------- */

package applianceAgent;

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

import apiWrapper.HttpClient;

@SuppressWarnings("serial")
public class ApplianceAgent extends Agent {
	double consumptionGearing = 0.64; 
	double yearlyConsumption = 5000; 
	private HttpClient httpClient;
	private String API_URL;

	private double getConsumption() {
		double [] consumption_pattern = null; //initialize consumption array
		
		if (consumptionType == "base_load") // sets the consumptionGearing for the base load appliance (opposite of fluctuating appliance as it sums up to 100%)
			consumptionGearing = 1-consumptionGearing;
		
		try {
			consumption_pattern = readCSVData(consumptionType); 
		} catch (Exception e) {
			System.out.println("CSV read in unsuccessful");
			e.printStackTrace();
		}
		double consumptionHourly = consumption_pattern[timeStep - (int) (timeStep/168)*168] * consumptionGearing * yearlyConsumption; // calculates the hourly consumption depending on the hour of the week (max 168h), restarts at hour 0 if second week starts
		return round(consumptionHourly,3); 
	}
	private String HOME_AGENT_ADDRESS;
	private String serviceType;
	private String serviceName;
	private String consumptionType;
	private int timeStep;
	private MessageTemplate energyBalanceMessageTemplate;

	protected void setup() {
		Object[] args = getArguments();
		if (args == null || args.length == 0) {
			throw new Error("Appliance AGent needs arguments!!!");
		}
		HOME_AGENT_ADDRESS = args[0].toString();
		serviceType = args[1].toString();
		serviceName = args[2].toString();
		API_URL= args[3].toString();
		consumptionType = args[4].toString();
		httpClient = new HttpClient(API_URL);
		
		registerService(serviceType, serviceName);
		log("created: "+serviceType+" -> "+serviceName);

		//tmplate for a Message type resuest from the homeagent
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
				updateSettings();
				JSONObject req = getRequestContent(request);
				timeStep = req.getInt("time");
				log("Sending consumption data with settings : yearlyCOnsumption = "+ yearlyConsumption+", consumptionGearing = " + consumptionGearing);
				ACLMessage consumptionMessageResponse = request.createReply();
				consumptionMessageResponse.setPerformative(ACLMessage.INFORM);
				String contentJSON = "{'consumption':" + getConsumption() +",'unit':'kWh','consumptionType':'"+consumptionType+"'}";
				consumptionMessageResponse.setContent(contentJSON); 
				return consumptionMessageResponse;
			}

			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
				log("Action successfully performed");
				ACLMessage inform = request.createReply();
				inform.setPerformative(ACLMessage.INFORM);
				return inform;
			}
		});
		
		log("Waiting for consumption requests...");
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
	//Method to retrieve consumption data from CSV file
	private double [] readCSVData(String target) throws Exception{
		final String CSV_FILENAME = "Total_Data.csv"; 
		double [] data = new double [168]; // array length equals to number of hours of 7days (24h * 7d)
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
				//				System.out.println(customerMap.get("rel_fluctuating load"));
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
	
	private void updateSettings() {
		try {
			String settings = httpClient.getSettings();
			JSONObject jsonSettings = new JSONObject(settings);
			 consumptionGearing =  (jsonSettings.getDouble("consumptionGearing")); 
			 yearlyConsumption =  (jsonSettings.getDouble("yearlyConsumption"));
			} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static double round (double value, int precision) {
	    int scale = (int) Math.pow(10, precision);
	    return (double) Math.round(value * scale) / scale;
	}
}
