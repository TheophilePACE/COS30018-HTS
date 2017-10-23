/** ----------------------------------------------------------------- */
/**   Retail Agent                                                    */
/** ----------------------------------------------------------------- */

package retailAgent;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SSIteratedContractNetResponder;
import jade.proto.SSResponderDispatcher;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.FIPANames;

import org.json.JSONObject;

@SuppressWarnings("serial")
public class RetailAgent extends Agent {
	// The current price for electricity
	private double getBuyPrice(int negotiationRound) {
		double offer = 11.3; // equals to the current Feed in tariff in Victoria as of 2017
		offer = 11.3 + (Math.sin((10*Math.random())*(double)negotiationRound)+(double)negotiationRound/2);
		return round(offer, 2);
	}
	private double getSellPrice(int negotiationRound) {
		double offer;
		if (negotiationStrategy == 0)
		{
			offer = initialOffer + 3*Math.sin(initialOffer*Math.random()*negotiationRound)-Math.sqrt(negotiationRound*2); //neutral conceder utility function
		}
		else
		{
			offer = Math.exp(1.05*Math.random()-(negotiationRound/30)*Math.log(initialOffer-7))+7; //aggressive conceder utility function with a price limit at 7 c/kWh
		}
		return round(offer, 2);
	}

	private double currentOffer = 0;
	private double initialOffer;
	private String requestType;
	private int round;
	private boolean end; // Represents negotiation round limit status
	private int negotiationStrategy = 0; // states if agent was successful in last negotiation round
	private int currentStrategy;
	private boolean successfulNegotiation = false; //states if agent has won last round of negotiations

	// Agent initialization
	protected void setup() {
		Object[] args = getArguments();
		if (args == null || args.length == 0) {
			throw new Error("Retailer Agent needs arguments!!!");
		}		// Printout a welcome message
		log("I have been created. Company: " + args[1].toString());

		// Set the type of service and the company name as a start-up argument
		ServiceDescription sd  = new ServiceDescription();
		sd.setType( args[0].toString());
		sd.setName( args[1].toString());
		register(sd);

		// Sets the initial offer price
		initialOffer = Double.parseDouble(args[2].toString()); 

		// Create message template
		MessageTemplate template = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
				MessageTemplate.MatchPerformative(ACLMessage.CFP) );

		addBehaviour(new SSResponderDispatcher(this, template) {
			@Override
			protected Behaviour createResponder(ACLMessage initiationMsg) {
				return new SSIteratedContractNetResponder(myAgent, initiationMsg) {
					@Override
					protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
						// Get request as JSON object
						JSONObject req = getRequestContent(cfp);
						requestType = req.getString("requestType");
						round = req.getInt("round");
						end = req.getBoolean("end");

						if(round == 1) {
							if (checkStrategy()) 
								negotiationStrategy = 0;
							else
								negotiationStrategy = 1;
						}else
						{
							negotiationStrategy = currentStrategy;
						}

						if (checkAction())
						{
							switch (round) {
							case 1:
								log("Price request received from " + cfp.getSender().getLocalName() + ". Request is " + cfp.getContent());		

								// Reply with the price offer based on request type
								if (requestType.equals("Buy")) // The broker wants to buy
									currentOffer = initialOffer;
								else // The broker wants to sell
									currentOffer = getBuyPrice(round);
								currentStrategy = negotiationStrategy;
								break;
							default:														
								if (requestType.equals("Buy"))
								{
									if (end == false)
									{
										if (currentOffer == 0)
										{
											log("We have yet to provide an offer. Current round is: " + round);
											currentOffer = getSellPrice(round);
										}										
										else
										{
											currentOffer = getSellPrice(round);
											log(cfp.getSender().getLocalName() + " wants a better offer. Current round is: " + round + ". Current offer is: " + currentOffer);
										}										
									}
									else 
									{
										currentOffer = initialOffer;
									}
								}
								else
								{
									if (end == false)
									{
										if (currentOffer == 0)
										{
											log("We have yet to provide an offer. Current round is: " + round);
											currentOffer = getBuyPrice(round);
										}										
										else
										{
											currentOffer = getBuyPrice(round);
											log(cfp.getSender().getLocalName() + " wants a better offer. Current round is: " + round + ". Current offer is: " + currentOffer);

										}
									}
									else
									{
										currentOffer = getBuyPrice(0);
									}
								}

								break; 

							}

							log("Offering price: '" + currentOffer + " c/kWh'");
							String contentJSON = "{'price':" + currentOffer + "}";
							ACLMessage propose = cfp.createReply();
							propose.setPerformative(ACLMessage.PROPOSE);
							propose.setContent(contentJSON);
							return propose;
						}
						else 
						{
							// We refuse to provide a proposal
							log("Cannot provide an offer at this time");
							currentStrategy = negotiationStrategy;
							throw new RefuseException("evaluation-failed");
						}
					}

					@Override
					protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
						// Broker agent accepted our proposal
						log(accept.getSender().getLocalName() + " accepted our proposal. Rounds taken: " + round);
						log("Informing " + accept.getSender().getLocalName() + " of order success");
						// Inform broker agent of order success
						ACLMessage inform = accept.createReply();
						inform.setPerformative(ACLMessage.INFORM);
						String contentJSON = "{'price':" + currentOffer + "}";
						inform.setContent(contentJSON);
						currentOffer = 0;
						successfulNegotiation = true;
						return inform;	
					}

					protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
						log("Proposal rejected");
					}

					public int onEnd() {
						currentOffer = 0;						
						return 0;
					}
				};
			}
		});

		log("Waiting for price requests...");
	}

	// Method to register the service
	void register( ServiceDescription sd)
	{
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		dfd.addServices(sd); 

		try {  
			DFService.register(this, dfd );  
		}
		catch (FIPAException fe) { fe.printStackTrace(); }
	}

	// Method to de-register the service (on take down)
	protected void takeDown() 
	{
		try { DFService.deregister(this);
		// Printout a dismissal message
		log("Terminating.");
		}
		catch (Exception e) {
			log("Error at service de-registration of Agent " + getLocalName() );
		}
	}

	private JSONObject getRequestContent(ACLMessage request) {
		return new JSONObject(request.getContent());
	}

	private String log(String s) {
		String toPrint = "[" + getLocalName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}

	//Method to check if requested action should be continued
	// 10% chance to fail at this stage
	private boolean checkAction() {
		if (Math.random() > 0.1)
			return true;
		else
			return false;
	}
	// Method to set the strategy for the whole negotiation session (winning bidder changes to passive strategy)
	private boolean checkStrategy() {
		if (successfulNegotiation == true) {
			successfulNegotiation = false;
			return true;
		}else {
			return false;
		}
	}
	private static double round (double value, int precision) {
	    int scale = (int) Math.pow(10, precision);
	    return (double) Math.round(value * scale) / scale;
	}
}