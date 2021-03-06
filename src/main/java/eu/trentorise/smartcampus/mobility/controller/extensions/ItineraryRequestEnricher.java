package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Multimap;

public interface ItineraryRequestEnricher {

	public List<PlanRequest> addPromotedItineraries(SingleJourney request, TType type, RType routeType);
	public List<Itinerary> filterPromotedItineraties(Multimap<Double, Itinerary> itineraries, Collection<PlanRequest> requests, SingleJourney request);
	public List<Itinerary> removeExtremeItineraties(List<Itinerary> itineraries, RType criteria);
	public void completeResponse(SingleJourney journeyRequest, List<PlanRequest> planRequests, List<Itinerary> itineraries);
	public void sort(List<Itinerary> itineraries, RType criterion);
	public boolean mustRetry(List<Itinerary> itineraries);
}
