package eu.trentorise.smartcampus.mobility.gamification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.gamification.model.SavedTrip;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance.ScoreStatus;
import eu.trentorise.smartcampus.mobility.geolocation.model.Activity;
import eu.trentorise.smartcampus.mobility.geolocation.model.Battery;
import eu.trentorise.smartcampus.mobility.geolocation.model.Coords;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.GeolocationsEvent;
import eu.trentorise.smartcampus.mobility.geolocation.model.Location;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;

@Component
public class GeolocationsProcessor {

	private static SimpleDateFormat shortSdf = new SimpleDateFormat("yyyy/MM/dd");
	private static SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm");
	private static SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	private static final String TRAVEL_ID = "travelId";
	public static final String START_TIME = "startTime";

	@Autowired
	private DomainStorage storage;

	@Autowired
	private GamificationValidator gamificationValidator;

	@Autowired
	private GamificationManager gamificationManager;

	private static Log logger = LogFactory.getLog(GeolocationsProcessor.class);

	public void storeGeolocationEvents(GeolocationsEvent geolocationsEvent, String appId, String userId, String gameId) throws Exception {
		// logger.info("Receiving geolocation events, token = " + token + ", " + geolocationsEvent.getLocation().size() + " events");
		ObjectMapper mapper = new ObjectMapper();

		int pointCount = 0;
		if (geolocationsEvent.getLocation() != null) {
			pointCount = geolocationsEvent.getLocation().size();
		}
		logger.info("Storing " + pointCount + " geolocations for " + userId + ", " + geolocationsEvent.getDevice());

		checkEventsOrder(geolocationsEvent, userId);

		Multimap<String, Geolocation> geolocationsByItinerary = ArrayListMultimap.create();
		Map<String, String> freeTracks = new HashMap<String, String>();
		Map<String, Long> freeTrackStarts = new HashMap<String, Long>();
		String deviceInfo = mapper.writeValueAsString(geolocationsEvent.getDevice());

		groupByItinerary(geolocationsEvent, userId, geolocationsByItinerary, freeTracks, freeTrackStarts);

		for (String key : geolocationsByItinerary.keySet()) {
			saveTrackedInstance(key, userId, appId, deviceInfo, geolocationsByItinerary, freeTracks, freeTrackStarts);
		}
	}

	private void checkEventsOrder(GeolocationsEvent geolocationsEvent, String userId) {
		if (geolocationsEvent.getLocation() != null && !geolocationsEvent.getLocation().isEmpty()) {
			Location lastOk = geolocationsEvent.getLocation().get(geolocationsEvent.getLocation().size() - 1);
			ArrayList<Location> toKeep = Lists.newArrayList();
			toKeep.add(lastOk);
			for (int i = geolocationsEvent.getLocation().size() - 2; i >= 0; i--) {
				Location l1 = geolocationsEvent.getLocation().get(i);

				Date dOk = lastOk.getTimestamp();
				Date d1 = l1.getTimestamp();
				if (d1 == null) {
					logger.warn("Missing timestamp in location object: " + l1.toString());
					continue;
				}

				int comp = d1.compareTo(dOk);
				if (comp < 0) {
					lastOk = l1;
					toKeep.add(l1);
				} else {
					String tidOk = null;
					String tid1 = null;

					if (lastOk.getExtras() != null && lastOk.getExtras().containsKey("idTrip")) {
						tidOk = (String) lastOk.getExtras().get("idTrip");
					}
					if (l1.getExtras() != null && l1.getExtras().containsKey("idTrip")) {
						tid1 = (String) l1.getExtras().get("idTrip");
					}
					logger.debug("'Unordered' events for user: " + userId + ", tripId: " + tid1 + " / " + tidOk + ", times: " + d1 + " / " + dOk + ", coordinates: " + l1.getCoords() + " / "
							+ lastOk.getCoords());
				}
			}

			geolocationsEvent.setLocation(toKeep);

			Collections.sort(geolocationsEvent.getLocation());
		} else {
			logger.info("No geolocations found.");
		}
	}

