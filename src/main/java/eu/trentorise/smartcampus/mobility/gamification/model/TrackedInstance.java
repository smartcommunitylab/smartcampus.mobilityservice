package eu.trentorise.smartcampus.mobility.gamification.model;

import java.util.Collection;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;

public class TrackedInstance {

	@Id
	private String id;

	private String clientId;
	private String userId;
	
	private ItineraryObject itinerary;
	private String freeTrackingTransport;
	
	private Collection<Geolocation> geolocationEvents;
	private Boolean started = Boolean.FALSE;
	private Boolean complete = Boolean.FALSE;
	private Boolean valid = Boolean.FALSE;
	
	private String time;
	
	private String deviceInfo;

	@Indexed
	private String day;
	
	private ValidationResult validationResult;
	
	private Long estimatedScore;

	private String appId;
	
	private Boolean switchValidity;
	private Boolean approved;
	private Boolean toCheck;
	
	private int groupId;
	
	public TrackedInstance() {
		geolocationEvents = Sets.newConcurrentHashSet();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String travelId) {
		this.clientId = travelId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public ItineraryObject getItinerary() {
		return itinerary;
	}

	public void setItinerary(ItineraryObject itinerary) {
		this.itinerary = itinerary;
	}

	public Collection<Geolocation> getGeolocationEvents() {
		return geolocationEvents;
	}

	public void setGeolocationEvents(Collection<Geolocation> geolocationEvents) {
		this.geolocationEvents = geolocationEvents;
	}

	public Boolean getStarted() {
		return started;
	}

	public void setStarted(Boolean started) {
		this.started = started;
	}

	public Boolean getComplete() {
		return complete;
	}

	public void setComplete(Boolean complete) {
		this.complete = complete;
	}

	public Boolean getValid() {
		return valid;
	}

	public void setValid(Boolean validity) {
		this.valid = validity;
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public ValidationResult getValidationResult() {
		return validationResult;
	}

	public void setValidationResult(ValidationResult validationResult) {
		this.validationResult = validationResult;
	}

	/**
	 * @return the deviceInfo
	 */
	public String getDeviceInfo() {
		return deviceInfo;
	}

	/**
	 * @param deviceInfo the deviceInfo to set
	 */
	public void setDeviceInfo(String deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	/**
	 * @return the time
	 */
	public String getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(String time) {
		this.time = time;
	}

	/**
	 * @return the freeTrackingTransport
	 */
	public String getFreeTrackingTransport() {
		return freeTrackingTransport;
	}

	/**
	 * @param freeTrackingTransport the freeTrackingTransport to set
	 */
	public void setFreeTrackingTransport(String freeTrackingTransport) {
		this.freeTrackingTransport = freeTrackingTransport;
	}

	public Long getEstimatedScore() {
		return estimatedScore;
	}

	public void setEstimatedScore(Long estimatedScore) {
		this.estimatedScore = estimatedScore;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public Boolean getSwitchValidity() {
		return switchValidity;
	}

	public void setSwitchValidity(Boolean switchValidity) {
		this.switchValidity = switchValidity;
	}

	public Boolean getApproved() {
		return approved;
	}

	public void setApproved(Boolean approved) {
		this.approved = approved;
	}

	public Boolean getToCheck() {
		return toCheck;
	}

	public void setToCheck(Boolean toCheck) {
		this.toCheck = toCheck;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	@Override
	public String toString() {
		return id;
	}


	
	

}
