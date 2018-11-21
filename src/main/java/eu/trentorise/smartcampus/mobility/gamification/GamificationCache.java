package eu.trentorise.smartcampus.mobility.gamification;

import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;

@Component
public class GamificationCache {

	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;	

	@Autowired
	private AppSetup appSetup;

	@Autowired
	private GameSetup gameSetup;	
	
	private LoadingCache<String, String> playerState;
	private LoadingCache<String, String> playerNotifications;
	
	private static transient final Logger logger = Logger.getLogger(GamificationCache.class);
	
	@PostConstruct
	public void init() {
		playerState = CacheBuilder.newBuilder().refreshAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
			@Override
			public String load(String id) throws Exception {
				try {
					String[] ids = id.split("@");
					String data = loadPlayerState(ids[0], ids[1]);
					logger.info("Loaded player state: " + ids[0]);
					return data;
				} catch (Exception e) {
					logger.error("Error populating player state cache.");
					throw e;
				}
			}
			
			@Override
			public ListenableFuture<String> reload(String key, String old) {
				ListenableFutureTask<String> task = ListenableFutureTask.create(new Callable<String>() {
					@Override
					public String call() throws Exception {
						try {
							return load(key);
						} catch (Exception e) {
							logger.error("Returning old value for player state: " + key);
							return old;
						}
					}
				});
				task.run();
				return task;
			}

		});	
		
		playerNotifications = CacheBuilder.newBuilder().refreshAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
			@Override
			public String load(String id) throws Exception {
				try {
					String[] ids = id.split("@");
					String data = loadNotifications(ids[0], ids[1]);
					logger.info("Loaded player notifications: " + ids[0]);
					return data;
				} catch (Exception e) {
					logger.error("Error populating player notifications cache.");
					throw e;
				}
			}
			
			@Override
			public ListenableFuture<String> reload(String key, String old) {
				ListenableFutureTask<String> task = ListenableFutureTask.create(new Callable<String>() {
					@Override
					public String call() throws Exception {
						try {
							return load(key);
						} catch (Exception e) {
							logger.error("Returning old value for notifications: " + key);
							return old;
						}
					}
				});
				task.run();
				return task;
			}

		});			
		
		
		
	}	
	
	public String getPlayerState(String playerId, String appId) {
		try {
			return playerState.get(playerId + "@" + appId);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String getPlayerNotifications(String playerId, String appId) {
		try {
			return playerNotifications.get(playerId + "@" + appId);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}	
	
	private String loadPlayerState(String playerId, String appId) {
		AppInfo appInfo = appSetup.findAppById(appId);
		if (appInfo == null) {
			return null;
		}
		String gameId = appInfo.getGameId();
		if (gameId == null) {
			return null;
		}
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> res = restTemplate.exchange(gamificationUrl + "gengine/state/" + gameId + "/" + playerId, HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)),
				String.class);
		String data = res.getBody();		
		
		return data;
	}

	private String loadNotifications(String playerId, String appId) {
		AppInfo appInfo = appSetup.findAppById(appId);
		if (appInfo == null) {
			return null;
		}
		String gameId = appInfo.getGameId();
		if (gameId == null) {
			return null;
		}
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> res = restTemplate.exchange(gamificationUrl + "/notification/game/" + gameId + "/player/" + playerId + "/grouped?size=1000", HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)),
				String.class);
		String data = res.getBody();		
		
		return data;
	}	
	
	
	
	HttpHeaders createHeaders(String appId) {
		return new HttpHeaders() {
			{
				AppInfo app = appSetup.findAppById(appId);
				GameInfo game = gameSetup.findGameById(app.getGameId());
				String auth = game.getUser() + ":" + game.getPassword();
				byte[] encodedAuth = Base64.encode(auth.getBytes(Charset.forName("UTF-8")));
				String authHeader = "Basic " + new String(encodedAuth);
				set("Authorization", authHeader);
			}
		};
	}
	
	
	
}