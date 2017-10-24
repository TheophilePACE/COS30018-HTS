/** ----------------------------------------------------------------- */
/**    HomeAgent												      */
/** ----------------------------------------------------------------- */

package homeAgent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import org.json.*;

import apiWrapper.HttpClient;

@SuppressWarnings("serial")
public class HomeAgent extends Agent {
	private int CYCLE_TIME;
	private long time = 0 ;
	private String API_URL= "";
	//Internal variables
	private double energyBalance = 0; //sum of production and consumption of energy
	private double energyProducted = 0;
	private double energyConsumed = 0;

	//TODO Change at every cycle. Provided by the Broker
	@SuppressWarnings("unused")
	private int BetterPrice = 0;
	@SuppressWarnings("unused")
	private String BetterProvider = "";
	private String brokerAgentAddress = "";
	private HashMap<String, Double> applianceEnergyBalance = new HashMap<String, Double>();
	private AID[] applianceList, generationList;
	HttpClient httpc;
	private MessageTemplate energyBalanceMessageTemplate = MessageTemplate.and(
			MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
			MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

	protected void setup() {
		//FIPANames.InteractionProtocol.FIPA_REQUEST
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			API_URL = args[0].toString();
			CYCLE_TIME = Integer.parseInt(args[1].toString());
			brokerAgentAddress = args[2].toString();
			httpc = new HttpClient(API_URL);
		} 

		TickerBehaviour triggerEnergyBalance = (new TickerBehaviour(this,CYCLE_TIME) {
			public void onTick() {
				/*updateSettings();								******UNCOMMENT FOR API SETTINGS*********
				if(CYCLE_TIME!=(int)this.getPeriod())
				{//CHANGE THE CYCLE TIME
					log("Cycle time has been changed from "+this.getPeriod() + " to " + CYCLE_TIME);
					this.reset(CYCLE_TIME);
				}*/
				time++; //one hour more
				System.out.println();
				log("<---------------- || NEW CYCLE || Time: "+ time +" || CYCLE_TIME: " + CYCLE_TIME + " || ----------------->");
				//Get all appliances address
				applianceList = getAgentDescriptionList("Appliance");
				//Get all generation address
				generationList = getAgentDescriptionList("Generation");
				//MSG to all appliances asking for consumption
				ACLMessage consumptionRequest = createConsumptionRequest(applianceList, generationList);
				addBehaviour(new collectApplianceEnergyBalances(myAgent, consumptionRequest, applianceList.length + generationList.length));
			}
		});
		addBehaviour(triggerEnergyBalance);
	}

	private class collectApplianceEnergyBalances extends AchieveREInitiator {
		private int nResponders =0;
		private Agent a;
		public collectApplianceEnergyBalances(Agent ag, ACLMessage consumptionRequest, int nbAppliances) {
			super(ag, consumptionRequest);
			a= ag;
			nResponders = nbAppliances;
			log("Sending consumption requests");
		}
		private ACLMessage createTradeRequest(double quantity) {
			
			ACLMessage tradeRequest = new ACLMessage(ACLMessage.REQUEST);
			// Add receivers from input args
			tradeRequest.addReceiver(new AID(brokerAgentAddress, AID.ISLOCALNAME));
			// Set the interaction protocol
			tradeRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			// Specify the reply deadline (10 seconds)
			tradeRequest.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
			// Set message content, if quantity >0 we need to buy otherwise sell
			String contentJSON = "{'requestType':'" 
					+( ( quantity > 0) ? "Buy" : "Sell" )+ 
					"','quantity':" + quantity + "}";
			tradeRequest.setContent(contentJSON);
			return tradeRequest;
		}
		protected void handleInform(ACLMessage inform) {
			//Received the expected information
			JSONObject JSONmsg = new JSONObject(inform.getContent());
			log("Received consumption from " + inform.getSender().getLocalName() + ". Consumption is: " + JSONmsg.getDouble("consumption"));		
			setApplianceEnergyBalance(inform.getSender().getLocalName(), new Double(JSONmsg.getDouble("consumption")));
			nResponders--;
		}
		protected void handleFailure(ACLMessage failure) {
			if (failure.getSender().equals(a.getAMS())) {
				// FAILURE notification from the JADE runtime: the receiver
				// does not exist
				System.out.println("Responder does not exist");
			}
			else {
				System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
			}
		}
		protected void handleAllResultNotifications(@SuppressWarnings("rawtypes") Vector notifications) {
			//Response received/not received from all appliances
			if (notifications.size() < nResponders) {
				// Some responder didn't reply within the specified timeout
				log("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
			} else {
				log("Received all consumption respones.");
			}
			double quantity =  makeEnergyBalance();
			log("Consumption: " + energyConsumed + " Generation: " + energyProducted * -1 + " --> BALANCE = " + quantity);

			if (quantity != 0)
			{
				ACLMessage tradeRequest = createTradeRequest(quantity);
				a.addBehaviour(new Negotiation(a,tradeRequest));
			}
			else
			{
				log("Perfect balance. No action required!");
			}
		}

	} 

