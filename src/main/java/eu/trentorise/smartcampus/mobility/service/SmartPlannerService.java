/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package eu.trentorise.smartcampus.mobility.service;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.SimpleLeg;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.Transport;
import it.sayservice.platform.smartplanner.data.message.alerts.Alert;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertAccident;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourneyParameters;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;
import it.sayservice.platform.smartplanner.data.message.otpbeans.GeolocalizedStopRequest;
import it.sayservice.platform.smartplanner.data.message.otpbeans.Stop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.controller.extensions.ItineraryRequestEnricher;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanRequest;
import eu.trentorise.smartcampus.mobility.controller.extensions.PromotedJourneyRequestConverter;
import eu.trentorise.smartcampus.mobility.controller.rest.ItinerarySorter;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;
import eu.trentorise.smartcampus.network.JsonUtils;

/**
 * @author raman
 *
 */
@Component
public class SmartPlannerService implements SmartPlannerHelper {

	private static final String SMARTPLANNER = "/smart-planner/api-webapp/planner/";
	private static final String OTP  = "/smart-planner/rest/";

	private static final String PLAN = "plan";
	private static final String RECURRENT = "recurrentJourney";


	@Autowired
	@Value("${otp.url}")
	private String otpURL;	

	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Autowired
	private PromotedJourneyRequestConverter promotedJourneyRequestConverter;
	@Autowired
	private ItineraryRequestEnricher itineraryRequestEnricher;
	@Autowired
	private ExecutorService executorService;
	
	private static final Logger logger = LoggerFactory.getLogger(SmartPlannerService.class);

	private String performGET(String request, String query) throws Exception {
		return HTTPConnector.doGet(otpURL+request, query, MediaType.APPLICATION_JSON, null, "UTF-8");
	}

