package eu.trentorise.smartcampus.mobility.geolocation.model;

import java.util.Date;


public class Geolocation {

	private String userId;
	private String travelId;
	
	private String uuid;
	private String device_id;
	private String device_model;
	private Double latitude;
	private Double longitude;

	private Long accuracy;

	private Double altitude;
	private Double speed;
	private Double heading;

	private String activity_type;
	private Long activity_confidence;
	private Double battery_level;

	private Boolean battery_is_charging;
	private Boolean is_moving;

	private Object geofence;

	private Date recorded_at;
	private Date created_at;

	double[] geocoding;

    public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTravelId() {
		return travelId;
	}

	public void setTravelId(String travelId) {
		this.travelId = travelId;
	}

	public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getDevice_model() {
        return device_model;
    }

    public void setDevice_model(String device_model) {
        this.device_model = device_model;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Long getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Long accuracy) {
        this.accuracy = accuracy;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public String getActivity_type() {
        return activity_type;
    }

    public void setActivity_type(String activity_type) {
        this.activity_type = activity_type;
    }

    public Long getActivity_confidence() {
        return activity_confidence;
    }

    public void setActivity_confidence(Long activity_confidence) {
        this.activity_confidence = activity_confidence;
    }

    public Double getBattery_level() {
        return battery_level;
    }

    public void setBattery_level(Double battery_level) {
        this.battery_level = battery_level;
    }

    public Boolean getBattery_is_charging() {
        return battery_is_charging;
    }

    public void setBattery_is_charging(Boolean battery_is_charging) {
        this.battery_is_charging = battery_is_charging;
    }

    public Boolean getIs_moving() {
        return is_moving;
    }

    public void setIs_moving(Boolean is_moving) {
        this.is_moving = is_moving;
    }

    public Object getGeofence() {
        return geofence;
    }

    public void setGeofence(Object geofence) {
        this.geofence = geofence;
    }

    public Date getRecorded_at() {
        return recorded_at;
    }

    public void setRecorded_at(Date recorded_at) {
        this.recorded_at = recorded_at;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public double[] getGeocoding() {
        return geocoding;
    }

    public void setGeocoding(double[] geocoding) {
        this.geocoding = geocoding;
    }

	
}

// "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL",
// "uuid TEXT",
// "device_id TEXT",
// "device_model TEXT",
// "latitude REAL",
// "longitude REAL",
// "accuracy INTEGER",
// "altitude REAL",
// "speed REAL",
// "heading REAL",
// "activity_type TEXT",
// "activity_confidence INTEGER",
// "battery_level REAL",
// "battery_is_charging BOOLEAN",
// "is_moving BOOLEAN",
// "geofence TEXT",
// "recorded_at DATETIME",
// "created_at DATETIME"