package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest.SmartplannerParameter;
import eu.trentorise.smartcampus.mobility.controller.rest.ItinerarySorter;
import eu.trentorise.smartcampus.mobility.util.PlanningPolicyHelper;

public class RoveretoPlanningPolicy implements PlanningPolicy {

	private static Log logger = LogFactory.getLog(RoveretoPlanningPolicy.class);
	
	@Override
	public List<PlanningRequest> generatePlanRequests(SingleJourney journeyRequest) {
		List<PlanningRequest> originalPlanningRequests = PlanningPolicyHelper.generateOriginalPlanRequests(journeyRequest);
		List<PlanningRequest> result = Lists.newArrayList();
		
		List<TType> types = Arrays.asList(journeyRequest.getTransportTypes());
		Set<TType> allTypes = Sets.newHashSet(types);
		
		PlanningResultGroup prg1a = new PlanningResultGroup("1a", 1,  journeyRequest.getRouteType());
		PlanningResultGroup prg1b = new PlanningResultGroup("1b", 1,  journeyRequest.getRouteType());
		PlanningResultGroup prg2 = new PlanningResultGroup("2", 2,  journeyRequest.getRouteType());
		
		for (PlanningRequest pr: originalPlanningRequests) {
			TType type = pr.getType(); 
			
			if (type.equals(TType.CAR) || type.equals(TType.CARWITHPARKING)) {
				if (!allTypes.contains(TType.PARK_AND_RIDE)) {
//					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.PARK_AND_RIDE, null, null, true, prg1a);
					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.PARK_AND_RIDE, null, null, null, pr.isWheelChair(), true, prg1a);
					result.add(npr);
					allTypes.add(TType.PARK_AND_RIDE);
				}	
				
				if (!allTypes.contains(TType.WALK)) {
//					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.WALK, null, 1, true, null);
					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.WALK, null, 1, null, pr.isWheelChair(), true, null);
					result.add(npr);
					allTypes.add(TType.WALK);
				}	
				
				if (!allTypes.contains(TType.TRANSIT)) {
//					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.TRANSIT, null, null, true, prg1b);
					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.TRANSIT, null, null, null, pr.isWheelChair(), true, prg1b);
					result.add(npr);
					allTypes.add(TType.TRANSIT);
				}
				
