package eu.trentorise.smartcampus.mobility.controller.rest;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.mobility.geolocation.model.Activity;
import eu.trentorise.smartcampus.mobility.geolocation.model.Battery;
import eu.trentorise.smartcampus.mobility.geolocation.model.Coords;
import eu.trentorise.smartcampus.mobility.geolocation.model.Device;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.GeolocationsEvent;
import eu.trentorise.smartcampus.mobility.geolocation.model.Location;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.resourceprovider.controller.SCController;
import eu.trentorise.smartcampus.resourceprovider.model.AuthServices;

@Controller
@RequestMapping(value = "/gamification")
public class GamificationController extends SCController {

	 @Autowired
	 private DomainStorage storage;

	@Autowired
	private AuthServices services;
	
	@Autowired
	@Value("${geolocations.db.dir}")
	private String geolocationsDBDir;	
	
	@Autowired
	@Value("${geolocations.db}")
	private String geolocationsDB;		

	private Connection connection;
	
	private SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS");

	private final static String CREATE_DB = "CREATE TABLE IF NOT EXISTS geolocations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT, device_id TEXT, device_model TEXT, latitude REAL,  longitude REAL, accuracy INTEGER, altitude REAL, speed REAL, heading REAL, activity_type TEXT, activity_confidence INTEGER, battery_level REAL, battery_is_charging BOOLEAN, is_moving BOOLEAN, geofence TEXT, recorded_at DATETIME, created_at DATETIME, userId TEXT, travelId TEXT)";

	@PostConstruct
	public void init() throws Exception {
		File f = new File(geolocationsDBDir);
		if (!f.exists()) {
			f.mkdir();
		}

		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:" + geolocationsDBDir + "/" + geolocationsDB);

		Statement statement = connection.createStatement();
		statement.setQueryTimeout(30);
		statement.executeUpdate(CREATE_DB);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/geolocations")
	public @ResponseBody void storeGeolocationEvent(@RequestBody GeolocationsEvent geolocationsEvent, HttpServletResponse response) throws Exception {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			Geolocation lastGeolocation = storage.getLastGeolocationByUserId(userId);
			String lastTravelId = null;
			if (lastGeolocation != null) {
				lastTravelId = lastGeolocation.getTravelId();
			}

			if (geolocationsEvent.getLocation() != null) {
				for (Location location : geolocationsEvent.getLocation()) {
					Coords coords = location.getCoords();
					Device device = geolocationsEvent.getDevice();
					Activity activity = location.getActivity();
					Battery battery = location.getBattery();

					Geolocation geolocation = new Geolocation();

					geolocation.setUserId(userId);
					geolocation.setTravelId(geolocationsEvent.getTravelId() != null ? geolocationsEvent.getTravelId() : lastTravelId);

					geolocation.setUuid(location.getUuid());
					if (device != null) {
						geolocation.setDevice_id(device.getUuid());
						geolocation.setDevice_model(device.getModel());
					} else {
						geolocation.setDevice_model("UNKNOWN");
					}
					if (coords != null) {
						geolocation.setLatitude(coords.getLatitude());
						geolocation.setLongitude(coords.getLongitude());
						double c[]= new double[2];
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
					geolocation.setCreated_at(new Date(System.currentTimeMillis()));

					if (location.getGeofence() != null) {
						ObjectMapper mapper = new ObjectMapper();
						geolocation.setGeofence(mapper.writeValueAsString(location.getGeofence())); 
																									
					}

					Statement statement = connection.createStatement();
					String s = buildInsert(geolocation);
					statement.execute(s);
					statement.close();							
					
					geolocation.setGeofence(location.getGeofence());
					
					storage.saveGeolocation(geolocation);
					
			
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/geolocations")
	public @ResponseBody List<Geolocation> searchGeolocationEvent(@RequestParam  Map<String, Object> query, HttpServletResponse response) throws Exception {
		
		Criteria criteria = new Criteria();
		for (String key: query.keySet()) {
			criteria = criteria.and(key).is(query.get(key));
		}
		
		Query mongoQuery = new Query(criteria).with(new Sort(Sort.Direction.DESC, "created_at"));;
		
		return storage.searchDomainObjects(mongoQuery, Geolocation.class);
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/journey/{itineraryId}")
	public @ResponseBody void finishJourney(@PathVariable String itineraryId, HttpServletResponse response) throws Exception {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
			ItineraryObject res = storage.searchDomainObject(pars, ItineraryObject.class);
			if (res != null && !userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			if (res == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private String buildInsert(Geolocation geolocation) {
		String s = "INSERT INTO geolocations VALUES($next_id,";
		s += convertToInsert(geolocation.getUuid()) + "," 
				+ convertToInsert(geolocation.getDevice_id()) + "," 
				+ convertToInsert(geolocation.getDevice_model()) + ","
				+ convertToInsert(geolocation.getLatitude()) + ","
				+ convertToInsert(geolocation.getLongitude()) + ","
				+ convertToInsert(geolocation.getAccuracy()) + "," 
				+ convertToInsert(geolocation.getAltitude()) + ","
				+ convertToInsert(geolocation.getSpeed()) + ","
				+ convertToInsert(geolocation.getHeading()) + ","
				+ convertToInsert(geolocation.getActivity_type()) + ","
				+ convertToInsert(geolocation.getActivity_confidence()) + ","
				+ convertToInsert(geolocation.getBattery_level()) + ","
				+ convertToInsert(geolocation.getBattery_is_charging()) + ","
				+ convertToInsert(geolocation.getIs_moving()) + ","
				+ convertToInsert(geolocation.getGeofence()) + ","
				+ convertToInsert(geolocation.getRecorded_at()) + ","
				+ convertToInsert(geolocation.getCreated_at()) + ","
				+  convertToInsert(geolocation.getUserId()) + ","
				+ convertToInsert(geolocation.getTravelId());
		s += ")";
		return s;
	}
	
	private String convertToInsert(Object o) {
		if (o instanceof String) {
			return "\"" + escape(o) + "\"";
		}
		if (o instanceof Boolean) {
			return ((Boolean)o).booleanValue() ? "1" : "0";
		}
		if (o instanceof Date) {
			return "\"" + sdf.format((Date)o)+ "\"";
		}
		if (o != null) {
			return o.toString();
		} else {
			return null;
		}
	}
	
	private String escape(Object o) {
		return ((o != null)?o.toString().replace("\"", "\"\""):"");
	}	

	@Override
	protected AuthServices getAuthServices() {
		return services;
	}
}