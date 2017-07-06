package eu.trentorise.smartcampus.mobility.gamification.diary;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;

@JsonInclude(Include.NON_NULL)
public class DiaryEntry implements Comparable<DiaryEntry> {

	public enum DiaryEntryType {
		TRAVEL, BADGE, CHALLENGE, CHALLENGE_WON, RECOMMENDED
	}
	
	public enum TravelType {
		PLANNED, FREETRACKING
	}	

	private long timestamp;
	private DiaryEntryType type;
	private String entityId;

	private TravelType travelType;
	private Date travelDate;
	private Double travelLength;
	private Long travelEstimatedScore;
	private TravelValidity travelValidity;

	private String badge;
	private String badgeCollection;
	
	private String challengeName;
	private Long challengeEnd;
//	private Boolean challengeCompleted;
//	private Long challengeCompletedDate;
	private Integer challengeBonus;
	
	private String recommendedNickname;

	@JsonGetter
	public String getDate() {
		return new Date(timestamp).toString();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public DiaryEntryType getType() {
		return type;
	}

	public void setType(DiaryEntryType type) {
		this.type = type;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public Date getTravelDate() {
		return travelDate;
	}

	public TravelType getTravelType() {
		return travelType;
	}

	public void setTravelType(TravelType travelType) {
		this.travelType = travelType;
	}

	public void setTravelDate(Date travelDate) {
		this.travelDate = travelDate;
	}

	public Double getTravelLength() {
		return travelLength;
	}

	public void setTravelLength(Double travelLength) {
		this.travelLength = travelLength;
	}

	public Long getTravelEstimatedScore() {
		return travelEstimatedScore;
	}

	public void setTravelEstimatedScore(Long travelEstimatedScore) {
		this.travelEstimatedScore = travelEstimatedScore;
	}


	public TravelValidity getTravelValidity() {
		return travelValidity;
	}

	public void setTravelValidity(TravelValidity travelValidity) {
		this.travelValidity = travelValidity;
	}

	public String getBadge() {
		return badge;
	}

	public void setBadge(String badge) {
		this.badge = badge;
	}

	public String getBadgeCollection() {
		return badgeCollection;
	}

	public void setBadgeCollection(String badgeCollection) {
		this.badgeCollection = badgeCollection;
	}

	public String getChallengeName() {
		return challengeName;
	}

	public void setChallengeName(String challengeName) {
		this.challengeName = challengeName;
	}

	public Long getChallengeEnd() {
		return challengeEnd;
	}

	public void setChallengeEnd(Long challengeEnd) {
		this.challengeEnd = challengeEnd;
	}

//	public Boolean getChallengeCompleted() {
//		return challengeCompleted;
//	}
//
//	public void setChallengeCompleted(Boolean challengeCompleted) {
//		this.challengeCompleted = challengeCompleted;
//	}
//
//	public Long getChallengeCompletedDate() {
//		return challengeCompletedDate;
//	}
//
//	public void setChallengeCompletedDate(Long challengeCompletedDate) {
//		this.challengeCompletedDate = challengeCompletedDate;
//	}

	public Integer getChallengeBonus() {
		return challengeBonus;
	}

	public void setChallengeBonus(Integer challengeBonus) {
		this.challengeBonus = challengeBonus;
	}

	public String getRecommendedNickname() {
		return recommendedNickname;
	}

	public void setRecommendedNickname(String recommendedNickname) {
		this.recommendedNickname = recommendedNickname;
	}

	@Override
	public String toString() {
		ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.JSON_STYLE);
		return tsb.build();
	}

	@Override
	public int compareTo(DiaryEntry o) {
		return (int) ((timestamp - o.timestamp) / 1000);
	}

}