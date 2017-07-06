package eu.trentorise.smartcampus.mobility.controller.rest;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.gamification.diary.DiaryEntry;
import eu.trentorise.smartcampus.mobility.gamification.diary.DiaryEntry.DiaryEntryType;
import eu.trentorise.smartcampus.mobility.gamification.diary.DiaryEntry.TravelType;
import eu.trentorise.smartcampus.mobility.gamification.model.BadgeNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.gamificationweb.ChallengesUtils;
import eu.trentorise.smartcampus.mobility.gamificationweb.StatusUtils;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeDescriptionDataSetup;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerStatus;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;

@Controller
public class DiaryController {

	@Autowired
	@Value("${smartcampus.urlws.gamification}")
	private String gamificationConsoleUrl;

	@Autowired
	@Value("${smartcampus.gamification.url}")
	private String gamificationWebUrl;

//	@Autowired
//	@Value("${smartcampus.gamification.gamename}")
//	private String gameName;

	@Autowired
	@Value("${aacURL}")
	private String aacURL;

	@Autowired
	@Value("${smartcampus.isTest}")
	private String isTest;

	private static SimpleDateFormat shortSdf = new SimpleDateFormat("yyyy/MM/dd");
	private static SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;

	@Autowired
	private DomainStorage storage;

	@Autowired
	private AppSetup appSetup;

	@Autowired
	private ChallengeDescriptionDataSetup challDescriptionSetup;

	private ChallengesUtils challUtils;
	
	private ObjectMapper mapper = new ObjectMapper();

	@PostConstruct
	public void init() {
		challUtils = new ChallengesUtils();
		challUtils.setChallLongDescriptionList(challDescriptionSetup.getDescriptions());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/diary")
	public @ResponseBody List<DiaryEntry> getNotifications(@RequestHeader(required = true, value = "appId") String appId, @RequestParam(required = false) Long from,
			@RequestParam(required = false) Long to, @RequestParam(required = false) String typeFilter, HttpServletResponse response) throws Exception {
		String userId = null;
		try {
			userId = (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//			userId = getUserId();
		} catch (SecurityException e) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return null;
		}	
		String type = (isTest.compareTo("true") == 0) ? "test" : "prod";

		Player p = playerRepositoryDao.findBySocialIdAndType(userId, type);

		List<DiaryEntry> result = Lists.newArrayList();

		long fromTime = from != null ? from : 0;
		long toTime = to != null ? to : System.currentTimeMillis();

		List<DiaryEntryType> types;
		if (typeFilter != null) {
			types = Splitter.on(",").splitToList(typeFilter).stream().map(x -> DiaryEntryType.valueOf(x)).collect(Collectors.toList());
		} else {
			types = Arrays.asList(DiaryEntryType.values());
		}

		if (types.contains(DiaryEntryType.BADGE)) {
			List<DiaryEntry> badges = getNotifications(userId, appId);
			result.addAll(badges);
		}
		if (types.contains(DiaryEntryType.TRAVEL)) {
			List<DiaryEntry> travels = getTrackedInstances(userId, appId, fromTime, toTime);
			result.addAll(travels);
		}
		if (types.contains(DiaryEntryType.CHALLENGE)) {
			List<DiaryEntry> challenges = getChallenges(p, appId);
			result.addAll(challenges);
		}
		if (types.contains(DiaryEntryType.CHALLENGE)) {
			List<DiaryEntry> recommended = getFriendRegistered(p, appId);
			result.addAll(recommended);
		}

		result = result.stream().filter(x -> x.getTimestamp() >= fromTime && x.getTimestamp() <= toTime).sorted().collect(Collectors.toList());

		
//		getRanking(p, appId);
		
		return result;
	}

	private List<DiaryEntry> getFriendRegistered(Player p, String appId) throws Exception {
		List<DiaryEntry> result = Lists.newArrayList();

		List<Player> rps = playerRepositoryDao.findByNickRecommandation(p.getNickname());
		if (rps != null) {
			for (Player rp : rps) {
				long timestamp = (long) rp.getPersonalData().get("timestamp");
				DiaryEntry de = new DiaryEntry();
				de.setType(DiaryEntryType.RECOMMENDED);
				de.setTimestamp(timestamp);
				de.setRecommendedNickname(rp.getNickname());
				result.add(de);
			}
		}
		return result;
	}
	
	private void getRanking(Player p, String appId) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		String gameId = appSetup.findAppById(appId).getGameId();
//		ResponseEntity<String> res = restTemplate.exchange(gamificationConsoleUrl + "state/" + gameId, HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)), String.class);
		ResponseEntity<String> res = restTemplate.exchange(gamificationConsoleUrl + "/model/game/" + gameId + "/classification", HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)), String.class);

		String allData = res.getBody();		