	private class Negotiation extends AchieveREInitiator{
		private ACLMessage tR;
		public Negotiation(Agent a, ACLMessage tRM) {
			super(a, tRM);
			tR = tRM;
		}

		protected void handleAgree(ACLMessage agree) {
			log(agree.getSender().getLocalName() + " has agreed to the request");
		}

		// Method to handle an inform message from responder
		protected void handleInform(ACLMessage inform) {
			JSONObject response = new JSONObject(inform.getContent());
			log(inform.getSender().getLocalName() + " successfully performed the request: '" + tR.getContent()
			+ " negotiated price of: '" + response.getDouble("price") + " c/kWh'");
			//storeNegotiatedPrice(response.getString("retailerId"),response.getDouble("price"));
			log("<----------------- || END OF NEGOTIATION || SUCCESS || ---------------------->");
			System.out.println();
			System.out.println();
		}

		// Method to handle a refuse message from responder
		protected void handleRefuse(ACLMessage refuse) {
			log(refuse.getSender().getLocalName() + " refused to perform the requested action");
		}

		// Method to handle a failure message (failure in delivering the message)
		protected void handleFailure(ACLMessage failure) {
			if (failure.getSender().equals(myAgent.getAMS())) {
				// FAILURE notification from the JADE runtime: the receiver (receiver does not exist)
				log("Responder does not exist");
			} else {
				log(failure.getSender().getLocalName() + " failed to perform the requested action");
				log("<----------------- || END OF NEGOTIATION || UNSUCCESSFUL ||---------------------->");
			}
		}
	}

	private ACLMessage createConsumptionRequest(AID[] appReceivers, AID[] genReceivers) {
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		for (int i = 0; i < appReceivers.length; ++i) {
			msg.addReceiver(appReceivers[i]);
		}
		for (int i = 0; i < genReceivers.length; ++i) {
			msg.addReceiver(genReceivers[i]);
		}
		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
		msg.setContent("{'request':'energyConsumption','time':"+time+"}");
		return msg;
	}

	private void setApplianceEnergyBalance(String applianceName, Double balance) {
		applianceEnergyBalance.put(applianceName, balance);
	}
	//TODO consider positive negative ...
	private double makeEnergyBalance() {
		energyConsumed = 0;
		energyProducted = 0;

		applianceEnergyBalance.forEach((k,v)->{
			if(v>0)
				energyConsumed+=v;
			else
				energyProducted+=v;
			//storeApplianceEnergyBalance(k, v); //Store old value
		});
		applianceEnergyBalance.clear(); // remove old values

		energyBalance = energyConsumed + energyProducted;
		return energyBalance;
	}

	private AID[] getAgentDescriptionList(String serviceType) {
		// Search for appliance agents
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(serviceType);
		dfd.addServices(sd);

		// Create and print list of retail agents found
		try {
			DFAgentDescription[] result = DFService.search(this, dfd); 
			log("Found the following " + serviceType + " agents:");
			AID[] agentsFound = new AID[result.length];
			for (int i = 0; i < result.length; ++i) {
				agentsFound[i] = result[i].getName();
				log(agentsFound[i].getLocalName());
			}
			return agentsFound;
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
			return null;
		}
	}

	private void updateSettings() {
		try {
			String settings = httpc.getSettings();
			JSONObject jsonSettings = new JSONObject(settings);
			CYCLE_TIME= (int) (jsonSettings.get("CYCLE_TIME"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void storeApplianceEnergyBalance(String applianceName,int balance) {
		JSONObject jsonEnergyBalance = new JSONObject();
		jsonEnergyBalance.put("applianceId", applianceName);
		jsonEnergyBalance.put("time", this.time);
		jsonEnergyBalance.put("quantity", balance);
		try {
			String requestResult = httpc.sendConsumption(jsonEnergyBalance.toString());
			log("Stored in db : "+ jsonEnergyBalance.toString() +"Result : " + requestResult);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void storeNegotiatedPrice(String retailerId,double price, double quantity) {
		JSONObject jsonPrice = new JSONObject();
		jsonPrice.put("price", price);
		jsonPrice.put("time", this.time);
		jsonPrice.put("retailerId", retailerId);
		try {
			String requestResult = httpc.sendPrice(jsonPrice.toString());
			log("Stored in db : "+ jsonPrice.toString() +"Result : " + requestResult);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String log(String s) {
		String toPrint = "[" + getLocalName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
}