				if (!allTypes.contains(TType.SHAREDBIKE)) {
//					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.SHAREDBIKE, null, null, true, prg1b);
					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.SHAREDBIKE, null, null, null, pr.isWheelChair(), true, prg1b);
					result.add(npr);
					allTypes.add(TType.SHAREDBIKE);
				}					
				

			}
			
			if (type.equals(TType.TRANSIT) || type.equals(TType.BUS) || type.equals(TType.TRAIN)) {
				if (!allTypes.contains(TType.TRAIN)) {
//					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.TRAIN, null, null, true, prg2);
					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.TRAIN, null, null ,null, pr.isWheelChair(), true, prg2);
					result.add(npr);
					allTypes.add(TType.TRAIN);
				}	
				
				if (!allTypes.contains(TType.SHAREDBIKE)) {
//					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.SHAREDBIKE, null, null, true, prg2);
					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.SHAREDBIKE, null, null, null, pr.isWheelChair(), true, prg2);
					result.add(npr);
					allTypes.add(TType.SHAREDBIKE);
				}					
				
				if (!allTypes.contains(TType.WALK)) {
//					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.WALK, null, 1, true, prg2);
					PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, TType.WALK, null, 1, null, pr.isWheelChair(), true, prg2);
					result.add(npr);
					allTypes.add(TType.WALK);
				}					
			}
		}
		
		result.addAll(originalPlanningRequests);
		
		for (PlanningRequest pr: result) {
			TType type = pr.getType();
			
			if (type.equals(TType.CARWITHPARKING)) {
				pr.setSmartplannerParameter(SmartplannerParameter.extraTransport, TType.WALK);
			}
			if (type.equals(TType.SHAREDBIKE) || type.equals(TType.SHAREDBIKE_WITHOUT_STATION) || type.equals(TType.SHAREDCAR) || type.equals(TType.SHAREDCAR_WITHOUT_STATION)
					|| type.equals(TType.CARWITHPARKING) || type.equals(TType.PARK_AND_RIDE)) {		
				pr.setSmartplannerParameter(SmartplannerParameter.maxTotalWalkDistance, 1250);
				pr.setSmartplannerParameter(SmartplannerParameter.maxChanges, 2);
			}
			if (type.equals(TType.SHAREDBIKE) || type.equals(TType.SHAREDBIKE_WITHOUT_STATION) || type.equals(TType.BICYCLE)) {
				pr.setRouteType(RType.safest);
			}
			if (type.equals(TType.WALK) && pr.isPromoted()) {
				if (pr.getRouteType().equals(RType.leastWalking)) {
					pr.setSmartplannerParameter(SmartplannerParameter.maxTotalWalkDistance, 500);
				} else {
					pr.setSmartplannerParameter(SmartplannerParameter.maxTotalWalkDistance, 1000);
				}	
			}			
		}
		
		PlanningPolicyHelper.buildSmartplannerRequests(result);
		return result;
	}

	@Override
	public List<PlanningRequest> evaluatePlanResults(List<PlanningRequest> planRequests) {
		List<PlanningRequest> ok = Lists.newArrayList(planRequests);
		planRequests.removeAll(planRequests);
		return ok;
	}

	@Override
	public List<Itinerary> extractItinerariesFromPlanResults(SingleJourney journeyRequest, List<PlanningRequest> planRequests) {
		Comparator<Itinerary> comparator = ItinerarySorter.comparatorByRouteType(journeyRequest.getRouteType());
		
		List<Itinerary> remaining = PlanningPolicyHelper.filterByGroups(planRequests, comparator);

		List<Itinerary> toRemove = Lists.newArrayList();
		for (PlanningRequest pr: planRequests) {
			List<TType> tt = (List<TType>)Arrays.asList(pr.getOriginalRequest().getTransportTypes());
			if (tt.contains(TType.CAR) && pr.getType().equals(TType.WALK)) {
				for (Itinerary it: pr.getItinerary()) {
					double length = 0;
					for (Leg leg: it.getLeg()) {
						length += leg.getLength();
					}
					if (length > 2000) {
						toRemove.add(it);
					}
				}
			}
		}		
		
		remaining.removeAll(toRemove);
		
		return remaining;
	}

	@Override
	public List<Itinerary> filterAndSortItineraries(SingleJourney journeyRequest, List<Itinerary> itineraries) {
		List<Itinerary> result = Lists.newArrayList(itineraries);
		Comparator<Itinerary> comparator = ItinerarySorter.comparatorByRouteType(journeyRequest.getRouteType());
		
		ItinerariesAnalysis analysis = PlanningPolicyHelper.analyzeItineraries(itineraries, false);
		
		List<Itinerary> toRemove = Lists.newArrayList();
		
		for (Itinerary it : itineraries) {
			if (!it.isPromoted()) {
				continue;
			}
			if ((analysis.maxTime != 0 && it.getEndtime() > analysis.maxTime + (1000 * 60 * 30))
					|| (analysis.maxDuration != 0
							&& it.getDuration() > Math.min(analysis.maxDuration
									+ (1000 * 60 * 30), analysis.maxDuration * 1.5) && it
							.getEndtime() > analysis.minTime + (1000 * 60 * 10))) {
				toRemove.add(it);
				logger.info("Removing by \"slow\" trip: " + it.getDuration() + "," + analysis.maxDuration + " / " + it.getStartime() + "," + analysis.maxTime);
				continue;
			}		
			

			double distance = 0;
			for (Leg leg : it.getLeg()) {
				distance += leg.getLength();
			}

			if (analysis.maxDistance != 0 && distance > 2 * analysis.maxDistance) {
				toRemove.add(it);
				logger.info("Removing by distance: " + distance + "/" + analysis.maxDistance);
				continue;
			}
		}
		
		result.removeAll(toRemove);
		
		result = PlanningPolicyHelper.keepPromotedDuplicated(result);
		
		result = PlanningPolicyHelper.keepBestPromoted(result, comparator, 2);
		
		ItinerarySorter.sortDisjoined(result, comparator);
		return result;
	}

	@Override
	public String getName() {
		return "Bike Sharing";
	}

	@Override
	public String getDescription() {
		return "Rovereto";
	}	
	
	@Override
	public Boolean getDraft() {
		return false;
	}	
	
	@Override
	public PolicyType getPolicyType() {
		return PolicyType.hardcoded;
	}	
	
}