	private String performPOST(String request, String body) throws Exception {
		return HTTPConnector.doPost(otpURL+request, body, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
	}

	@Override
	public String stopTimetable(String agencyId, String routeId, String stopId) throws Exception {
		return performGET(OTP + "gettimetable/" + agencyId + "/" + routeId + "/" + stopId, null);
	}

	
	@Override
	public String stopTimetable(String agencyId, String stopId, Integer maxResults) throws Exception {
		return performGET(OTP + "getlimitedtimetable/" + agencyId + "/" + stopId + "/" + maxResults, null);
	}

	@Override
	public String transitTimes(String routeId, Long from, Long to) throws Exception {
		return performGET(OTP + "getTransitTimes/" + routeId + "/" + from + "/" + to, null);
	}

	
	@Override
	public String delays(String routeId, Long from, Long to) throws Exception {
		return performGET(OTP + "getTransitDelays/" + routeId + "/" + from + "/" + to, null);
	}

	@Override
	public RecurrentJourney planRecurrent(RecurrentJourneyParameters parameters) throws Exception {
		List<String> reqs = buildRecurrentJourneyPlannerRequest(parameters);
		List<SimpleLeg> legs = new ArrayList<SimpleLeg>();
		for (String req : reqs) {
			String plan = performGET(SMARTPLANNER + RECURRENT, req);
			List<?> sl = mapper.readValue(plan, List.class);
			for (Object o : sl) {
				legs.add(mapper.convertValue(o, SimpleLeg.class));
			}
		}

		RecurrentJourney journey = new RecurrentJourney();
		journey.setParameters(parameters);
		journey.setLegs(legs);
		journey.setMonitorLegs(buildMonitorMap(legs));
		return journey;
	}

	private List<String> buildRecurrentJourneyPlannerRequest(RecurrentJourneyParameters request) {
		List<String> reqs = new ArrayList<String>();
		for (TType type : request.getTransportTypes()) {
			String rec = request.getRecurrence().toString().replaceAll("[\\[\\] ]", "");
			String req = String.format("recurrence=%s&from=%s&to=%s&time=%s&interval=%s&transportType=%s&routeType=%s&fromDate=%s&toDate=%s&numOfItn=%s", rec, request.getFrom().toLatLon(), request.getTo().toLatLon(), request.getTime(), request.getInterval(), type, request.getRouteType(), request.getFromDate(), request.getToDate(), request.getResultsNumber());
			reqs.add(req);
		}
		return reqs;
	}

	private Map<String, Boolean> buildMonitorMap(List<SimpleLeg> legs) {
		Map<String, Boolean> result = new TreeMap<String, Boolean>();

		for (SimpleLeg leg : legs) {
			Transport transport = leg.getTransport();
			if (transport.getType() != TType.BUS && transport.getType() != TType.TRAIN) {
				continue;
			}
			String id = transport.getAgencyId() + "_" + transport.getRouteId();
			if (!result.containsKey(id)) {
				result.put(id, true);
			}
		}

		return result;
	}

	@Override
	public RecurrentJourney replanRecurrent(RecurrentJourneyParameters parameters, RecurrentJourney oldJourney) throws Exception {
		List<String> reqs = buildRecurrentJourneyPlannerRequest(parameters);
		List<SimpleLeg> legs = new ArrayList<SimpleLeg>();
		for (String req : reqs) {
			String plan = performGET(SMARTPLANNER + RECURRENT, req); 
			List<?> sl = mapper.readValue(plan, List.class);
			for (Object o : sl) {
				legs.add((SimpleLeg) mapper.convertValue(o, SimpleLeg.class));
			}
		}
		RecurrentJourney journey = new RecurrentJourney();
		journey.setParameters(parameters);
		journey.setLegs(legs);
		journey.setMonitorLegs(buildMonitorMap(legs, oldJourney.getMonitorLegs()));
		return journey;

	}

	private Map<String, Boolean> buildMonitorMap(List<SimpleLeg> legs, Map<String, Boolean> old) {
		Map<String, Boolean> result = new TreeMap<String, Boolean>();

		for (SimpleLeg leg : legs) {
			Transport transport = leg.getTransport();
			if (transport.getType() != TType.BUS && transport.getType() != TType.TRAIN) {
				continue;
			}
			String id = transport.getAgencyId() + "_" + transport.getRouteId();
			if (!result.containsKey(id)) {
				if (old.containsKey(id)) {
					result.put(id, old.get(id));
				} else {
					result.put(id, true);
				}
			}
		}

		return result;
	}

	@Override
	public String parkingsByAgency(String agencyId) throws Exception {
		return performGET(SMARTPLANNER + "getParkingsByAgency?agencyId=" + agencyId, null);
	}

	@Override
	public String bikeSharingByAgency(String agencyId) throws Exception {
		return performGET(SMARTPLANNER + "getBikeSharingByAgency?agencyId=" + agencyId, null);
	}

	@Override
	public String roadInfoByAgency(String agencyId, Long from, Long to) throws Exception {
		return performGET(SMARTPLANNER + "getAR?agencyId=" + agencyId + "&from=" + from + "&to=" + to, null);
	}
	@Override
	public String routes(String agencyId) throws Exception {
		return performGET(OTP + "getroutes/" + agencyId, null);
	}

	@Override
	public String stops(String agencyId, String routeId) throws Exception {
		return performGET(OTP + "getstops/" + agencyId + "/" + routeId, null);
	}

	@Override
	public String stops(String agencyId, String routeId, double latitude, double longitude, double radius) throws Exception {
		return performGET(OTP + "getstops/" + agencyId + "/" + routeId + "/" + latitude + "/" + longitude + "/" + radius, null);
	}
	
	@Override
	public List<Stop> stops(String agencyId, double lat, double lng, double radius, Integer page, Integer count) throws Exception {
		GeolocalizedStopRequest gsr = new GeolocalizedStopRequest();
		gsr.setAgencyId(agencyId);
		gsr.setCoordinates(new double[]{lat,lng});
		gsr.setRadius(radius);
		gsr.setPageSize(count == null ? 100 : count);
		gsr.setPageNumber(page == null ? 0 : page);
		ObjectMapper mapper = new ObjectMapper();
		String content = mapper.writeValueAsString(gsr);

		String res = performPOST(OTP + "getGeolocalizedStops", content);
		return JsonUtils.toObjectList(res, Stop.class);

	}

	@Override
	public List<Itinerary> planSingleJourney(SingleJourney journeyRequest) throws Exception {
		Map<String, Itinerary> itineraryCache = new TreeMap<String, Itinerary>();

		promotedJourneyRequestConverter.modifyRequest(journeyRequest);
		
		List<PlanRequest> reqs = buildItineraryPlannerRequest(journeyRequest, true);
		Multimap<Integer, Itinerary> evalIts = ArrayListMultimap.create();

		List<Itinerary> itineraries = new ArrayList<Itinerary>();

		List<Future<PlanRequest>> results = Lists.newArrayList();
		Map<String, PlanRequest> reqMap = Maps.newTreeMap();
		for (PlanRequest pr : reqs) {
			reqMap.put(pr.getRequest(), pr);
		}
		
		for (PlanRequest pr : reqMap.values()) {
			CallableItineraryRequest callableReq = new CallableItineraryRequest();
			callableReq.setRequest(pr);
			Future<PlanRequest> future = executorService.submit(callableReq);
			results.add(future);
		}
		for (Future<PlanRequest> plan: results) {
			PlanRequest pr = plan.get();
			if (pr.getPlan() == null) continue;
			List<?> its = mapper.readValue(pr.getPlan(), List.class);
			for (Object it : its) {
				Itinerary itinerary = mapper.convertValue(it, Itinerary.class);
				pr.getItinerary().add(itinerary);
				if (pr.getValue() != 0) {
					itinerary.setPromoted(true);
					evalIts.put(pr.getValue(), itinerary);
				} else {
					itineraries.add(itinerary);
				}
				itineraryCache.put(pr.getRequest(), itinerary);
			}
		}
		
		
		List<Itinerary> evaluated = itineraryRequestEnricher.filterPromotedItineraties(evalIts, journeyRequest.getRouteType());
		itineraries.addAll(evaluated);

		itineraries = itineraryRequestEnricher.removeExtremeItineraties(itineraries, journeyRequest.getRouteType());

		ItinerarySorter.sort(itineraries, journeyRequest.getRouteType());

		itineraryRequestEnricher.completeResponse(journeyRequest, reqs, itineraries);

		promotedJourneyRequestConverter.promoteJourney(reqs);
		
		return itineraries;

	}
	
	private List<PlanRequest> buildItineraryPlannerRequest(SingleJourney request, boolean expand) {
		List<PlanRequest> reqsList = Lists.newArrayList();
		for (TType type : request.getTransportTypes()) {
			int minitn = 1;
			if (type.equals(TType.TRANSIT)) {
				minitn = 3;
			}
			int itn = Math.max(request.getResultsNumber(), minitn);			
			String req = String.format("from=%s,%s&to=%s,%s&date=%s&departureTime=%s&transportType=%s&numOfItn=%s", request.getFrom().getLat(), request.getFrom().getLon(), request.getTo().getLat(), request.getTo().getLon(), request.getDate(), request.getDepartureTime(), type, itn);
			PlanRequest pr = new PlanRequest();
			pr.setRequest(req);
			pr.setType(type);
			pr.setValue(0);
			reqsList.add(pr);
			if (expand) {
				reqsList.addAll(itineraryRequestEnricher.addPromotedItineraries(request, type));
			}
		}
		
		for (PlanRequest req: reqsList) {
			req.setOriginalRequest(request);
		}
		
		return reqsList;
	}

	private class CallableItineraryRequest implements Callable<PlanRequest> {
		
		private PlanRequest request;
		
		public void setRequest(PlanRequest req) {
			this.request = req;
		}
		
		@Override
		public PlanRequest call() throws Exception {
			try {
				String plan = performGET(SMARTPLANNER + PLAN, request.getRequest());
				request.setPlan(plan);
			} catch (Exception e) {
				request.setPlan(null);
			}
			return request;
		}
	}

	@Override
	public void sendAlert(Alert alert) throws Exception {
		String req = mapper.writeValueAsString(alert);
		String param = null;
		if (alert instanceof AlertDelay) {
			param = "updateAD";
		} else if (alert instanceof AlertAccident) {
			param = "updateAE";
		} else if (alert instanceof AlertStrike) {
			param = "updateAS";
		} else if (alert instanceof AlertParking) {
			param = "updateAP";
		} else if (alert instanceof AlertRoad) {
			param = "updateAR";
		} else {
			throw new IllegalArgumentException("Unknown allert type "+alert.getClass().getName());
		}
		
		String result = HTTPConnector.doPost(otpURL + SMARTPLANNER + param, req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
		logger .info(result);				
		
	};		

	
}