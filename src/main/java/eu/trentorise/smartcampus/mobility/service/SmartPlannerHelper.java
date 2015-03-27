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
import it.sayservice.platform.smartplanner.data.message.alerts.Alert;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourneyParameters;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;
import it.sayservice.platform.smartplanner.data.message.otpbeans.Stop;

import java.util.List;


/**
 * @author raman
 *
 */
public interface SmartPlannerHelper {

	RecurrentJourney planRecurrent(RecurrentJourneyParameters parameters) throws Exception;
	RecurrentJourney replanRecurrent(RecurrentJourneyParameters parameters, RecurrentJourney oldJourney) throws Exception;
	List<Itinerary> planSingleJourney(SingleJourney journeyRequest) throws Exception;
	
	String parkingsByAgency(String agencyId) throws Exception;
	String bikeSharingByAgency(String agencyId) throws Exception;
	String roadInfoByAgency(String agencyId, Long from, Long to) throws Exception;
	String routes(String agencyId) throws Exception;
	String stops(String agencyId, String routeId) throws Exception;
	String stops(String agencyId, String routeId, double latitude, double longitude, double radius) throws Exception;
	String stopTimetable(String agencyId, String routeId, String stopId) throws Exception;
	String stopTimetable(String agencyId, String stopId, Integer maxResults) throws Exception;
	String transitTimes(String routeId, Long from, Long to) throws Exception;
	String delays(String routeId, Long from, Long to) throws Exception;

	List<Stop> stops(String agencyId, double lat, double lng, double radius, Integer page, Integer count) throws Exception;

	void sendAlert(Alert alert) throws Exception;
}