//		System.err.println(allData);
		
		Map<String, Object> map = mapper.readValue(allData, Map.class);
		System.err.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map));
		
	}

	private List<DiaryEntry> getChallenges(Player p, String appId) throws Exception {
		List<DiaryEntry> result = Lists.newArrayList();

		String language = ((p.getLanguage() != null) && (p.getLanguage().compareTo("") != 0)) ? p.getLanguage() : "it";

		RestTemplate restTemplate = new RestTemplate();
		String gameId = appSetup.findAppById(appId).getGameId();
		ResponseEntity<String> res = restTemplate.exchange(gamificationConsoleUrl + "state/" + gameId + "/" + p.getPid(), HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)),
				String.class);

		String allData = res.getBody();

		if (challUtils.getChallLongDescriptionList() == null || challUtils.getChallLongDescriptionList().isEmpty()) {
			challUtils.setChallLongDescriptionList(challDescriptionSetup.getDescriptions());
		}

		StatusUtils statusUtils = new StatusUtils();
		PlayerStatus ps = statusUtils.correctPlayerData(allData, p.getPid(), gameId, p.getNickname(), challUtils, gamificationWebUrl, 1, language);

		if (ps.getChallengeConcept() != null) {
			List<ChallengesData> cds = Lists.newArrayList();
			cds.addAll(ps.getChallengeConcept().getActiveChallengeData());
			cds.addAll(ps.getChallengeConcept().getOldChallengeData());
			for (ChallengesData cd : cds) {
				DiaryEntry de = new DiaryEntry();
				de.setEntityId(cd.getChallId());
				de.setType(DiaryEntryType.CHALLENGE);
				de.setTimestamp(cd.getStartDate());
				de.setChallengeName(cd.getChallDesc());
				de.setChallengeBonus(cd.getBonus());
				if (cd.getChallCompletedDate() != 0) {
					DiaryEntry de2 = new DiaryEntry();
					de2.setEntityId(cd.getChallId());
					de2.setType(DiaryEntryType.CHALLENGE_WON);
					de2.setChallengeName(cd.getChallDesc());
					de2.setChallengeBonus(cd.getBonus());					
					de2.setTimestamp(cd.getChallCompletedDate());
					result.add(de2);
				}
				de.setChallengeEnd(cd.getEndDate());
				
				result.add(de);
			}
		}

		return result;
	}

	private List<DiaryEntry> getNotifications(String playerId, String appId) throws Exception {
		List<DiaryEntry> result = Lists.newArrayList();

		RestTemplate restTemplate = new RestTemplate();
		String gameId = appSetup.findAppById(appId).getGameId();
		ResponseEntity<String> res = restTemplate.exchange(gamificationConsoleUrl + "notification/" + gameId + "/" + playerId, HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)),
				String.class);

		List nots = mapper.readValue(res.getBody(), List.class);
		for (Object o : nots) {
			if (((Map) o).containsKey("badge")) {
				BadgeNotification not = mapper.convertValue(o, BadgeNotification.class);
				DiaryEntry de = new DiaryEntry();
				de.setType(DiaryEntryType.BADGE);
				de.setTimestamp(not.getTimestamp());
				de.setBadge(not.getBadge());
				de.setBadgeCollection(not.getCollectionName());
				result.add(de);
			}
		}

		return result;
	}

	private List<DiaryEntry> getTrackedInstances(String playerId, String appId, long from, long to) throws Exception {
		List<DiaryEntry> result = Lists.newArrayList();

		Criteria criteria = new Criteria("userId").is(playerId).and("appId").is(appId);
		String fd = shortSdf.format(new Date(from));
		criteria = criteria.and("day").gte(fd);
		String td = shortSdf.format(new Date(to));
		criteria = criteria.andOperator(new Criteria("day").lte(td));

		Query query = new Query(criteria);
		List<TrackedInstance> instances = storage.searchDomainObjects(query, TrackedInstance.class);
		for (TrackedInstance instance : instances) {
			DiaryEntry de = new DiaryEntry();
			de.setType(DiaryEntryType.TRAVEL);
			long timestamp = 0;
			if (instance.getGeolocationEvents() != null && !instance.getGeolocationEvents().isEmpty()) {
				Geolocation event = instance.getGeolocationEvents().iterator().next();
				timestamp = event.getRecorded_at().getTime();
			} else if (instance.getDay() != null && instance.getTime() != null) {
				String dt = instance.getDay() + " " + instance.getTime();
				timestamp = fullSdf.parse(dt).getTime();
			} else if (instance.getDay() != null) {
				timestamp = shortSdf.parse(instance.getDay()).getTime();
			}
			de.setTimestamp(timestamp);
			de.setTravelEstimatedScore(instance.getEstimatedScore());
			if (instance.getValidationResult() != null) {
				de.setTravelLength(instance.getValidationResult().getDistance());
			}
			if (instance.getItinerary() != null) {
				de.setTravelType(TravelType.PLANNED);
			} else if (instance.getFreeTrackingTransport() != null) {
				de.setTravelType(TravelType.FREETRACKING);
			}
			de.setTravelValidity(instance.getValidationResult().getTravelValidity());
			de.setEntityId(instance.getId());
			result.add(de);
		}

		return result;
	}

	HttpHeaders createHeaders(String appId) {
		return new HttpHeaders() {
			{
				AppInfo app = appSetup.findAppById(appId);
				String auth = app.getGameUser() + ":" + app.getGamePassword();
				byte[] encodedAuth = Base64.encode(auth.getBytes(Charset.forName("UTF-8")));
				String authHeader = "Basic " + new String(encodedAuth);
				set("Authorization", authHeader);
			}
		};
	}

	// @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	// @ExceptionHandler(Exception.class)
	// @ResponseBody
	// ErrorInfo handleBadRequest(HttpServletRequest req, Exception ex) {
	// ex.printStackTrace();
	// StackTraceElement ste = ex.getStackTrace()[0];
	// return new ErrorInfo(req.getRequestURL().toString(),
	// ex.getClass().getTypeName(), ste.getClassName(), ste.getLineNumber());
	// }

}