	private void groupByItinerary(GeolocationsEvent geolocationsEvent, String userId, Multimap<String, Geolocation> geolocationsByItinerary, Map<String, String> freeTracks, Map<String, Long> freeTrackStarts) throws Exception {
		Long now = System.currentTimeMillis();
		Map<String, Object> device = geolocationsEvent.getDevice();

		if (geolocationsEvent.getLocation() != null) {
			for (Location location : geolocationsEvent.getLocation()) {
				String locationTravelId = null;
				Long locationTs = null;
				if (location.getExtras() != null && location.getExtras().containsKey("idTrip")) {
					locationTravelId = (String) location.getExtras().get("idTrip");
					locationTs = location.getExtras().get("start") != null ? Long.parseLong("" + location.getExtras().get("start")) : null;
				} else {
					// now the plugin supports correctly the extras for each
					// location.
					// locations with empty idTrip are possible only upon
					// initialization/synchronization.
					// we skip them here
					logger.warn("location without idTrip, user: " + userId + ", extras = " + location.getExtras());
					continue;
					// if (lastTravelId != null) {
					// locationTravelId = lastTravelId;
					// } else {
					// continue;
					// }
				}

				if (location.getTimestamp() == null) {
					logger.warn("Missing timestamp in location object: " + location.toString());
					continue;
				}

				if (locationTs == null) {
					locationTs = location.getTimestamp().getTime();
				}

				// discard event older than 2 days
				if (now - 2 * 24 * 3600 * 1000 > location.getTimestamp().getTime()) {
					logger.warn("Timestamp too old, skipping.");
					continue;
				}

				Geolocation geolocation = buildGeolocation(location, userId, locationTravelId, device, now);
				
				String day = shortSdf.format(new Date(locationTs));
				String key = geolocation.getTravelId() + "@" + day;
				geolocationsByItinerary.put(key, geolocation);
				if (StringUtils.hasText((String) location.getExtras().get("transportType"))) {
					freeTracks.put(key, (String) location.getExtras().get("transportType"));
				}

				freeTrackStarts.put(key, locationTs);

				// storage.saveGeolocation(geolocation);
			}
		}

		if (geolocationsByItinerary.keySet() == null || geolocationsByItinerary.keySet().isEmpty()) {
			logger.error("No geolocationsByItinerary set.");
		}
	}
	
	private Geolocation buildGeolocation(Location location, String userId, String locationTravelId, Map<String, Object> device, Long now) {
		Coords coords = location.getCoords();
		Activity activity = location.getActivity();
		Battery battery = location.getBattery();

		Geolocation geolocation = new Geolocation();

		geolocation.setUserId(userId);

		geolocation.setTravelId(locationTravelId);

		geolocation.setUuid(location.getUuid());
		if (device != null) {
			geolocation.setDevice_id((String) device.get("uuid"));
			geolocation.setDevice_model((String) device.get("model"));
		} else {
			geolocation.setDevice_model("UNKNOWN");
		}
		if (coords != null) {
			geolocation.setLatitude(coords.getLatitude());
			geolocation.setLongitude(coords.getLongitude());
			double c[] = new double[2];
			c[0] = geolocation.getLongitude();
			c[1] = geolocation.getLatitude();
			geolocation.setGeocoding(c);
			geolocation.setAccuracy(coords.getAccuracy());
			geolocation.setAltitude(coords.getAltitude());
			geolocation.setSpeed(coords.getSpeed());
			geolocation.setHeading(coords.getHeading());
		}
		if (activity != null) {
			geolocation.setActivity_type(activity.getType());
			geolocation.setActivity_confidence(activity.getConfidence());
		}
		if (battery != null) {
			geolocation.setBattery_level(battery.getLevel());
			geolocation.setBattery_is_charging(battery.getIs_charging());
		}

		geolocation.setIs_moving(location.getIs_moving());

		geolocation.setRecorded_at(new Date(location.getTimestamp().getTime()));

		// TODO: check
		geolocation.setCreated_at(new Date(now++));

		geolocation.setGeofence(location.getGeofence());

		if (StringUtils.hasText((String) location.getExtras().get("btDeviceId"))) {
			geolocation.setCertificate((String) location.getExtras().get("btDeviceId"));
		}

		return geolocation;
	}

