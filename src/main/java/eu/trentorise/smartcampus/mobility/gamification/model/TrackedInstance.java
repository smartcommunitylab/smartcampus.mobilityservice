package eu.trentorise.smartcampus.mobility.gamification.model;

import java.util.Set;

import org.springframework.data.annotation.Id;

import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;

public class TrackedInstance {

	@Id
	private String id;

	private String clientId;
	
	private ItineraryObject itinerary;
	private Set<Geolocation> geolocationEvents;
	private Boolean started = Boolean.FALSE;
	private Boolean complete = Boolean.FALSE;
	private Boolean valid = Boolean.FALSE;
	
	private String day;

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

	public ItineraryObject getItinerary() {
		return itinerary;
	}

	public void setItinerary(ItineraryObject itinerary) {
		this.itinerary = itinerary;
	}

	public Set<Geolocation> getGeolocationEvents() {
		return geolocationEvents;
	}

	public void setGeolocationEvents(Set<Geolocation> geolocationEvents) {
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


}