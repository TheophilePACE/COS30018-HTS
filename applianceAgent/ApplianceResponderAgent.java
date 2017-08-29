/* ----------------------------------------------------------------- */
/*   Appliance Responder Agent                                       */
/*   Waits for energy usage request from ApplianceAgent, responds    */
/*   with a random purchased amount that is up to double what is     */
/*   requested. Also random chance to fail or refuse request         */
/* ----------------------------------------------------------------- */

package applianceAgent;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.FailureException;

public class ApplianceResponderAgent extends Agent {

	protected void setup() {
		System.out.println(getLocalName() + ": waiting for requests...");
		
		// Message template to listen only for messages matching the correct interaction protocol and performative
		MessageTemplate template = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

		// Add the AchieveREResponder behaviour which implements the responder role in a FIPA_REQUEST interaction protocol
		// The responder can either choose to agree to request or refuse request
		addBehaviour(new AchieveREResponder(this, template) {
			protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
				System.out.println(getLocalName() + ": REQUEST received from "
						+ request.getSender().getName() + ". Query is " + request.getContent());
				
				// Method to determine how to respond to request
				if (checkAction()) {
					// Agent agrees to perform the action. Note that in the FIPA-Request
					// protocol the AGREE message is optional. Return null if you
					// don't want to send it.
					System.out.println(getLocalName() + ": Agreeing to the request and responding with AGREE");
					ACLMessage agree = request.createReply();
					agree.setPerformative(ACLMessage.AGREE);
					return agree;
				} else {
					// Agent refuses to perform the action and responds with a REFUSE
					System.out.println("Agent " + getLocalName() + ": Refuse");
					throw new RefuseException("check-failed");
				}
			}

			// If the agent agreed to the request received, then it has to perform the associated action and return the result of the action
			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
					throws FailureException {
				// Get int value sent in request
				int req = Integer.valueOf(request.getContent().split("[a-z]")[0]);
				// Perform the action (dummy method)
				if (performAction()) {
					System.out.println(getLocalName() + ": Action successfully performed, informing initiator");
					ACLMessage inform = request.createReply();
					inform.setPerformative(ACLMessage.INFORM);
					// Reply with a random int value. min is requested amount, max is double
					inform.setContent(String.valueOf(randomWithRange(req, (req * 2))));
					return inform;
				} else {
					// Action failed
					System.out.println(getLocalName() + ": Action failed, informing initiator");
					throw new FailureException("unexpected-error");
				}
			}
		});
	}

	private boolean checkAction() {
		// Simulate a check by generating a random number
		return (Math.random() > 0.1);
	}

	private boolean performAction() {
		// Simulate action execution by generating a random number
		return (Math.random() > 0.1);
	}
	
	// Function to return a random int within a range
	int randomWithRange(int min, int max)
	{
	   int range = Math.abs(max - min) + 1;     
	   return (int)(Math.random() * range) + (min <= max ? min : max);
	}
}