	private void saveTrackedInstance(String key, String userId, String appId, String deviceInfo, Multimap<String, Geolocation> geolocationsByItinerary, Map<String, String> freeTracks,
			Map<String, Long> freeTrackStarts) throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		String splitKey[] = key.split("@");
		String travelId = splitKey[0];
		String day = splitKey[1];

		TrackedInstance res = getStoredTrackedInstance(key, travelId, day, userId, geolocationsByItinerary, freeTracks, freeTrackStarts);

		if (geolocationsByItinerary.get(key) != null) {
			logger.info("Adding " + geolocationsByItinerary.get(key).size() + " geolocations to result.");
		}
		for (Geolocation geoloc : geolocationsByItinerary.get(key)) {
			res.getGeolocationEvents().add(geoloc);
		}

		// boolean canSave = true;
		//
		if (res.getItinerary() != null) {
			savePlanned(res, userId, travelId, day, appId);
		} else if (res.getFreeTrackingTransport() != null) {
			saveFreeTracking(res, userId, travelId, appId);
		}
		//

		res.setAppId(appId);
		res.setDeviceInfo(deviceInfo);
		storage.saveTrackedInstance(res);

		logger.info("Saved geolocation events, user: " + userId + ", travel: " + res.getId() + ", " + res.getGeolocationEvents().size() + " events.");
	}

	private TrackedInstance getStoredTrackedInstance(String key, String travelId, String day, String userId, Multimap<String, Geolocation> geolocationsByItinerary, Map<String, String> freeTracks,
			Map<String, Long> freeTrackStarts) throws Exception {
		Map<String, Object> pars = Maps.newTreeMap();

		TrackedInstance res = storage.searchDomainObject(pars, TrackedInstance.class);
		if (res == null) {
			logger.error("No existing TrackedInstance found.");
			res = new TrackedInstance();
			res.setClientId(travelId);
			res.setDay(day);
			res.setUserId(userId);
			res.setId(ObjectId.get().toString());
			pars.remove("day");
			ItineraryObject res2 = storage.searchDomainObject(pars, ItineraryObject.class);
			if (res2 == null) {
				logger.warn("No existing ItineraryObject found.");
				pars = new TreeMap<String, Object>();
				pars.put("itinerary.clientId", travelId);
				pars.put("itinerary.userId", userId);
				SavedTrip res3 = storage.searchDomainObject(pars, SavedTrip.class);
				if (res3 != null) {
					res.setItinerary(res3.getItinerary());
				} else {
					logger.warn("No existing SavedTrip found.");
				}
			} else {
				res.setItinerary(res2);
				res.setTime(timeSdf.format(geolocationsByItinerary.get(key).iterator().next().getRecorded_at()));
			}
			if (res.getItinerary() == null) {
				if (travelId.contains("_temporary_")) {
					logger.error("Orphan temporary, skipping clientId: " + travelId);
					// TODO check
					return null;
				}
				String ftt = freeTracks.get(key);
				if (ftt == null) {
					logger.warn("No freetracking transport found, extracting from clientId: " + travelId);
					String[] cid = travelId.split("_");
					if (cid != null && cid.length > 1) {
						ftt = cid[0];
					} else {
						logger.error("Cannot find transport type for " + key);
					}
				}
				res.setFreeTrackingTransport(ftt);
				if (freeTrackStarts.containsKey(key)) {
					res.setTime(timeSdf.format(new Date(freeTrackStarts.get(key))));
				}
			}
		}

		return res;
	}

	private void savePlanned(TrackedInstance res, String userId, String travelId, String day, String appId) throws Exception {

		if (!res.getComplete()) {
			ValidationResult vr = gamificationValidator.validatePlannedJourney(res.getItinerary(), res.getGeolocationEvents(), appId);
			res.setValidationResult(vr);

			if (vr != null && TravelValidity.VALID.equals(vr.getTravelValidity())) {
				Map<String, Object> trackingData = gamificationValidator.computePlannedJourneyScore(appId, res.getItinerary().getData(), res.getGeolocationEvents(), vr.getValidationStatus(),
						res.getOverriddenDistances(), false);
				res.setScoreStatus(ScoreStatus.COMPUTED);
				if (trackingData.containsKey("estimatedScore")) {
					res.setScore((Long) trackingData.get("estimatedScore"));
				}
				trackingData.put(TRAVEL_ID, res.getId());
				trackingData.put(START_TIME, getStartTime(res));
				if (gamificationManager.sendIntineraryDataToGamificationEngine(appId, userId, travelId + "_" + day, res.getItinerary(), trackingData)) {
					res.setScoreStatus(ScoreStatus.SENT);
				}
			}
			res.setComplete(true);
		}
	}

	private void saveFreeTracking(TrackedInstance res, String userId, String travelId, String appId) throws Exception {
		if (!res.getComplete()) {
			ValidationResult vr = gamificationValidator.validateFreeTracking(res.getGeolocationEvents(), res.getFreeTrackingTransport(), appId);
			if (vr != null && TravelValidity.VALID.equals(vr.getTravelValidity())) {
				// TODO reenabled
				boolean isGroup = gamificationValidator.isTripsGroup(res.getGeolocationEvents(), userId, appId, res.getFreeTrackingTransport());
				if (isGroup) {
					if ("bus".equals(res.getFreeTrackingTransport()) || "train".equals(res.getFreeTrackingTransport())) {
						vr.getValidationStatus().setValidationOutcome(TravelValidity.PENDING);
						logger.info("In a group");
					}
				}
			}

			res.setValidationResult(vr);
			if (vr != null && TravelValidity.VALID.equals(vr.getTravelValidity())) {
				// canSave =
				Map<String, Object> trackingData = gamificationValidator.computeFreeTrackingScore(appId, res.getGeolocationEvents(), res.getFreeTrackingTransport(), vr.getValidationStatus(),
						res.getOverriddenDistances());
				res.setScoreStatus(ScoreStatus.COMPUTED);
				if (trackingData.containsKey("estimatedScore")) {
					res.setScore((Long) trackingData.get("estimatedScore"));
				}
				trackingData.put(TRAVEL_ID, res.getId());
				trackingData.put(START_TIME, getStartTime(res));
				if (gamificationManager.sendFreeTrackingDataToGamificationEngine(appId, userId, travelId, res.getGeolocationEvents(), res.getFreeTrackingTransport(), trackingData)) {
					res.setScoreStatus(ScoreStatus.SENT);
				}
			} else {
				logger.debug("Validation result null, not sending data to gamification");
			}
		}
		res.setComplete(true);
	}

	private long getStartTime(TrackedInstance trackedInstance) throws ParseException {
		long time = 0;
		if (trackedInstance.getGeolocationEvents() != null && !trackedInstance.getGeolocationEvents().isEmpty()) {
			Geolocation event = trackedInstance.getGeolocationEvents().stream().sorted().findFirst().get();
			time = event.getRecorded_at().getTime();
		} else if (trackedInstance.getDay() != null && trackedInstance.getTime() != null) {
			String dt = trackedInstance.getDay() + " " + trackedInstance.getTime();
			time = fullSdf.parse(dt).getTime();
		} else if (trackedInstance.getDay() != null) {
			time = shortSdf.parse(trackedInstance.getDay()).getTime();
		}
		return time;
	}

}