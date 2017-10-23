/* ----------------------------------------------------------------- */
/*   Appliance Agent                                                 */
/*   Takes tick rate and ResponderAgent name as input args, sends    */
/*   energy usage request to ResponderAgent each tick based on its   */
/*   appliance type and yearly consumption pattern 					 */
/* ----------------------------------------------------------------- */

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
import java.util.Map;

import org.supercsv.cellprocessor.*;
import org.supercsv.cellprocessor.ift.*;
import org.supercsv.io.*;
import org.supercsv.prefs.*;
//import org.json.*;

public class ApplianceAgent extends Agent {
	
	//To do: needs input about yearly consumption, and ratio of base vs. fluctuating load from GUI SETTINGS && info about time step from home agent
	private double getConsumption() {
		double ratio = 0.5; //dummy value
		double yearly_consumption = 4000; //dummy value
		int timestep = 23; //dummy value
		double [] consumption_pattern = null; //initialize consumption array
		
		try {
			consumption_pattern = readCSVData("base_load"); //dummy value 
		} catch (Exception e) {
			System.out.println("CSV read in unsuccessful");
			e.printStackTrace();
		}
		double consumption_hourly = consumption_pattern[timestep] * ratio * yearly_consumption;
		return consumption_hourly; 
	}
	private long CYCLE_TIME;
	private String HOME_AGENT_ADDRESS;
	private String serviceType;
	private String serviceName;
	private MessageTemplate energyBalanceMessageTemplate;

	protected void setup() {
		Object[] args = getArguments();
		if (args == null || args.length == 0) {
			throw new Error("Appliance AGent needs arguments!!!");
		}

		CYCLE_TIME = (long)args[0];
		HOME_AGENT_ADDRESS = args[1].toString();
		serviceType = args[2].toString();
		serviceName = args[3].toString();
		
		registerService(serviceType, serviceName);
		log("created: "+serviceName+" -> "+serviceName);

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
				// We agree to perform the action. Note that in the FIPA-Request
				// protocol the AGREE message is optional. Return null if you
				// don't want to send it.
				log("Sending consumption data");
				ACLMessage consumptionMessageResponse = request.createReply();
				consumptionMessageResponse.setPerformative(ACLMessage.INFORM);
				String contentJSON = "{'consumption':" + getConsumption() +",unit:'kWh'}";
				consumptionMessageResponse.setContent(contentJSON); //TODO REFACTOR OUT OF BEHAVIOUR
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
	private String log(String s) {
		String toPrint = "[" + getLocalName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
	//Method to retrieve consumption data from CSV file
	private static double [] readCSVData(String target) throws Exception{
		final String CSV_FILENAME = "C:\\Users\\Victor\\Desktop\\Daten\\01_Masterstudium_TUM\\04_Swinburne\\02_Intelligent Systems\\COS30018_Group Assignment\\src\\cos30018\\applianceAgent\\Total_Data.csv"; //change to new path
		double [] data = new double [168];
		ICsvMapReader mapReader = null;
		try {
			mapReader = new CsvMapReader(new FileReader(CSV_FILENAME), CsvPreference.STANDARD_PREFERENCE);

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
}